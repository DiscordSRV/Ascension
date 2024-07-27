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

package com.discordsrv.common.feature.messageforwarding.game.minecrafttodiscord;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.message.AllowedMention;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.util.DiscordFormattingUtil;
import com.discordsrv.api.eventbus.EventPriority;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.message.forward.game.GameChatMessageForwardedEvent;
import com.discordsrv.api.events.message.receive.game.GameChatMessageReceiveEvent;
import com.discordsrv.api.placeholder.format.FormattedText;
import com.discordsrv.api.placeholder.util.Placeholders;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.channels.MinecraftToDiscordChatConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.feature.messageforwarding.game.AbstractGameMessageModule;
import com.discordsrv.common.permission.game.Permission;
import com.discordsrv.common.util.CompletableFutureUtil;
import com.discordsrv.common.util.ComponentUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MinecraftToDiscordChatModule extends AbstractGameMessageModule<MinecraftToDiscordChatConfig, GameChatMessageReceiveEvent> {

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
    public MinecraftToDiscordChatConfig mapConfig(BaseChannelConfig channelConfig) {
        return channelConfig.minecraftToDiscord;
    }

    @Override
    public void postClusterToEventBus(GameChannel channel, @NotNull ReceivedDiscordMessageCluster cluster) {
        discordSRV.eventBus().publish(new GameChatMessageForwardedEvent(channel, cluster));
    }

    @Override
    public List<CompletableFuture<ReceivedDiscordMessage>> sendMessageToChannels(
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
        List<CompletableFuture<ReceivedDiscordMessage>> futures = new ArrayList<>();

        // Format messages per-Guild
        for (Map.Entry<DiscordGuild, Set<DiscordGuildMessageChannel>> entry : channelMap.entrySet()) {
            Guild guild = entry.getKey().asJDA();
            CompletableFuture<SendableDiscordMessage> messageFuture = getMessageForGuild(config, format, guild, message, player, context);

            for (DiscordGuildMessageChannel channel : entry.getValue()) {
                futures.add(messageFuture.thenCompose(msg -> sendMessageToChannel(channel, msg)));
            }
        }

        return futures;
    }

    @Override
    public void setPlaceholders(MinecraftToDiscordChatConfig config, GameChatMessageReceiveEvent event, SendableDiscordMessage.Formatter formatter) {}

    private final Pattern MENTION_PATTERN = Pattern.compile("@\\S+");

    private CompletableFuture<SendableDiscordMessage> getMessageForGuild(
            MinecraftToDiscordChatConfig config,
            SendableDiscordMessage.Builder format,
            Guild guild,
            Component message,
            IPlayer player,
            Object[] context
    ) {
        MinecraftToDiscordChatConfig.Mentions mentionConfig = config.mentions;
        MentionCachingModule mentionCaching = discordSRV.getModule(MentionCachingModule.class);

        if (mentionCaching != null && mentionConfig.users && mentionConfig.uncachedUsers
                && player.hasPermission(Permission.MENTION_USER_LOOKUP)) {
            List<CompletableFuture<List<MentionCachingModule.CachedMention>>> futures = new ArrayList<>();

            String messageContent = discordSRV.componentFactory().plainSerializer().serialize(message);
            Matcher matcher = MENTION_PATTERN.matcher(messageContent);
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
            MinecraftToDiscordChatConfig config,
            SendableDiscordMessage.Builder format,
            Guild guild,
            Component message,
            IPlayer player,
            Object[] context,
            Set<MentionCachingModule.CachedMention> memberMentions
    ) {
        MinecraftToDiscordChatConfig.Mentions mentionConfig = config.mentions;
        Set<MentionCachingModule.CachedMention> mentions = new LinkedHashSet<>();

        if (memberMentions != null) {
            mentions.addAll(memberMentions);
        }

        MentionCachingModule mentionCaching = discordSRV.getModule(MentionCachingModule.class);
        if (mentionCaching != null) {
            if (mentionConfig.roles) {
                mentions.addAll(mentionCaching.getRoleMentions(guild).values());
            }
            if (mentionConfig.channels) {
                mentions.addAll(mentionCaching.getChannelMentions(guild).values());
            }
            if (mentionConfig.users) {
                mentions.addAll(mentionCaching.getMemberMentions(guild).values());
            }
        }

        List<MentionCachingModule.CachedMention> orderedMentions = mentions.stream()
                .sorted(Comparator.comparingInt(mention -> ((MentionCachingModule.CachedMention) mention).searchLength()).reversed())
                .collect(Collectors.toList());

        List<AllowedMention> allowedMentions = new ArrayList<>();
        if (mentionConfig.users && player.hasPermission(Permission.MENTION_USER)) {
            allowedMentions.add(AllowedMention.ALL_USERS);
        }
        if (mentionConfig.roles) {
            if (player.hasPermission(Permission.MENTION_ROLE_MENTIONABLE)) {
                for (Role role : guild.getRoles()) {
                    if (role.isMentionable()) {
                        allowedMentions.add(AllowedMention.role(role.getIdLong()));
                    }
                }
            }
            if (player.hasPermission(Permission.MENTION_ROLE_ALL)) {
                allowedMentions.add(AllowedMention.ALL_ROLES);
            }
        }

        boolean everyone = mentionConfig.everyone && player.hasPermission(Permission.MENTION_EVERYONE);
        if (everyone) {
            allowedMentions.add(AllowedMention.EVERYONE);
        }

        return format.setAllowedMentions(allowedMentions)
                .toFormatter()
                .addContext(context)
                .addPlaceholder("message", () -> {
                    String convertedComponent = convertComponent(config, message);
                    Placeholders channelMessagePlaceholders = new Placeholders(
                            DiscordFormattingUtil.escapeMentions(convertedComponent));

                    // From longest to shortest
                    orderedMentions.forEach(mention -> channelMessagePlaceholders.replaceAll(mention.search(), mention.mention()));

                    String finalMessage = channelMessagePlaceholders.toString();
                    return new FormattedText(preventEveryoneMentions(everyone, finalMessage));
                })
                .applyPlaceholderService()
                .build();
    }

    public String convertComponent(MinecraftToDiscordChatConfig config, Component component) {
        String content = discordSRV.placeholderService().getResultAsCharSequence(component).toString();

        Placeholders messagePlaceholders = new Placeholders(content);
        config.contentRegexFilters.forEach(messagePlaceholders::replaceAll);

        return messagePlaceholders.toString();
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
