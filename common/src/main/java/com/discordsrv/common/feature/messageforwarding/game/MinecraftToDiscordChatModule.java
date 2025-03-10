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

package com.discordsrv.common.feature.messageforwarding.game;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.message.AllowedMention;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.message.forward.game.GameChatMessageForwardedEvent;
import com.discordsrv.api.events.message.receive.game.GameChatMessageReceiveEvent;
import com.discordsrv.api.placeholder.format.FormattedText;
import com.discordsrv.api.placeholder.format.PlainPlaceholderFormat;
import com.discordsrv.api.placeholder.util.Placeholders;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.channels.MinecraftToDiscordChatConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.feature.mention.CachedMention;
import com.discordsrv.common.feature.mention.MentionCachingModule;
import com.discordsrv.common.permission.game.Permissions;
import com.discordsrv.common.util.ComponentUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MinecraftToDiscordChatModule extends AbstractGameMessageModule<MinecraftToDiscordChatConfig, GameChatMessageReceiveEvent> {

    public MinecraftToDiscordChatModule(DiscordSRV discordSRV) {
        super(discordSRV, "MINECRAFT_TO_DISCORD");
    }

    @Subscribe(priority = EventPriorities.LAST, ignoreCancelled = false, ignoreProcessed = false)
    public void onChatReceive(GameChatMessageReceiveEvent event) {
        if (checkProcessor(event) || checkCancellation(event)) {
            return;
        }

        discordSRV.scheduler().run(() -> process(event, event.getPlayer(), event.getGameChannel()));
        event.markAsProcessed();
    }

    @Override
    public MinecraftToDiscordChatConfig mapConfig(BaseChannelConfig channelConfig) {
        return channelConfig.minecraftToDiscord;
    }

    @Override
    public void postClusterToEventBus(GameChannel channel, @NotNull ReceivedDiscordMessageCluster cluster) {
        discordSRV.eventBus().publish(new GameChatMessageForwardedEvent(channel, cluster));
    }

    @Override
    public List<Task<ReceivedDiscordMessage>> sendMessageToChannels(
            MinecraftToDiscordChatConfig config,
            IPlayer player,
            SendableDiscordMessage.Builder format,
            Collection<DiscordGuildMessageChannel> channels,
            GameChatMessageReceiveEvent event,
            Object... context
    ) {
        Map<DiscordGuild, Set<DiscordGuildMessageChannel>> channelMap = new LinkedHashMap<>();
        for (DiscordGuildMessageChannel channel : channels) {
            DiscordGuild guild = channel.getGuild();

            channelMap.computeIfAbsent(guild, key -> new LinkedHashSet<>())
                    .add(channel);
        }

        Component message = ComponentUtil.fromAPI(event.getMessage());
        List<Task<ReceivedDiscordMessage>> futures = new ArrayList<>();

        // Format messages per-Guild
        for (Map.Entry<DiscordGuild, Set<DiscordGuildMessageChannel>> entry : channelMap.entrySet()) {
            Guild guild = entry.getKey().asJDA();
            Task<SendableDiscordMessage> messageFuture = getMessageForGuild(config, format, guild, message, player, context);

            for (DiscordGuildMessageChannel channel : entry.getValue()) {
                futures.add(messageFuture.then(msg -> {
                    if (msg.isEmpty()) {
                        return Task.completed(null);
                    }
                    return sendMessageToChannel(channel, msg);
                }));
            }
        }

        return futures;
    }

    @Override
    public void setPlaceholders(MinecraftToDiscordChatConfig config, GameChatMessageReceiveEvent event, SendableDiscordMessage.Formatter formatter) {}

    private Task<SendableDiscordMessage> getMessageForGuild(
            MinecraftToDiscordChatConfig config,
            SendableDiscordMessage.Builder format,
            Guild guild,
            Component message,
            IPlayer player,
            Object[] context
    ) {
        MentionCachingModule mentionCaching = discordSRV.getModule(MentionCachingModule.class);
        if (mentionCaching != null) {
            String messageContent = discordSRV.componentFactory().plainSerializer().serialize(message);
            return mentionCaching.lookup(config.mentions, guild, player, messageContent, null)
                    .thenApply(mentions -> getMessageForGuildWithMentions(config, format, guild, message, player, context, mentions));
        }

        return Task.completed(getMessageForGuildWithMentions(config, format, guild, message, player, context, null));
    }

    private SendableDiscordMessage getMessageForGuildWithMentions(
            MinecraftToDiscordChatConfig config,
            SendableDiscordMessage.Builder format,
            Guild guild,
            Component message,
            IPlayer player,
            Object[] context,
            List<CachedMention> mentions
    ) {
        MinecraftToDiscordChatConfig.Mentions mentionConfig = config.mentions;

        List<AllowedMention> allowedMentions = new ArrayList<>();
        if (mentionConfig.users && player.hasPermission(Permissions.MENTION_USER)) {
            allowedMentions.add(AllowedMention.ALL_USERS);
        }
        if (mentionConfig.roles) {
            if (player.hasPermission(Permissions.MENTION_ROLE_ALL)) {
                allowedMentions.add(AllowedMention.ALL_ROLES);
            } else if (player.hasPermission(Permissions.MENTION_ROLE_MENTIONABLE)) {
                for (Role role : guild.getRoles()) {
                    if (role.isMentionable()) {
                        allowedMentions.add(AllowedMention.role(role.getIdLong()));
                    }
                }
            }
        }

        boolean everyoneMentionAllowed = mentionConfig.everyone && player.hasPermission(Permissions.MENTION_EVERYONE);
        if (everyoneMentionAllowed) {
            allowedMentions.add(AllowedMention.EVERYONE);
        }

        return format.setAllowedMentions(allowedMentions)
                .toFormatter()
                .addContext(context)
                .addContext(guild)
                .addPlaceholder("message", () -> {
                    String content = PlainPlaceholderFormat.supplyWith(
                            PlainPlaceholderFormat.Formatting.DISCORD,
                            () -> discordSRV.placeholderService().getResultAsCharSequence(message).toString()
                    );
                    Placeholders messagePlaceholders = new Placeholders(content);
                    config.contentRegexFilters.forEach(messagePlaceholders::replaceAll);

                    if (mentions != null) {
                        mentions.forEach(mention -> messagePlaceholders.replaceAll(mention.search(), mention.mention()));
                    }

                    String finalMessage = messagePlaceholders.toString();
                    return FormattedText.of(preventEveryoneMentions(everyoneMentionAllowed, finalMessage));
                })
                .applyPlaceholderService()
                .build();
    }

    private String preventEveryoneMentions(boolean everyoneAllowed, String message) {
        if (everyoneAllowed) {
            // Nothing to do
            return message;
        }

        message = message
                .replace("@everyone", "\\@\u200Beveryone") // zero-width-space
                .replace("@here", "\\@\u200Bhere"); // zero-width-space

        if (message.contains("@everyone") || message.contains("@here")) {
            throw new IllegalStateException("@everyone or @here blocking unsuccessful");
        }
        return message;
    }
}
