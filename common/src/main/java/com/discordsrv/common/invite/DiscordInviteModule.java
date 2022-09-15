/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.invite;

import com.discordsrv.api.discord.connection.jda.errorresponse.ErrorCallbackContext;
import com.discordsrv.api.placeholder.FormattedText;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.DiscordInviteConfig;
import com.discordsrv.common.module.type.AbstractModule;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.attribute.IInviteContainer;

import java.util.List;

public class DiscordInviteModule extends AbstractModule<DiscordSRV> {

    private static final String UNKNOWN_INVITE = "https://discord.gg/#Could_not_get_invite,_please_check_your_configuration";
    private String invite;

    public DiscordInviteModule(DiscordSRV discordSRV) {
        super(discordSRV);
        discordSRV.placeholderService().addGlobalContext(this);
    }

    @Override
    public void reload() {
        JDA jda = discordSRV.jda().orElse(null);
        if (jda == null) {
            return;
        }

        DiscordInviteConfig config = discordSRV.config().invite;

        // Manual
        String invite = config.inviteUrl;
        if (invite != null && !invite.isEmpty()) {
            this.invite = invite;
            return;
        }

        List<Guild> guilds = jda.getGuilds();
        if (guilds.size() != 1) {
            return;
        }

        Guild guild = guilds.get(0);

        // Vanity url
        if (config.attemptToUseVanityUrl) {
            String vanityUrl = guild.getVanityUrl();
            if (vanityUrl != null) {
                this.invite = vanityUrl;
                return;
            }
        }

        // Auto create
        if (config.autoCreateInvite) {
            Member selfMember = guild.getSelfMember();
            if (!selfMember.hasPermission(Permission.CREATE_INSTANT_INVITE)) {
                return;
            }

            IInviteContainer channel = guild.getRulesChannel();
            if (channel == null) {
                channel = guild.getDefaultChannel();
            }
            if (channel == null) {
                return;
            }

            channel.createInvite().setMaxAge(0).setUnique(true).queue(
                    inv -> this.invite = inv.getUrl(),
                    ErrorCallbackContext.context("Failed to auto create invite")
            );
        }
    }

    @Placeholder("discord_invite")
    public CharSequence getInvite() {
        return new FormattedText(invite != null ? invite : UNKNOWN_INVITE);
    }
}
