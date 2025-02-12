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

package com.discordsrv.common.command.game.abstraction.handler;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.command.game.abstraction.command.GameCommandArguments;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.util.CommandUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class BasicCommandHandler implements ICommandHandler {

    private final DiscordSRV discordSRV;
    private final Map<String, GameCommand> commands = new HashMap<>();

    public BasicCommandHandler(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public void registerCommand(GameCommand command) {
        commands.put(command.getLabel(), command);
    }

    private GameCommand processCommand(
            GameCommand command,
            List<String> arguments,
            Arguments argumentValues,
            BiConsumer<GameCommand, Arguments> tooManyArguments,
            BiConsumer<GameCommand, Arguments> unclosedQuote,
            List<String> literalOptions
    ) {
        GameCommand redirection = command.getRedirection();
        if (redirection != null) {
            command = redirection;
        }

        if (!arguments.isEmpty()) {
            List<String> currentOptions = new ArrayList<>();
            for (GameCommand child : command.getChildren()) {
                if (child.getArgumentType() == GameCommand.ArgumentType.LITERAL) {
                    currentOptions.add(child.getLabel());
                }

                GameCommand.ArgumentResult result = child.checkArgument(arguments.get(0));
                GameCommand.MatchResult matchResult = result.result();
                if (matchResult == GameCommand.MatchResult.NO_MATCH) {
                    continue;
                }
                arguments.remove(0);

                Object value;
                if (child.getArgumentType().multi()) {
                    StringBuilder content = new StringBuilder((String) result.value());
                    if (matchResult == GameCommand.MatchResult.END) {
                        // Premature end
                        if (unclosedQuote != null) {
                            unclosedQuote.accept(command, argumentValues);
                        }
                        return unclosedQuote == null ? command : null;
                    }

                    boolean stringStart = matchResult == GameCommand.MatchResult.CONTINUE;
                    while (stringStart && !arguments.isEmpty()) {
                        result = child.checkArgument(arguments.remove(0));
                        Object resultValue = result.value();
                        content.append(' ').append(resultValue != null ? resultValue : "");
                        matchResult = result.result();
                        if (matchResult == GameCommand.MatchResult.END) {
                            stringStart = false;
                        }
                    }
                    if (stringStart && child.getArgumentType() == GameCommand.ArgumentType.STRING) {
                        // Unclosed quote
                        if (unclosedQuote != null) {
                            unclosedQuote.accept(command, argumentValues);
                        } else {
                            argumentValues.put(child.getLabel(), "\"" + content);
                        }
                        return unclosedQuote == null ? child : null;
                    }

                    value = content.toString();
                } else {
                    value = result.value();
                }

                argumentValues.put(child.getLabel(), value);
                return processCommand(child, arguments, argumentValues, tooManyArguments, unclosedQuote, literalOptions);
            }

            if (literalOptions != null) {
                literalOptions.addAll(currentOptions);
                return command;
            }
        }

        if (!arguments.isEmpty() && tooManyArguments != null) {
            tooManyArguments.accept(command, argumentValues);
            return null;
        }

        return command;
    }

    private <T> T useCommand(
            String command,
            List<String> arguments,
            BiConsumer<GameCommand, Arguments> tooManyArguments,
            BiConsumer<GameCommand, Arguments> unclosedQuote,
            BiFunction<GameCommand, Arguments, T> function,
            List<String> literalOptions
    ) {
        command = command.substring(command.lastIndexOf(':') + 1);
        GameCommand commandBuilder = commands.get(command);
        if (commandBuilder == null) {
            return null;
        }

        Arguments args = new Arguments();
        GameCommand subCommand = processCommand(commandBuilder, new ArrayList<>(arguments), args, tooManyArguments, unclosedQuote, literalOptions);
        if (subCommand == null) {
            return null;
        }

        return function.apply(subCommand, args);
    }

    public void execute(ICommandSender sender, String command, List<String> arguments) {
        CommandUtil.basicStatusCheck(discordSRV, sender);

        useCommand(
                command,
                arguments,
                (cmd, args) -> {
                    if (!cmd.hasPermission(sender)) {
                        cmd.sendNoPermission(sender);
                        return;
                    }

                    error(sender, command, arguments, args, "Incorrect argument for command");
                },
                (cmd, args) -> {
                    if (!cmd.hasPermission(sender)) {
                        cmd.sendNoPermission(sender);
                        return;
                    }

                    error(sender, command, arguments, args, "Unclosed quoted string");
                },
                (cmd, args) -> {
                    cmd.getExecutor().execute(sender, args, command);
                    return null;
                },
                null);
    }

    private void error(ICommandSender sender, String command, List<String> arguments, Arguments args, String title) {
        // Mimic brigadier behaviour (en-US)
        StringBuilder builder = new StringBuilder(command);
        for (Object value : args.getValues().values()) {
            builder.append(" ").append(value);
        }
        int length = builder.length();
        String pre = length >= 10 ? "..." + builder.substring(length - 9, length) : builder.toString();

        sender.sendMessage(
                Component.text()
                        .append(Component.text(title, NamedTextColor.RED))
                        .append(Component.newline())
                        .append(Component.text(pre + " ", NamedTextColor.GRAY))
                        .append(Component.text(String.join(" ", arguments), NamedTextColor.RED, TextDecoration.UNDERLINED))
                        .append(Component.text("<--[HERE]", NamedTextColor.RED, TextDecoration.ITALIC))
        );
    }

    public List<String> suggest(ICommandSender sender, String command, List<String> arguments) {
        List<String> suggestions = new ArrayList<>();
        return useCommand(
                command,
                arguments,
                null,
                null,
                (cmd, args) -> {
                    suggestions.addAll(
                            cmd.getSuggester().suggestValues(sender, args, String.valueOf(args.get(cmd.getLabel(), Object.class)))
                                    .stream()
                                    .map(value -> value.substring(value.lastIndexOf(' ') + 1))
                                    .collect(Collectors.toList())
                    );
                    return suggestions;
                },
                suggestions
        );
    }

    private static class Arguments implements GameCommandArguments {

        private final Map<String, Object> values = new LinkedHashMap<>();

        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(String label, Class<T> type) {
            return (T) values.get(label);
        }

        public void put(String label, Object value) {
            values.put(label, value);
        }

        public Map<String, Object> getValues() {
            return values;
        }

        @Override
        public String toString() {
            return "Arguments{" +
                    "values=" + values +
                    '}';
        }
    }
}
