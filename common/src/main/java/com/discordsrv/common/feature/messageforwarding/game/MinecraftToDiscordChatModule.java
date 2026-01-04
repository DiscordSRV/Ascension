/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.message.AllowedMention;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.message.post.game.AbstractGameMessagePostEvent;
import com.discordsrv.api.events.message.post.game.GameChatMessagePostEvent;
import com.discordsrv.api.events.message.postprocess.game.GameChatMessagePostProcessEvent;
import com.discordsrv.api.events.message.preprocess.game.GameChatMessagePreProcessEvent;
import com.discordsrv.api.placeholder.format.FormattedText;
import com.discordsrv.api.placeholder.format.PlainPlaceholderFormat;
import com.discordsrv.api.placeholder.util.Placeholders;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.channels.MinecraftToDiscordChatConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.core.component.DiscordMentionComponent;
import com.discordsrv.common.feature.mention.CachedMention;
import com.discordsrv.common.feature.mention.MentionCachingModule;
import com.discordsrv.common.permission.game.Permission;
import com.discordsrv.common.permission.game.Permissions;
import com.discordsrv.common.util.ComponentUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MinecraftToDiscordChatModule extends AbstractGameMessageModule<MinecraftToDiscordChatConfig, GameChatMessagePreProcessEvent, GameChatMessagePostProcessEvent> {

    public MinecraftToDiscordChatModule(DiscordSRV discordSRV) {
        super(discordSRV, "MINECRAFT_TO_DISCORD");
    }

    @Subscribe(priority = EventPriorities.LAST, ignoreCancelled = false, ignoreProcessed = false)
    public void onChatReceive(GameChatMessagePreProcessEvent event) {
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
    public Task<Void> sendMessageToChannels(
            MinecraftToDiscordChatConfig config,
            GameChatMessagePreProcessEvent event,
            IPlayer player,
            SendableDiscordMessage.Builder format,
            List<DiscordGuildMessageChannel> allChannels,
            List<Object> context
    ) {
        Map<DiscordGuild, List<DiscordGuildMessageChannel>> channelMap = new LinkedHashMap<>();
        for (DiscordGuildMessageChannel channel : allChannels) {
            DiscordGuild guild = channel.getGuild();

            List<DiscordGuildMessageChannel> guildChannels = channelMap.computeIfAbsent(guild, key -> new ArrayList<>());
            if (!guildChannels.contains(channel)) {
                guildChannels.add(channel);
            }
        }

        Component message = ComponentUtil.fromAPI(event.getMessage());
        List<Task<Void>> futures = new ArrayList<>();

        // Format messages per-Guild
        for (Map.Entry<DiscordGuild, List<DiscordGuildMessageChannel>> entry : channelMap.entrySet()) {
            Guild guild = entry.getKey().asJDA();
            Task<SendableDiscordMessage> messageFuture = getMessageForGuild(config, format, guild, message, player, context);

            futures.add(messageFuture.then(discordMessage -> {
                if (discordMessage.isEmpty()) {
                    return Task.completed(null);
                }
                List<DiscordGuildMessageChannel> channels = entry.getValue();

                GameChatMessagePostProcessEvent postProcessEvent = createPostProcessEvent(event, player, channels, discordMessage);
                discordSRV.eventBus().publish(postProcessEvent);
                if (checkCancellation(postProcessEvent)) {
                    return Task.completed(null);
                }
                discordMessage = postProcessEvent.getMessage();

                List<Task<ReceivedDiscordMessage>> messageFutures = new ArrayList<>(channels.size());
                for (DiscordGuildMessageChannel channel : channels) {
                    messageFutures.add(sendMessageToChannel(channel, discordMessage));
                }
                return messageSent(postProcessEvent, Task.allOf(messageFutures));
            }));
        }

        return Task.allOf(futures).thenApply(__ -> null);
    }

    @Override
    protected GameChatMessagePostProcessEvent createPostProcessEvent(
            GameChatMessagePreProcessEvent preEvent,
            IPlayer player,
            List<DiscordGuildMessageChannel> channels,
            SendableDiscordMessage discordMessage) {
        return new GameChatMessagePostProcessEvent(preEvent, player, channels, discordMessage);
    }

    @Override
    protected AbstractGameMessagePostEvent<GameChatMessagePostProcessEvent> createPostEvent(
            GameChatMessagePostProcessEvent preEvent,
            ReceivedDiscordMessageCluster cluster
    ) {
        return new GameChatMessagePostEvent(preEvent, cluster);
    }

    @Override
    public void setPlaceholders(MinecraftToDiscordChatConfig config, GameChatMessagePreProcessEvent event, SendableDiscordMessage.Formatter formatter) {}

    private Task<SendableDiscordMessage> getMessageForGuild(
            MinecraftToDiscordChatConfig config,
            SendableDiscordMessage.Builder format,
            Guild guild,
            Component message,
            IPlayer player,
            List<Object> context
    ) {
        MentionCachingModule mentionCaching = discordSRV.getModule(MentionCachingModule.class);
        if (mentionCaching != null) {
            List<Pair<Message.MentionType, String>> preResolvedMentions = DiscordMentionComponent.digValues(message);
            if (!preResolvedMentions.isEmpty()) {
                return mentionCaching.lookup(config.mentions, guild, player, preResolvedMentions)
                        .thenApply(mentions -> getMessageForGuildWithMentions(config, format, guild, message, player, context, mentions));
            }

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
            List<Object> context,
            List<CachedMention> mentions
    ) {
        boolean everyoneMentionAllowed = config.mentions.everyone && player.hasPermission(Permissions.MENTION_EVERYONE);
        List<AllowedMention> allowedMentions = getAllowedMentions(config.mentions, player, everyoneMentionAllowed, mentions);

        return format.setAllowedMentions(allowedMentions)
                .toFormatter()
                .addContext(context)
                .addContext(guild)
                .addPlaceholder("message", () -> {
                    String content = PlainPlaceholderFormat.supplyWith(
                            PlainPlaceholderFormat.Formatting.DISCORD_MARKDOWN,
                            () -> discordSRV.placeholderService().convertReplacementToCharSequence(message).toString()
                    );
                    Placeholders messagePlaceholders = new Placeholders(content);
                    config.contentRegexFilters.forEach(messagePlaceholders::replaceAll);

                    if (mentions != null) {
                        mentions.forEach(mention -> messagePlaceholders.replaceAll(mention.search(), mention.discordMention()));
                    }

                    String finalMessage = messagePlaceholders.toString();
                    return FormattedText.of(preventEveryoneMentions(everyoneMentionAllowed, finalMessage));
                })
                .build();
    }

    private List<AllowedMention> getAllowedMentions(
            MinecraftToDiscordChatConfig.Mentions config,
            IPlayer player,
            boolean everyoneMentionAllowed,
            @Nullable List<CachedMention> mentions
    ) {
        List<AllowedMention> allowedMentions = new ArrayList<>();

        boolean users = config.users;
        boolean allUsers = users && player.hasPermission(Permissions.MENTION_USER_ALL);
        if (allUsers) {
            allowedMentions.add(AllowedMention.ALL_USERS);
        }

        boolean roles = config.roles;
        boolean allRoles = roles && player.hasPermission(Permissions.MENTION_ROLE_ALL);
        boolean mentionableRoles = roles && player.hasPermission(Permissions.MENTION_ROLE_MENTIONABLE);
        if (allRoles) {
            allowedMentions.add(AllowedMention.ALL_ROLES);
        }

        if (everyoneMentionAllowed) {
            allowedMentions.add(AllowedMention.EVERYONE);
        }

        if (mentions == null) {
            return allowedMentions;
        }

        for (CachedMention mention : mentions) {
            CachedMention.Type type = mention.type();
            if (users && type == CachedMention.Type.USER && !allUsers) {
                if (player.hasPermission(Permission.of("mention.user." + Long.toUnsignedString(mention.id())))) {
                    allowedMentions.add(AllowedMention.user(mention.id()));
                }
            } else if (roles && type == CachedMention.Type.ROLE && !allRoles) {
                if (mention.mentionable() && mentionableRoles) {
                    allowedMentions.add(AllowedMention.role(mention.id()));
                    continue;
                }

                if (player.hasPermission(Permission.of("mention.role." + Long.toUnsignedString(mention.id())))) {
                    allowedMentions.add(AllowedMention.role(mention.id()));
                }
            }
        }

        return allowedMentions;
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
