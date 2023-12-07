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

package com.discordsrv.sponge.command.game.handler;

import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.command.game.abstraction.GameCommandArguments;
import com.discordsrv.common.command.game.abstraction.GameCommandExecutor;
import com.discordsrv.common.command.game.handler.ICommandHandler;
import com.discordsrv.common.command.game.sender.ICommandSender;
import com.discordsrv.sponge.SpongeDiscordSRV;
import com.discordsrv.sponge.command.game.sender.SpongeCommandSender;
import net.kyori.adventure.audience.Audience;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.command.parameter.managed.Flag;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.RegisterCommandEvent;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.plugin.PluginContainer;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Sponge has its own api for interacting with Brigadier...
 */
public class SpongeCommandHandler implements ICommandHandler {

    private final Map<String, Command.Parameterized> commands = new LinkedHashMap<>();
    private final Supplier<SpongeDiscordSRV> discordSRV;
    private final PluginContainer container;

    public SpongeCommandHandler(Supplier<SpongeDiscordSRV> discordSRV, PluginContainer container) {
        this.discordSRV = discordSRV;
        this.container = container;
    }

    @Override
    public void registerCommand(GameCommand command) {
        commands.put(command.getLabel(), remap(command));
    }

    private ICommandSender getSender(Subject subject, Audience audience) {
        SpongeDiscordSRV discordSRV = this.discordSRV.get();
        if (discordSRV != null) {
            if (subject instanceof ServerPlayer) {
                return discordSRV.playerProvider().player((ServerPlayer) subject);
            } else if (subject instanceof SystemSubject) {
                return discordSRV.console();
            }
        }

        return new SpongeCommandSender(discordSRV, () -> subject, () -> audience);
    }

    private ICommandSender getSender(CommandContext context) {
        return getSender(context.subject(), context.cause().audience());
    }

    private Command.Parameterized remap(GameCommand command) {
        GameCommand redirection = command.getRedirection();
        if (redirection != null) {
            command = redirection;
        }

        //GameCommandSuggester suggester = command.getSuggester();
        GameCommandExecutor executor = command.getExecutor();
        String permission = command.getRequiredPermission();

        Command.Builder builder = Command.builder();
        builder.addFlag(Flag.builder().alias(command.getLabel()).build());

        if (permission != null) {
            builder.permission(permission);
        }
        for (GameCommand child : command.getChildren()) {
            builder.addChild(remap(child));
        }

        if (executor != null) {
            String label = command.getLabel();
            builder.executor(context -> {
                executor.execute(getSender(context), new GameCommandArguments() {
                    @Override
                    public <T> T get(String label, Class<T> type) {
                        return context.one(new Parameter.Key<T>() {
                            @Override
                            public String key() {
                                return label;
                            }

                            @Override
                            public Type type() {
                                return type;
                            }

                            @Override
                            public boolean isInstance(Object value) {
                                return type.isInstance(value);
                            }

                            @SuppressWarnings("unchecked")
                            @Override
                            public T cast(Object value) {
                                return (T) value;
                            }
                        }).orElse(null);
                    }
                }, label);

                return CommandResult.success();
            });
        }

        return builder.build();
    }

    @Listener
    public void onRegister(RegisterCommandEvent<Command.Parameterized> event) {
        for (Map.Entry<String, Command.Parameterized> entry : commands.entrySet()) {
            event.register(container, entry.getValue(), entry.getKey());
        }
    }
}
