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

package com.discordsrv.common.command.discord.commands;

import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.commands.*;
import com.discordsrv.common.command.discord.commands.subcommand.ExecuteCommand;
import com.discordsrv.common.config.main.command.DiscordCommandConfig;
import com.discordsrv.common.feature.linking.LinkProvider;
import com.discordsrv.common.feature.linking.requirelinking.RequiredLinkingModule;

public class DiscordSRVDiscordCommand {

    private static final String LABEL = "discordsrv";
    private static final ComponentIdentifier IDENTIFIER = ComponentIdentifier.of("DiscordSRV", "discordsrv");

    private static DiscordCommand INSTANCE;

    public static DiscordCommand get(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            DiscordCommandConfig commandConfig = discordSRV.config().discordCommand;
            DiscordCommand.ChatInputBuilder builder = DiscordCommand.chatInput(IDENTIFIER, LABEL, "")
                    .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.discordsrvCommandDescription.content()))
                    .addSubCommandGroup(DebugCommand.getDiscord(discordSRV))
                    .addSubCommand(ParseCommand.getDiscord(discordSRV))
                    .addSubCommand(VersionCommand.getDiscord(discordSRV))
                    .addSubCommand(ResyncCommand.getDiscord(discordSRV));

            if (discordSRV.config().executeCommand.enabled) {
                builder = builder.addSubCommand(ExecuteCommand.get(discordSRV));
            }

            LinkProvider linkProvider = discordSRV.linkProvider();
            if (linkProvider != null) {
                builder = builder.addSubCommand(LinkedCommand.getDiscord(discordSRV));

                if (linkProvider.usesLocalLinking()) {
                    builder = builder
                            .addSubCommand(LinkOtherCommand.getDiscord(discordSRV))
                            .addSubCommand(UnlinkCommand.getDiscordWithOther(discordSRV));
                }
            }

            RequiredLinkingModule<?> requiredLinking = discordSRV.getModule(RequiredLinkingModule.class);
            if (requiredLinking != null && requiredLinking.isEnabled()) {
                builder = builder.addSubCommandGroup(BypassCommand.getDiscord(discordSRV));
            }

            Long guildId = commandConfig.managementCommandServerId > 0 ? commandConfig.managementCommandServerId : null;
            INSTANCE = builder
                    .setContexts(true, guildId == null && !commandConfig.enableManagementCommandGlobally)
                    .setGuildId(guildId)
                    .setDefaultPermission(DiscordCommand.DefaultAccess.ADMINISTRATOR)
                    .build();
        }

        return INSTANCE;
    }
}
