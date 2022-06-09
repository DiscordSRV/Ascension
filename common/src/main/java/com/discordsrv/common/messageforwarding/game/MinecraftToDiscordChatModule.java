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

package com.discordsrv.common.messageforwarding.game;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordTextChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
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
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class MinecraftToDiscordChatModule extends AbstractGameMessageModule<MinecraftToDiscordChatConfig, GameChatMessageReceiveEvent> {

    private final Map<Long, Map<Long, CachedMention>> memberMentions = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, CachedMention>> roleMentions = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, CachedMention>> channelMentions = new ConcurrentHashMap<>();

    public MinecraftToDiscordChatModule(DiscordSRV discordSRV) {
        super(discordSRV, "MINECRAFT_TO_DISCORD");
    }

    @Override
    public void disable() {
        memberMentions.clear();
        roleMentions.clear();
        channelMentions.clear();
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

        return DiscordFormattingUtil.escapeContent(messagePlaceholders.toString());
    }

    @Override
    public Map<CompletableFuture<ReceivedDiscordMessage>, DiscordMessageChannel> sendMessageToChannels(
            OrDefault<MinecraftToDiscordChatConfig> config,
            SendableDiscordMessage.Builder format,
            List<DiscordMessageChannel> channels,
            String message,
            Object... context
    ) {
        Map<DiscordGuild, Set<DiscordMessageChannel>> channelMap = new LinkedHashMap<>();
        for (DiscordMessageChannel channel : channels) {
            DiscordGuild guild;
            if (channel instanceof DiscordTextChannel) {
                guild = ((DiscordTextChannel) channel).getGuild();
            } else if (channel instanceof DiscordThreadChannel) {
                guild = ((DiscordThreadChannel) channel).getParentChannel().getGuild();
            } else {
                continue;
            }

            channelMap.computeIfAbsent(guild, key -> new LinkedHashSet<>())
                    .add(channel);
        }

        Map<CompletableFuture<ReceivedDiscordMessage>, DiscordMessageChannel> futures = new LinkedHashMap<>();

        OrDefault<MinecraftToDiscordChatConfig.Mentions> mentionConfig = config.map(cfg -> cfg.mentions);
        // Format messages per-Guild
        for (Map.Entry<DiscordGuild, Set<DiscordMessageChannel>> entry : channelMap.entrySet()) {
            Guild guild = entry.getKey().asJDA();

            Placeholders channelMessagePlaceholders = new Placeholders(message);
            List<CachedMention> mentions = new ArrayList<>();
            if (mentionConfig.get(cfg -> cfg.roles, false)) {
                mentions.addAll(getRoleMentions(guild).values());
            }
            if (mentionConfig.get(cfg -> cfg.users, false)) {
                mentions.addAll(getMemberMentions(guild).values());
            }
            if (mentionConfig.get(cfg -> cfg.roles, true)) {
                mentions.addAll(getChannelMentions(guild).values());
            }

            // From longest to shortest
            mentions.stream()
                    .sorted(Comparator.comparingInt(mention -> ((CachedMention) mention).searchLength).reversed())
                    .forEachOrdered(mention -> channelMessagePlaceholders.replaceAll(mention.search, mention.mention));

            SendableDiscordMessage discordMessage = format.toFormatter()
                    .addContext(context)
                    .addReplacement("%message%", new FormattedText(channelMessagePlaceholders.toString()))
                    .applyPlaceholderService()
                    .build();

            for (DiscordMessageChannel channel : entry.getValue()) {
                futures.put(channel.sendMessage(discordMessage), channel);
            }
        }

        return futures;
    }

    //
    // Mention caching
    //

    @Subscribe
    public void onGuildDelete(GuildLeaveEvent event) {
        long guildId = event.getGuild().getIdLong();
        memberMentions.remove(guildId);
        roleMentions.remove(guildId);
        channelMentions.remove(guildId);
    }

    private Map<Long, CachedMention> getRoleMentions(Guild guild) {
        return roleMentions.computeIfAbsent(guild.getIdLong(), key -> {
            Map<Long, CachedMention> mentions = new LinkedHashMap<>();
            for (Role role : guild.getRoles()) {
                mentions.put(role.getIdLong(), convertRole(role));
            }
            return mentions;
        });
    }

    private CachedMention convertRole(Role role) {
        return new CachedMention(
                "@" + role.getName(),
                role.getAsMention(),
                role.getIdLong()
        );
    }

    @Subscribe
    public void onRoleCreate(RoleCreateEvent event) {
        Role role = event.getRole();
        getRoleMentions(event.getGuild()).put(role.getIdLong(), convertRole(role));
    }

    @Subscribe
    public void onRoleUpdate(RoleUpdateNameEvent event) {
        Role role = event.getRole();
        getRoleMentions(event.getGuild()).put(role.getIdLong(), convertRole(role));
    }

    @Subscribe
    public void onRoleDelete(RoleDeleteEvent event) {
        Role role = event.getRole();
        getRoleMentions(event.getGuild()).remove(role.getIdLong());
    }

    private Map<Long, CachedMention> getMemberMentions(Guild guild) {
        return channelMentions.computeIfAbsent(guild.getIdLong(), key -> {
            Map<Long, CachedMention> mentions = new LinkedHashMap<>();
            for (Member member : guild.getMembers()) {
                mentions.put(member.getIdLong(), convertMember(member));
            }
            return mentions;
        });
    }

    private CachedMention convertMember(Member member) {
        return new CachedMention(
                "@" + member.getEffectiveName(),
                member.getAsMention(),
                member.getIdLong()
        );
    }

    @Subscribe
    public void onMemberAdd(GuildMemberJoinEvent event) {
        Member member = event.getMember();
        getMemberMentions(event.getGuild()).put(member.getIdLong(), convertMember(member));
    }

    @Subscribe
    public void onMemberUpdate(GuildMemberUpdateNicknameEvent event) {
        Member member = event.getMember();
        getMemberMentions(event.getGuild()).put(member.getIdLong(), convertMember(member));
    }

    @Subscribe
    public void onMemberDelete(GuildMemberRemoveEvent event) {
        Member member = event.getMember();
        if (member == null) {
            return;
        }

        getMemberMentions(event.getGuild()).remove(member.getIdLong());
    }

    private Map<Long, CachedMention> getChannelMentions(Guild guild) {
        return memberMentions.computeIfAbsent(guild.getIdLong(), key -> {
            Map<Long, CachedMention> mentions = new LinkedHashMap<>();
            for (GuildChannel channel : guild.getChannels()) {
                mentions.put(channel.getIdLong(), convertChannel(channel));
            }
            return mentions;
        });
    }

    private CachedMention convertChannel(GuildChannel channel) {
        return new CachedMention(
                "#" + channel.getName(),
                channel.getAsMention(),
                channel.getIdLong()
        );
    }

    @Subscribe
    public void onChannelCreate(ChannelCreateEvent event) {
        if (!event.getChannelType().isGuild()) {
            return;
        }

        GuildChannel channel = (GuildChannel) event.getChannel();
        getChannelMentions(event.getGuild()).put(channel.getIdLong(), convertChannel(channel));
    }

    @Subscribe
    public void onChannelUpdate(ChannelUpdateNameEvent event) {
        if (!event.getChannelType().isGuild()) {
            return;
        }

        GuildChannel channel = (GuildChannel) event.getChannel();
        getChannelMentions(event.getGuild()).put(channel.getIdLong(), convertChannel(channel));
    }

    @Subscribe
    public void onChannelDelete(ChannelDeleteEvent event) {
        if (!event.getChannelType().isGuild()) {
            return;
        }

        GuildChannel channel = (GuildChannel) event.getChannel();
        getChannelMentions(event.getGuild()).remove(channel.getIdLong());
    }

    public static class CachedMention {

        private final Pattern search;
        private final int searchLength;
        private final String mention;
        private final long id;

        public CachedMention(String search, String mention, long id) {
            this.search = Pattern.compile(search, Pattern.LITERAL);
            this.searchLength = search.length();
            this.mention = mention;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CachedMention that = (CachedMention) o;
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }
}
