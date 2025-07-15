/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.discordsrv.common.command.game.abstraction.command;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.config.helper.MinecraftMessage;
import com.discordsrv.common.permission.game.Permission;
import com.discordsrv.common.util.CommandUtil;
import com.discordsrv.common.util.function.CheckedFunction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class GameCommand {

    private static final GameCommandSuggester BOOLEANS_SUGGESTER = (player, previous, current) -> {
        if (current.isEmpty()) {
            return Arrays.asList("true", "false");
        } else if ("true".startsWith(current)) {
            return Collections.singletonList("true");
        } else if ("false".startsWith(current)) {
            return Collections.singletonList("false");
        } else {
            return Collections.emptyList();
        }
    };

    public static GameCommand literal(String label) {
        return new GameCommand(label, ArgumentType.LITERAL);
    }

    public static GameCommand booleanArgument(String label) {
        return new GameCommand(label, ArgumentType.BOOLEAN).suggester(BOOLEANS_SUGGESTER);
    }

    public static GameCommand doubleArgument(String label) {
        return new GameCommand(label, ArgumentType.DOUBLE);
    }

    public static GameCommand floatArgument(String label) {
        return new GameCommand(label, ArgumentType.FLOAT);
    }

    public static GameCommand integerArgument(String label) {
        return new GameCommand(label, ArgumentType.INTEGER);
    }

    public static GameCommand longArgument(String label) {
        return new GameCommand(label, ArgumentType.LONG);
    }

    public static GameCommand string(String label) {
        return new GameCommand(label, ArgumentType.STRING);
    }

    public static GameCommand stringWord(String label) {
        return new GameCommand(label, ArgumentType.STRING_WORD);
    }

    public static GameCommand stringGreedy(String label) {
        return new GameCommand(label, ArgumentType.STRING_GREEDY);
    }

    public static GameCommand player(DiscordSRV discordSRV, @Nullable GameCommandSuggester suggester) {
        return GameCommand.stringWord("player")
                .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.playerCommandArgumentDescription.minecraft()))
                .suggester(suggester != null ? suggester : CommandUtil.targetSuggestions(discordSRV, null, player -> true, false));
    }

    public static GameCommand user(DiscordSRV discordSRV, @Nullable GameCommandSuggester suggester) {
        return GameCommand.stringWord("user")
                .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.discordUserCommandArgumentDescription.minecraft()))
                .suggester(suggester != null ? suggester : CommandUtil.targetSuggestions(discordSRV, user -> true, null, false));
    }

    public static GameCommand target(DiscordSRV discordSRV, @Nullable GameCommandSuggester suggester) {
        return GameCommand.stringWord("target")
                .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.targetCommandArgumentDescription))
                .suggester(suggester != null ? suggester : CommandUtil.targetSuggestions(discordSRV, user -> true, player -> true, false));
    }

    private final ExecutorProxy executorProxy = new ExecutorProxy();
    private final SuggesterProxy suggesterProxy = new SuggesterProxy();

    // Command info
    private GameCommand parent = null;
    private final String label;
    private final ArgumentType argumentType;
    private final Map<Locale, Component> descriptionTranslations;
    private final List<GameCommand> children;
    private GameCommand redirection = null;

    // Permission
    private Permission requiredPermission;
    private boolean requiredPermissionSetExplicitly = false;
    private Component noPermissionMessage = null;

    // Executor & suggester
    private GameCommandExecutor commandExecutor = null;
    private GameCommandSuggester commandSuggester = null;

    // Argument type bounds
    private double maxValue = Double.MAX_VALUE;
    private double minValue = Double.MIN_VALUE;

    private GameCommand(String label, ArgumentType argumentType) {
        this.label = label;
        this.argumentType = argumentType;
        this.children = new ArrayList<>();
        this.descriptionTranslations = new HashMap<>();
    }

    @Nullable
    public GameCommand getParent() {
        return parent;
    }

    public String getLabel() {
        return label;
    }

    public ArgumentType getArgumentType() {
        return argumentType;
    }

    public GameCommand addDescriptionTranslation(Locale locale, Component description) {
        this.descriptionTranslations.put(locale, description);
        return this;
    }

    public GameCommand addDescriptionTranslations(Map<Locale, MinecraftMessage> descriptions) {
        descriptions.forEach((locale, message) -> addDescriptionTranslation(locale, message.asComponent()));
        return this;
    }

    @Unmodifiable
    public Map<Locale, Component> getDescriptionTranslations() {
        return Collections.unmodifiableMap(descriptionTranslations);
    }

    /**
     * Adds a sub command. You can only have multiple literals (with unique labels) or a single non-literal one.
     */
    public GameCommand then(GameCommand child) {
        if (redirection != null) {
            throw new IllegalStateException("Cannot add children to a redirected node");
        }
        for (GameCommand builder : children) {
            if (builder.getArgumentType() == ArgumentType.LITERAL
                    && child.getArgumentType() == ArgumentType.LITERAL
                    && builder.getLabel().equals(child.getLabel())) {
                throw new IllegalArgumentException("Duplicate literal with label \"" + child.label + "\"");
            }
            if (child.getArgumentType() == ArgumentType.LITERAL && builder.getArgumentType() != ArgumentType.LITERAL) {
                throw new IllegalStateException("A non-literal is already present, cannot add literal");
            }
            if (child.getArgumentType() != ArgumentType.LITERAL) {
                throw new IllegalStateException("Cannot add non-literal when another child is already present");
            }
        }
        child.parent = this;
        applyToChildren(child);
        this.children.add(child);
        return this;
    }

    private void applyToChildren(GameCommand command) {
        if (command.getNoPermissionMessage() == null && noPermissionMessage != null) {
            command.noPermissionMessage(noPermissionMessage);
        }
        if (!command.requiredPermissionSetExplicitly) {
            command.requiredPermission = this.requiredPermission;
        }
        for (GameCommand child : command.getChildren()) {
            command.applyToChildren(child);
        }
    }

    public List<GameCommand> getChildren() {
        return children;
    }

    public GameCommand redirect(GameCommand redirection) {
        if (!children.isEmpty()) {
            throw new IllegalStateException("Cannot redirect a node with children");
        }
        if (requiredPermission != null) {
            throw new IllegalStateException("Cannot redirect a node with a required permission");
        }
        this.redirection = redirection;
        return this;
    }

    public GameCommand getRedirection() {
        return redirection;
    }

    /**
     * If permission is not set explicitly, permission will be inherited from any parent command this may have.
     * @param permission the new required permission for this command
     * @return the required permission or {@code null} to not require any permission
     */
    public GameCommand requiredPermission(Permission permission) {
        if (redirection != null) {
            throw new IllegalStateException("Cannot required permissions on a node with a redirection");
        }
        this.requiredPermission = permission;
        this.requiredPermissionSetExplicitly = true;
        return this;
    }

    public Permission getRequiredPermission() {
        if (redirection != null) {
            return redirection.getRequiredPermission();
        }
        return requiredPermission;
    }

    public GameCommand noPermissionMessage(Component noPermissionMessage) {
        this.noPermissionMessage = noPermissionMessage;
        return this;
    }

    public Component getNoPermissionMessage() {
        return noPermissionMessage;
    }

    public GameCommand executor(GameCommandExecutor executor) {
        this.commandExecutor = executor;
        return this;
    }

    @NotNull
    public GameCommandExecutor getExecutor() {
        return executorProxy;
    }

    /**
     * Cannot be used with {@link #literal(String)}.
     */
    public GameCommand suggester(GameCommandSuggester suggester) {
        if (argumentType == ArgumentType.LITERAL) {
            throw new IllegalArgumentException("Cannot use on argument type literal");
        }
        this.commandSuggester = suggester;
        return this;
    }

    @NotNull
    public GameCommandSuggester getSuggester() {
        return suggesterProxy;
    }

    /**
     * Can only be used on number argument types.
     */
    public GameCommand minValue(double minValue) {
        mustBeNumber();
        this.minValue = minValue;
        return this;
    }

    public double getMinValue() {
        return minValue;
    }

    /**
     * Can only be used on number argument types.
     */
    public GameCommand maxValue(double maxValue) {
        mustBeNumber();
        this.maxValue = maxValue;
        return this;
    }

    public double getMaxValue() {
        return maxValue;
    }

    private void mustBeNumber() {
        if (!argumentType.number()) {
            throw new IllegalArgumentException("Cannot be used on this argument type");
        }
    }

    public boolean hasPermission(ICommandSender sender) {
        Permission requiredPermission = getRequiredPermission();
        return requiredPermission == null || sender.hasPermission(requiredPermission);
    }

    public void sendNoPermission(ICommandSender sender) {
        sender.sendMessage(noPermissionMessage != null
                           ? noPermissionMessage
                           : Component.text("No permission", NamedTextColor.RED));
    }

    public ArgumentResult checkArgument(String argument) {
        switch (getArgumentType()) {
            case LITERAL: return ArgumentResult.fromBoolean(argument.equals(getLabel()));
            case STRING: {
                if (argument.startsWith("\"")) {
                    boolean single = argument.length() == 1;
                    if (argument.endsWith("\"") && !single) {
                        return new ArgumentResult(argument.substring(1, argument.length() - 1), MatchResult.MATCHES);
                    }
                    return new ArgumentResult(single ? "" : argument.substring(1), single ? MatchResult.END : MatchResult.CONTINUE);
                }
                if (argument.endsWith("\"")) {
                    return new ArgumentResult(argument.substring(0, argument.length() - 1), MatchResult.END);
                }
                return new ArgumentResult(argument, MatchResult.MATCHES);
            }
            case STRING_GREEDY: return new ArgumentResult(argument, GameCommand.MatchResult.CONTINUE);
            default: {
                try {
                    return new ArgumentResult(getArgumentType().function().apply(argument), MatchResult.MATCHES);
                } catch (Throwable ignored) {
                    return ArgumentResult.NO_MATCH;
                }
            }
        }
    }

    public String getArgumentLabel() {
        if (argumentType == ArgumentType.LITERAL) {
            return label;
        } else if (parent.commandExecutor != null) {
            // Parent has an executor, this parameter is optional
            return "[" + label + "]";
        } else {
            return "<" + label + ">";
        }
    }

    public Component describe(@Nullable Locale locale, GameCommandArguments arguments, String rootAlias) {
        List<GameCommand> commandHierarchyInOrder = new ArrayList<>();
        GameCommand current = this;
        while (current != null) {
            commandHierarchyInOrder.add(0, current);
            current = current.parent;
        }

        List<Component> commandHierarchyWithDescriptions = new ArrayList<>(commandHierarchyInOrder.size());
        boolean allLiteral = true;
        List<String> literalParts = new ArrayList<>();
        Component lastLiteralDescription = null;

        for (GameCommand command : commandHierarchyInOrder) {
            TextComponent.Builder component = Component.text().content(command.getArgumentLabel());

            String label = command.getLabel();
            String argumentLabel = command.getArgumentLabel();
            if (command.getParent() == null) {
                label = rootAlias;
                argumentLabel = rootAlias;
            }

            boolean literal = command.getArgumentType() == ArgumentType.LITERAL;
            if (literal) {
                component.content(argumentLabel);
                if (allLiteral) {
                    literalParts.add(argumentLabel);
                }
            } else if (arguments.has(label)) {
                String value = String.valueOf(arguments.get(label, Object.class));
                component.content(value);
                if (allLiteral) {
                    literalParts.add(value);
                }
            } else {
                component.content(argumentLabel);
                allLiteral = false;
            }

            Component description = locale != null ? command.getDescriptionTranslations().get(locale) : null;
            if (description == null) {
                description = command.getDescriptionTranslations().get(Locale.ROOT);
            }
            if (description != null) {
                component.hoverEvent(HoverEvent.showText(description));
            }
            if (literal) {
                lastLiteralDescription = description;
            }

            commandHierarchyWithDescriptions.add(component.build());
        }

        TextComponent.Builder builder = Component.text()
                .append(
                        Component.text()
                                .content("/")
                                .clickEvent(ClickEvent.suggestCommand("/" + String.join(" ", literalParts) + (allLiteral ? "" : " ")))
                                .append(Component.join(JoinConfiguration.spaces(), commandHierarchyWithDescriptions))
                );
        if (lastLiteralDescription != null) {
            builder.append(Component.space()).append(lastLiteralDescription.color(NamedTextColor.GRAY));
        }
        return builder.build();
    }

    public void sendCommandInstructions(ICommandSender sender, GameCommandArguments arguments, String rootAlias) {
        if (getRedirection() != null) {
            getRedirection().sendCommandInstructions(sender, arguments, rootAlias);
            return;
        }

        TextComponent.Builder builder = Component.text();
        builder.append(describe(sender.locale(), arguments, rootAlias).color(NamedTextColor.GOLD));

        boolean anySubCommands = false, anyAvailable = false;
        for (GameCommand child : children) {
            anySubCommands = true;
            if (child.getRedirection() != null || !child.hasPermission(sender)) {
                continue;
            }
            if (!anyAvailable) {
                builder.append(Component.newline()).append(Component.text("Available subcommands:", NamedTextColor.AQUA));
            }
            anyAvailable = true;

            // If the child has exactly one child of its own, go deeper
            while (child.getChildren().size() == 1) {
                child = child.getChildren().get(0);
            }
            builder.append(Component.newline()).append(child.describe(sender.locale(), arguments, rootAlias));
        }
        if (anySubCommands && !anyAvailable) {
            builder.append(Component.newline())
                    .append(Component.text("No available subcommands", NamedTextColor.RED));
        }
        sender.sendMessage(builder.build());
    }

    public static class ArgumentResult {

        public static final ArgumentResult EMPTY_MATCH = new ArgumentResult(MatchResult.MATCHES);
        public static final ArgumentResult NO_MATCH = new ArgumentResult(MatchResult.NO_MATCH);

        public static ArgumentResult fromBoolean(boolean value) {
            return value ? EMPTY_MATCH : NO_MATCH;
        }

        private final Object value;
        private final MatchResult result;

        private ArgumentResult(MatchResult result) {
            this(null, result);
        }

        public ArgumentResult(Object value, MatchResult result) {
            this.value = value;
            this.result = result;
        }

        public Object value() {
            return value;
        }

        public MatchResult result() {
            return result;
        }
    }

    public enum MatchResult {
        MATCHES,
        CONTINUE,
        END,
        NO_MATCH
    }

    public enum ArgumentType {

        LITERAL(),
        BOOLEAN(false, input -> {
            boolean value;
            if ((value = input.equals("true")) || input.equals("false")) {
                return value;
            }
            throw new IllegalArgumentException();
        }),
        DOUBLE(true, Double::parseDouble),
        FLOAT(true, Float::parseFloat),
        INTEGER(true, Integer::parseInt),
        LONG(true, Long::parseLong),
        STRING_WORD(false, input -> input),
        STRING(false, true),
        STRING_GREEDY(false, true);

        private final boolean number;
        private final boolean multi;
        private final CheckedFunction<String, Object> function;

        ArgumentType() {
            this(false, null);
        }

        ArgumentType(boolean number, CheckedFunction<String, Object> function) {
            this(number, false, function);
        }

        ArgumentType(boolean number, boolean multi) {
            this(number, multi, null);
        }

        ArgumentType(boolean number, boolean multi, CheckedFunction<String, Object> function) {
            this.number = number;
            this.multi = multi;
            this.function = function;
        }

        public boolean number() {
            return number;
        }

        public boolean multi() {
            return multi;
        }

        public CheckedFunction<String, Object> function() {
            return function;
        }
    }

    private class ExecutorProxy implements GameCommandExecutor {

        @Override
        public void execute(ICommandSender sender, GameCommandArguments arguments, GameCommand command, String rootAlias) {
            if (!hasPermission(sender)) {
                sendNoPermission(sender);
                return;
            }

            GameCommandExecutor executor = (getRedirection() != null ? getRedirection().commandExecutor : commandExecutor);
            if (executor != null) {
                executor.execute(sender, arguments, command, rootAlias);
            } else if (!children.isEmpty()) {
                sendCommandInstructions(sender, arguments, rootAlias);
            } else {
                throw new IllegalStateException("Command (" + GameCommand.this + ") doesn't have children and has no executor");
            }
        }
    }

    private class SuggesterProxy implements GameCommandSuggester {

        @Override
        public List<String> suggestValues(
                ICommandSender sender,
                GameCommandArguments previousArguments,
                String currentInput
        ) {
            if (!hasPermission(sender)) {
                return Collections.emptyList();
            }

            if (commandSuggester != null) {
                return commandSuggester.suggestValues(sender, previousArguments, currentInput);
            } else {
                return Collections.emptyList();
            }
        }
    }
}
