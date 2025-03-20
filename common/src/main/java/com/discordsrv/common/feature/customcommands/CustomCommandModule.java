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

package com.discordsrv.common.feature.customcommands;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.discord.interaction.command.AbstractCommandInteractionEvent;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.CustomCommandConfig;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;
import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.Subst;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class CustomCommandModule extends AbstractModule<DiscordSRV> {

    private final List<DiscordCommand> registeredCommands = new ArrayList<>();

    public CustomCommandModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "CUSTOM_COMMANDS"));
    }

    @Override
    public boolean canEnableBeforeReady() {
        return discordSRV.config() != null;
    }

    @Override
    public void reload(Consumer<ReloadResult> resultConsumer) {
        List<CustomCommandConfig> configs = discordSRV.config().customCommands;

        List<LayerCommand> layeredCommands = new ArrayList<>();
        int i = 0;
        for (CustomCommandConfig config : configs) {
            List<String> commandParts = Arrays.asList(config.command.split(" "));
            int parts = commandParts.size();
            if (parts > 3) {
                logger().error("Invalid command (" + config.command + "), too many parts: " + parts);
                continue;
            }
            if (StringUtils.isEmpty(config.description)) {
                logger().error("Invalid command (" + config.command + "): empty description");
                continue;
            }

            String prefixOrMainCommand = String.join(" ", commandParts.subList(0, Math.max(parts - 1, 1)));
            if (parts > 2) {
                String group = commandParts.get(0);
                if (layeredCommands.stream().anyMatch(command -> command.getPrefix().equals(group))) {
                    logger().error("Cannot use sub command group, sub command already being used: " + group); // TODO: better error
                    continue;
                }
            }

            @Subst("ip")
            String name = commandParts.get(parts - 1);
            DiscordCommand.ChatInputBuilder commandBuilder = DiscordCommand.chatInput(
                    ComponentIdentifier.of("DiscordSRV", "custom-command-" + (++i)),
                    name,
                    config.description
            );

            for (CustomCommandConfig.OptionConfig option : config.options) {
                if (StringUtils.isEmpty(option.description)) {
                    logger().error("Invalid command option (" + option.name + " of " + config.command + "): empty description");
                    continue;
                }
                commandBuilder.addOption(
                        CommandOption.builder(option.type, option.name, option.description)
                                .setRequired(option.required)
                                .build()
                );
            }

            commandBuilder.setGuildOnly(config.serverId >= 0);
            commandBuilder.setGuildId(config.serverId > 0 ? config.serverId : null);

            ExecutionHandler handler = new ExecutionHandler(config);
            commandBuilder.setEventHandler(handler::accept);
            DiscordCommand command = commandBuilder.build();

            LayerCommand foundLayer = layeredCommands.stream()
                    .filter(cmd -> cmd.getLayer() == parts)
                    .filter(cmd -> cmd.getPrefix().equals(prefixOrMainCommand))
                    .findAny().orElse(null);
            if (foundLayer != null) {
                if (parts == 1) {
                    logger().error("Duplicate main command: " + commandParts.get(0));
                    continue;
                }

                foundLayer.getCommands().add(command);
                continue;
            }

            layeredCommands.add(new LayerCommand(
                    prefixOrMainCommand,
                    parts,
                    new ArrayList<>(Collections.singleton(command))
            ));
        }

        List<DiscordCommand> commandsToRegister = new ArrayList<>();
        for (LayerCommand layeredCommand : layeredCommands) {
            commandsToRegister.addAll(layeredCommand.getCommands());
        }

        for (DiscordCommand command : registeredCommands) {
            discordSRV.discordAPI().unregisterCommand(command);
        }
        registeredCommands.clear();
        registeredCommands.addAll(commandsToRegister);

        for (DiscordCommand command : commandsToRegister) {
            DiscordCommand.RegistrationResult registrationResult = discordSRV.discordAPI().registerCommand(command);
            logger().debug("Registration of " + command.getName() + ": " + registrationResult.name());
        }
    }

    public static class LayerCommand {

        private final String prefix;
        private final int layer;
        private final List<DiscordCommand> commands;

        public LayerCommand(String prefix, int layer, List<DiscordCommand> commands) {
            this.prefix = prefix;
            this.layer = layer;
            this.commands = commands;
        }

        public String getPrefix() {
            return prefix;
        }

        public int getLayer() {
            return layer;
        }

        public List<DiscordCommand> getCommands() {
            return commands;
        }
    }

    public class ExecutionHandler implements Consumer<AbstractCommandInteractionEvent<?>> {

        private final CustomCommandConfig config;

        public ExecutionHandler(CustomCommandConfig config) {
            this.config = config;
        }

        @Override
        public void accept(AbstractCommandInteractionEvent<?> event) {
            DiscordGuildMember member = event.getMember();
            if (member == null) {
                return;
            }

            boolean anyAllowingConstraint = config.constraints.isEmpty();
            for (CustomCommandConfig.ConstraintConfig constraint : config.constraints) {
                boolean included = constraint.roleAndUserIds.contains(member.getUser().getId());
                if (!included) {
                    for (DiscordRole role : member.getRoles()) {
                        if (constraint.roleAndUserIds.contains(role.getId())) {
                            included = true;
                            break;
                        }
                    }
                }

                if (included != constraint.blacklist) {
                    anyAllowingConstraint = true;
                    break;
                }
            }
            if (!anyAllowingConstraint) {
                event.reply(SendableDiscordMessage.builder().setContent("You do not have permission to run that command").build(), true); // TODO: translation
                return;
            }

            List<String> commandsToRun = config.consoleCommandsToRun;
            for (String command : commandsToRun) {
                discordSRV.console().runCommandWithLogging(discordSRV, event.getUser(), command);
            }

            SendableDiscordMessage.Formatter formatter = config.response.toFormatter();
            optionsToFormatter(event, formatter);

            SendableDiscordMessage message = formatter.applyPlaceholderService().build();
            event.reply(message, config.ephemeral).whenComplete((__, t) -> {
                if (t != null) {
                    logger().debug("Failed to reply to custom command: " + config.command, t);
                }
            });
        }

        private void optionsToFormatter(AbstractCommandInteractionEvent<?> event, SendableDiscordMessage.Formatter formatter) {
            for (CustomCommandConfig.OptionConfig option : config.options) {
                String optionName = option.name;

                Object context;
                switch (option.type) {
                    case CHANNEL:
                        context = event.getOptionAsChannel(optionName);
                        break;
                    case USER:
                        context = event.getOptionAsUser(optionName);
                        break;
                    case ROLE:
                        context = event.getOptionAsRole(optionName);
                        break;
                    case MENTIONABLE:
                        Long id = event.getOptionAsLong(optionName);
                        if (id == null) {
                            context = event.getOptionAsString(optionName);
                            break;
                        }

                        DiscordUser user = discordSRV.discordAPI().getUserById(id);
                        if (user != null) {
                            context = user;
                            break;
                        }

                        DiscordRole role = discordSRV.discordAPI().getRoleById(id);
                        if (role != null) {
                            context = role;
                            break;
                        }

                        DiscordChannel channel = discordSRV.discordAPI().getChannelById(id);
                        if (channel != null) {
                            context = channel;
                            break;
                        }

                        context = event.getOptionAsString(optionName);
                        break;
                    default:
                        context = event.getOptionAsString(optionName);
                        break;
                }
                formatter = formatter.addPlaceholder("option_" + optionName, context);
            }
        }
    }
}
