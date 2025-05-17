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
import com.discordsrv.common.command.combined.commands.UnlinkCommand;
import com.discordsrv.common.command.discord.commands.subcommand.LinkInitDiscordCommand;
import com.discordsrv.common.command.discord.commands.subcommand.PlayerListCommand;
import com.discordsrv.common.feature.linking.LinkProvider;

public class MinecraftDiscordCommand {

    private static DiscordCommand INSTANCE;

    public static DiscordCommand get(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            ComponentIdentifier identifier = ComponentIdentifier.of("DiscordSRV", "minecraft");
            DiscordCommand.ChatInputBuilder builder = DiscordCommand.chatInput(identifier, "minecraft", "Minecraft server commands")
                    .addSubCommand(PlayerListCommand.get(discordSRV));

            LinkProvider linkProvider = discordSRV.linkProvider();
            if (linkProvider != null) {
                if (linkProvider.usesLocalLinking()) {
                    builder = builder
                            .addSubCommand(LinkInitDiscordCommand.getInstance(discordSRV))
                            .addSubCommand(UnlinkCommand.getDiscordWithoutOther(discordSRV));
                }
            }

            INSTANCE = builder
                    .setGuildOnly(false)
                    .build();
        }

        return INSTANCE;
    }
}
