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

package com.discordsrv.common.command.game.abstraction.handler.util;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.command.game.abstraction.command.GameCommandArguments;
import com.discordsrv.common.command.game.abstraction.command.GameCommandExecutor;
import com.discordsrv.common.command.game.abstraction.command.GameCommandSuggester;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.permission.game.Permission;
import com.discordsrv.common.util.CommandUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Helper class to convert DiscordSRV's abstract command tree into a brigadier one.
 */
public final class BrigadierUtil {

    private static final Map<GameCommand, CommandNode<?>> CACHE = new ConcurrentHashMap<>();

    private BrigadierUtil() {}

    public static <S> LiteralCommandNode<S> convertToBrigadier(DiscordSRV discordSRV, GameCommand command, Function<S, ICommandSender> commandSenderMapper) {
        return (LiteralCommandNode<S>) convert(discordSRV, command, commandSenderMapper);
    }

    @SuppressWarnings("unchecked")
    private static <S> CommandNode<S> convert(DiscordSRV discordSRV, GameCommand commandBuilder, Function<S, ICommandSender> commandSenderMapper) {
        CommandNode<S> alreadyConverted = (CommandNode<S>) CACHE.get(commandBuilder);
        if (alreadyConverted != null) {
            return alreadyConverted;
        }

        GameCommand.ArgumentType type = commandBuilder.getArgumentType();
        String label = commandBuilder.getLabel();
        GameCommandExecutor executor = commandBuilder.getExecutor();
        GameCommandSuggester suggester = commandBuilder.getSuggester();
        GameCommand redirection = commandBuilder.getRedirection();
        Permission requiredPermission = commandBuilder.getRequiredPermission();

        ArgumentBuilder<S, ?> argumentBuilder;
        if (type == GameCommand.ArgumentType.LITERAL) {
            argumentBuilder = LiteralArgumentBuilder.literal(label);
        } else {
            argumentBuilder = RequiredArgumentBuilder.argument(label, convertType(commandBuilder));
        }

        for (GameCommand child : commandBuilder.getChildren()) {
            argumentBuilder.then(convert(discordSRV, child, commandSenderMapper));
        }
        if (redirection != null) {
            CommandNode<S> redirectNode = (CommandNode<S>) CACHE.get(redirection);
            if (redirectNode == null) {
                redirectNode = convert(discordSRV, redirection, commandSenderMapper);
            }
            argumentBuilder.redirect(redirectNode);
        }

        if (requiredPermission != null) {
            argumentBuilder.requires(sender -> {
                ICommandSender commandSender = commandSenderMapper.apply(sender);
                return commandSender.hasPermission(requiredPermission);
            });
        }
        if (executor != null) { // TODO: check if null check needed
            argumentBuilder.executes(context -> {
                ICommandSender commandSender = commandSenderMapper.apply(context.getSource());
                CommandUtil.basicStatusCheck(discordSRV, commandSender);

                executor.execute(
                        commandSender,
                        getArgumentMapper(context),
                        commandBuilder
                );
                return Command.SINGLE_SUCCESS;
            });
        }
        if (suggester != null && argumentBuilder instanceof RequiredArgumentBuilder) { // TODO: check if null check needed
            ((RequiredArgumentBuilder<S, ?>) argumentBuilder).suggests((context, builder) -> {
                try {
                    List<?> suggestions =  suggester.suggestValues(
                            commandSenderMapper.apply(context.getSource()),
                            getArgumentMapper(context),
                            builder.getRemaining()
                    );
                    suggestions.forEach(suggestion -> builder.suggest(suggestion.toString()));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                return CompletableFuture.completedFuture(builder.build());
            });
        }

        CommandNode<S> node = argumentBuilder.build();
        CACHE.put(commandBuilder, node);
        return node;
    }

    private static ArgumentType<?> convertType(GameCommand builder) {
        GameCommand.ArgumentType argumentType = builder.getArgumentType();
        double min = builder.getMinValue();
        double max = builder.getMaxValue();
        switch (argumentType) {
            case LONG: return LongArgumentType.longArg((long) min, (long) max);
            case FLOAT: return FloatArgumentType.floatArg((float) min, (float) max);
            case DOUBLE: return DoubleArgumentType.doubleArg(min, max);
            case STRING: return StringArgumentType.string();
            case STRING_WORD: return StringArgumentType.word();
            case STRING_GREEDY: return StringArgumentType.greedyString();
            case BOOLEAN: return BoolArgumentType.bool();
            case INTEGER: return IntegerArgumentType.integer((int) min, (int) max);
        }
        throw new IllegalStateException();
    }

    private static GameCommandArguments getArgumentMapper(CommandContext<?> context) {
        return new GameCommandArguments() {
            @Override
            public <T> T get(String label, Class<T> type) {
                try {
                    return context.getArgument(label, type);
                } catch (Throwable t) {
                    return null;
                }
            }
        };
    }
}
