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

package com.discordsrv.common.invite;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.discord.connection.jda.errorresponse.ErrorCallbackContext;
import com.discordsrv.api.event.bus.Subscribe;
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
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateVanityCodeEvent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Consumer;

public class DiscordInviteModule extends AbstractModule<DiscordSRV> {

    private static final String UNKNOWN_INVITE = "https://discord.gg/#Could_not_get_invite,_please_check_your_configuration";
    private String invite;

    public DiscordInviteModule(DiscordSRV discordSRV) {
        super(discordSRV);
        discordSRV.placeholderService().addGlobalContext(this);
    }

    @Override
    public @NotNull Collection<DiscordGatewayIntent> requiredIntents() {
        DiscordInviteConfig config = discordSRV.config().invite;
        if (StringUtils.isNotEmpty(config.inviteUrl)) {
            return Collections.emptySet();
        }

        return EnumSet.of(DiscordGatewayIntent.GUILD_INVITES);
    }

    @Subscribe
    public void onGuildInviteDelete(GuildInviteDeleteEvent event) {
        if (invite.equals(event.getUrl())) {
            reload(__ -> {});
        }
    }

    @Subscribe
    public void onGuildUpdateVanityCode(GuildUpdateVanityCodeEvent event) {
        reload(__ -> {});
    }

    @Override
    public void reload(Consumer<DiscordSRVApi.ReloadResult> resultConsumer) {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            return;
        }

        DiscordInviteConfig config = discordSRV.config().invite;

        // Manual
        String invite = config.inviteUrl;
        if (StringUtils.isNotEmpty(invite)) {
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
