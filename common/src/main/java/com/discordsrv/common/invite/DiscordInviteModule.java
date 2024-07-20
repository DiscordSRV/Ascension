/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.api.placeholder.format.FormattedText;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.DiscordInviteConfig;
import com.discordsrv.common.discord.util.DiscordPermissionUtil;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.AbstractModule;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.attribute.IInviteContainer;
import net.dv8tion.jda.api.events.guild.invite.GuildInviteDeleteEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateVanityCodeEvent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public class DiscordInviteModule extends AbstractModule<DiscordSRV> {

    private static final String UNKNOWN_INVITE = DiscordSRV.WEBSITE + "/invalid-invite";
    private String invite;
    private Boolean botPublic = null;
    private Future<?> appInfoFuture = null;

    public DiscordInviteModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "INVITE"));
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
            logger().debug("JDA == null");
            return;
        }

        DiscordInviteConfig config = discordSRV.config().invite;

        // Manual
        String invite = config.inviteUrl;
        if (StringUtils.isNotEmpty(invite)) {
            logger().debug("Using configured invite");
            this.invite = invite;
            return;
        }

        Guild guild = jda.getGuildById(config.serverId);
        if (guild != null) {
            logger().debug("Automatically determining invite for configured server id (" + Long.toUnsignedString(config.serverId) + ")");
            consumeGuild(guild, config);
            return;
        }

        List<Guild> guilds = jda.getGuilds();
        if (guilds.size() != 1) {
            logger().debug("Bot is in " + guilds.size() + " servers, not automatically determining invites");
            return;
        }

        if (botPublic == null) {
            if (appInfoFuture == null) {
                appInfoFuture = jda.retrieveApplicationInfo().submit().whenComplete((appInfo, t) -> {
                    botPublic = appInfo.isBotPublic();
                    logger().debug("The bot is " + (botPublic ? "public" : "private"));
                    appInfoFuture = null;
                    consumeGuild(guilds.get(0), config);
                });
            }
            return;
        }

        if (!botPublic) {
            consumeGuild(guilds.get(0), config);
        }
    }

    private void consumeGuild(Guild guild, DiscordInviteConfig config) {
        // Vanity url
        if (config.attemptToUseVanityUrl) {
            String vanityUrl = guild.getVanityUrl();
            if (vanityUrl != null) {
                logger().debug("Using vanity url");
                this.invite = vanityUrl;
                return;
            }
        }

        // Auto create
        if (config.autoCreateInvite) {
            logger().debug("Auto creating invite");

            List<IInviteContainer> channels = new ArrayList<>();
            Optional.ofNullable(guild.getRulesChannel()).ifPresent(channels::add);
            Optional.ofNullable(guild.getDefaultChannel()).ifPresent(channels::add);
            if (channels.isEmpty()) {
                logger().debug("No rules and default channel");
                return;
            }

            List<String> missingPermissionMessages = new ArrayList<>();
            IInviteContainer channelToUse = null;
            for (IInviteContainer potentialChannel : channels) {
                String missingPermissions = DiscordPermissionUtil.missingPermissionsString(
                        potentialChannel,
                        Permission.VIEW_CHANNEL,
                        Permission.CREATE_INSTANT_INVITE,
                        Permission.MANAGE_CHANNEL
                );

                if (missingPermissions != null) {
                    missingPermissionMessages.add(missingPermissions);
                } else {
                    channelToUse = potentialChannel;
                    break;
                }
            }
            if (channelToUse == null) {
                logger().error("Failed to automatically create invite: " + String.join(" and ", missingPermissionMessages));
                return;
            }

            IInviteContainer channel = channelToUse;
            channel.retrieveInvites().queue(invites -> {
                boolean found = false;
                for (Invite existingInvite : invites) {
                    User inviter = existingInvite.getInviter();
                    if (inviter != null && inviter.getIdLong() == inviter.getJDA().getSelfUser().getIdLong()) {
                        this.invite = existingInvite.getUrl();
                        found = true;
                        break;
                    }
                }

                if (found) {
                    return;
                }

                channel.createInvite().setMaxAge(0).setUnique(true).queue(
                        inv -> this.invite = inv.getUrl(),
                        ErrorCallbackContext.context("Failed to auto create invite")
                );
            }, ErrorCallbackContext.context("Failed to get invites for automatic invite creation"));
        }
    }

    @Placeholder("discord_invite")
    public CharSequence getInvite() {
        return new FormattedText(invite != null ? invite : UNKNOWN_INVITE);
    }
}
