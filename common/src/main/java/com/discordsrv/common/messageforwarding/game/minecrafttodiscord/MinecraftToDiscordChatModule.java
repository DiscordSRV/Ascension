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

package com.discordsrv.common.messageforwarding.game.minecrafttodiscord;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.entity.channel.DiscordGuildChannel;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.message.AllowedMention;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.util.DiscordFormattingUtil;
import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.message.forward.game.GameChatMessageForwardedEvent;
import com.discordsrv.api.event.events.message.receive.game.GameChatMessageReceiveEvent;
import com.discordsrv.api.placeholder.FormattedText;
import com.discordsrv.api.placeholder.util.Placeholders;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.MinecraftToDiscordChatConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.function.OrDefault;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.messageforwarding.game.AbstractGameMessageModule;
import com.discordsrv.common.player.IPlayer;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MinecraftToDiscordChatModule
        extends AbstractGameMessageModule<MinecraftToDiscordChatConfig, GameChatMessageReceiveEvent> {

    public MinecraftToDiscordChatModule(DiscordSRV discordSRV) {
        super(discordSRV, "MINECRAFT_TO_DISCORD");
    }

    @Subscribe(priority = EventPriority.LAST)
    public void onChatReceive(GameChatMessageReceiveEvent event) {
        if (checkProcessor(event) || checkCancellation(event) || !discordSRV.isReady()) {
            return;
        }

        GameChannel gameChannel = event.getGameChannel();
        process(event, event.getPlayer(), gameChannel);
        event.markAsProcessed();
    }

    @Override
    public OrDefault<MinecraftToDiscordChatConfig> mapConfig(OrDefault<BaseChannelConfig> channelConfig) {
        return channelConfig.map(cfg -> cfg.minecraftToDiscord);
    }

    @Override
    public void postClusterToEventBus(ReceivedDiscordMessageCluster cluster) {
        discordSRV.eventBus().publish(new GameChatMessageForwardedEvent(cluster));
    }

    @Override
    public String convertMessage(OrDefault<MinecraftToDiscordChatConfig> config, Component component) {
        DiscordSerializer discordSerializer = discordSRV.componentFactory().discordSerializer();
        String content = discordSerializer.serialize(component, discordSerializer.getDefaultOptions().withEscapeMarkdown(false));

        Placeholders messagePlaceholders = new Placeholders(content);
        config.opt(cfg -> cfg.contentRegexFilters)
                .ifPresent(patterns -> patterns.forEach(messagePlaceholders::replaceAll));

        return messagePlaceholders.toString();
    }

    @Override
    public Map<CompletableFuture<ReceivedDiscordMessage>, DiscordMessageChannel> sendMessageToChannels(
            OrDefault<MinecraftToDiscordChatConfig> config,
            SendableDiscordMessage.Builder format,
            List<DiscordMessageChannel> channels,
            String message,
            IPlayer player,
            Object... context
    ) {
        Map<DiscordGuild, Set<DiscordMessageChannel>> channelMap = new LinkedHashMap<>();
        for (DiscordMessageChannel channel : channels) {
            DiscordGuild guild;
            if (channel instanceof DiscordGuildChannel) {
                guild = ((DiscordGuildChannel) channel).getGuild();
            } else {
                continue;
            }

            channelMap.computeIfAbsent(guild, key -> new LinkedHashSet<>())
                    .add(channel);
        }

        Map<CompletableFuture<ReceivedDiscordMessage>, DiscordMessageChannel> futures = new LinkedHashMap<>();

        // Format messages per-Guild
        for (Map.Entry<DiscordGuild, Set<DiscordMessageChannel>> entry : channelMap.entrySet()) {
            Guild guild = entry.getKey().asJDA();
            CompletableFuture<SendableDiscordMessage> messageFuture = getMessageForGuild(config, format, guild, message, player, context);

            for (DiscordMessageChannel channel : entry.getValue()) {
                futures.put(messageFuture.thenCompose(channel::sendMessage), channel);
            }
        }

        return futures;
    }

    private final Pattern MENTION_PATTERN = Pattern.compile("@\\S+");

    private CompletableFuture<SendableDiscordMessage> getMessageForGuild(
            OrDefault<MinecraftToDiscordChatConfig> config,
            SendableDiscordMessage.Builder format,
            Guild guild,
            String message,
            IPlayer player,
            Object[] context
    ) {
        OrDefault<MinecraftToDiscordChatConfig.Mentions> mentionConfig = config.map(cfg -> cfg.mentions);
        MentionCachingModule mentionCaching = discordSRV.getModule(MentionCachingModule.class);

        if (mentionCaching != null
                && mentionConfig.get(cfg -> cfg.users, false)
                && mentionConfig.get(cfg -> cfg.uncachedUsers, false)
                && player.hasPermission("discordsrv.mention.lookup.user")) {
            List<CompletableFuture<List<MentionCachingModule.CachedMention>>> futures = new ArrayList<>();

            Matcher matcher = MENTION_PATTERN.matcher(message);
            while (matcher.find()) {
                futures.add(mentionCaching.lookupMemberMentions(guild, matcher.group()));
            }

            if (!futures.isEmpty()) {
                return CompletableFutureUtil.combine(futures).thenApply(values -> {
                    Set<MentionCachingModule.CachedMention> mentions = new LinkedHashSet<>();
                    for (List<MentionCachingModule.CachedMention> value : values) {
                        mentions.addAll(value);
                    }

                    return getMessageForGuildWithMentions(config, format, guild, message, player, context, mentions);
                });
            }
        }

        return CompletableFuture.completedFuture(getMessageForGuildWithMentions(config, format, guild, message, player, context, null));
    }

    private SendableDiscordMessage getMessageForGuildWithMentions(
            OrDefault<MinecraftToDiscordChatConfig> config,
            SendableDiscordMessage.Builder format,
            Guild guild,
            String message,
            IPlayer player,
            Object[] context,
            Set<MentionCachingModule.CachedMention> memberMentions
    ) {
        OrDefault<MinecraftToDiscordChatConfig.Mentions> mentionConfig = config.map(cfg -> cfg.mentions);
        Set<MentionCachingModule.CachedMention> mentions = new LinkedHashSet<>();

        if (memberMentions != null) {
            mentions.addAll(memberMentions);
        }

        MentionCachingModule mentionCaching = discordSRV.getModule(MentionCachingModule.class);
        if (mentionCaching != null) {
            if (mentionConfig.get(cfg -> cfg.roles, false)) {
                mentions.addAll(mentionCaching.getRoleMentions(guild).values());
            }
            if (mentionConfig.get(cfg -> cfg.channels, true)) {
                mentions.addAll(mentionCaching.getChannelMentions(guild).values());
            }
            if (mentionConfig.get(cfg -> cfg.users, false)) {
                mentions.addAll(mentionCaching.getMemberMentions(guild).values());
            }
        }

        Placeholders channelMessagePlaceholders = new Placeholders(
                DiscordFormattingUtil.escapeMentions(message));

        // From longest to shortest
        mentions.stream()
                .sorted(Comparator.comparingInt(mention -> ((MentionCachingModule.CachedMention) mention).searchLength()).reversed())
                .forEachOrdered(mention -> channelMessagePlaceholders.replaceAll(mention.search(), mention.mention()));

        String formattedMessage = DiscordFormattingUtil.escapeFormatting(
                DiscordFormattingUtil.escapeQuotes(
                        channelMessagePlaceholders.toString()));

        List<AllowedMention> allowedMentions = new ArrayList<>();
        if (mentionConfig.get(cfg -> cfg.users, false) && player.hasPermission("discordsrv.mention.user")) {
            allowedMentions.add(AllowedMention.ALL_USERS);
        }
        if (mentionConfig.get(cfg -> cfg.roles, false)) {
            if (player.hasPermission("discordsrv.mention.roles.mentionable")) {
                for (Role role : guild.getRoles()) {
                    if (role.isMentionable()) {
                        allowedMentions.add(AllowedMention.role(role.getIdLong()));
                    }
                }
            }
            if (player.hasPermission("discordsrv.mention.roles.all")) {
                allowedMentions.add(AllowedMention.ALL_ROLES);
            }
        }

        if (mentionConfig.get(cfg -> cfg.everyone, false) && player.hasPermission("discordsrv.mention.everyone")) {
            allowedMentions.add(AllowedMention.EVERYONE);
        } else {
            formattedMessage = formattedMessage
                    .replace("@everyone", "\\@\u200Beveryone") // zero-width-space
                    .replace("@here", "\\@\u200Bhere"); // zero-width-space

            if (formattedMessage.contains("@everyone") || formattedMessage.contains("@here")) {
                throw new IllegalStateException("@everyone or @here blocking unsuccessful");
            }
        }

        return format.setAllowedMentions(allowedMentions)
                .toFormatter()
                .addContext(context)
                .addReplacement("%message%", new FormattedText(formattedMessage))
                .applyPlaceholderService()
                .build();
    }
}
