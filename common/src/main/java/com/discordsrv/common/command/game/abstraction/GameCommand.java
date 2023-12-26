/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.command.game.abstraction;

import com.discordsrv.common.command.game.sender.ICommandSender;
import com.discordsrv.common.function.CheckedFunction;
import com.discordsrv.common.permission.Permission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    private final ExecutorProxy executorProxy = new ExecutorProxy();
    private final SuggesterProxy suggesterProxy = new SuggesterProxy();

    // Command info
    private GameCommand parent = null;
    private final String label;
    private final ArgumentType argumentType;
    private final List<GameCommand> children;
    private GameCommand redirection = null;

    // Permission
    private String requiredPermission;
    private Component noPermissionMessage = null;

    // Executor & suggestor
    private GameCommandExecutor commandExecutor = null;
    private GameCommandSuggester commandSuggester = null;

    // Argument type bounds
    private double maxValue = Double.MAX_VALUE;
    private double minValue = Double.MIN_VALUE;

    private GameCommand(String label, ArgumentType argumentType) {
        this.label = label;
        this.argumentType = argumentType;
        this.children = new ArrayList<>();
    }

    private GameCommand(GameCommand original) {
        this.parent = original.parent;
        this.label = original.label;
        this.argumentType = original.argumentType;
        this.children = original.children;
        this.redirection = original.redirection;
        this.requiredPermission = original.requiredPermission;
        this.noPermissionMessage = original.noPermissionMessage;
        this.commandExecutor = original.commandExecutor;
        this.commandSuggester = original.commandSuggester;
        this.maxValue = original.maxValue;
        this.minValue = original.minValue;
    }

    public String getLabel() {
        return label;
    }

    public ArgumentType getArgumentType() {
        return argumentType;
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
        if (child.getNoPermissionMessage() == null && noPermissionMessage != null) {
            child.noPermissionMessage(noPermissionMessage);
        }
        child.parent = this;
        this.children.add(child);
        return this;
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

    public GameCommand requiredPermission(Permission permission) {
        return requiredPermission(permission.permission());
    }

    public GameCommand requiredPermission(String permission) {
        if (redirection != null) {
            throw new IllegalStateException("Cannot required permissions on a node with a redirection");
        }
        this.requiredPermission = permission;
        return this;
    }

    public String getRequiredPermission() {
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
        String requiredPermission = getRequiredPermission();
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
        } else {
            return "<" + label + ">";
        }
    }

    public Component describe() {
        StringBuilder stringBuilder = new StringBuilder();
        GameCommand current = this;
        while (current != null) {
            stringBuilder.insert(0, current.getArgumentLabel() + " ");
            current = current.parent;
        }

        String command = "/" + stringBuilder.substring(0, stringBuilder.length() - 1);
        return Component.text(command, NamedTextColor.AQUA);
    }

    public void sendCommandInstructions(ICommandSender sender) {
        if (children.isEmpty()) {
            throw new IllegalStateException("No children");
        }

        TextComponent.Builder builder = Component.text();
        builder.append(describe().color(NamedTextColor.GRAY).append(Component.text(" available subcommands:")));
        boolean anyAvailable = false;
        for (GameCommand child : children) {
            if (!child.hasPermission(sender)) {
                continue;
            }
            anyAvailable = true;
            builder.append(Component.newline()).append(child.describe());
        }
        if (!anyAvailable) {
            builder.append(Component.newline())
                    .append(Component.text("No available subcommands", NamedTextColor.RED));
        }
        sender.sendMessage(builder.build());
    }

    @SuppressWarnings({"CloneDoesntDeclareCloneNotSupportedException", "MethodDoesntCallSuperMethod"})
    @Override
    protected GameCommand clone() {
        return new GameCommand(this);
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
        public void execute(ICommandSender sender, GameCommandArguments arguments, String label) {
            if (!hasPermission(sender)) {
                sendNoPermission(sender);
                return;
            }

            if (commandExecutor != null) {
                commandExecutor.execute(sender, arguments, label);
            } else if (!children.isEmpty()) {
                sendCommandInstructions(sender);
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
