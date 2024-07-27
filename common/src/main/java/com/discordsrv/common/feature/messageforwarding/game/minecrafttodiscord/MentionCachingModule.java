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

import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.MinecraftToDiscordChatConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.github.benmanes.caffeine.cache.Cache;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
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
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class MentionCachingModule extends AbstractModule<DiscordSRV> {

    private final Map<Long, Map<Long, CachedMention>> memberMentions = new ConcurrentHashMap<>();
    private final Map<Long, Cache<Long, CachedMention>> memberMentionsCache = new ConcurrentHashMap<>();

    private final Map<Long, Map<Long, CachedMention>> roleMentions = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, CachedMention>> channelMentions = new ConcurrentHashMap<>();

    public MentionCachingModule(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public @NotNull Collection<DiscordGatewayIntent> requiredIntents() {
        boolean anyUserMentions = false;
        for (BaseChannelConfig channel : discordSRV.channelConfig().getAllChannels()) {
            MinecraftToDiscordChatConfig config = channel.minecraftToDiscord;
            if (!config.enabled) {
                continue;
            }

            MinecraftToDiscordChatConfig.Mentions mentions = config.mentions;
            if (mentions.users) {
                anyUserMentions = true;
                break;
            }
        }

        if (anyUserMentions) {
            return EnumSet.of(DiscordGatewayIntent.GUILD_MEMBERS);
        }

        return Collections.emptySet();
    }

    @Override
    public boolean isEnabled() {
        for (BaseChannelConfig channel : discordSRV.channelConfig().getAllChannels()) {
            MinecraftToDiscordChatConfig config = channel.minecraftToDiscord;
            if (!config.enabled) {
                continue;
            }

            MinecraftToDiscordChatConfig.Mentions mentions = config.mentions;
            if (mentions.roles || mentions.users || mentions.channels) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void disable() {
        memberMentions.clear();
        roleMentions.clear();
        channelMentions.clear();
    }

    @Subscribe
    public void onGuildDelete(GuildLeaveEvent event) {
        long guildId = event.getGuild().getIdLong();
        memberMentions.remove(guildId);
        roleMentions.remove(guildId);
        channelMentions.remove(guildId);
    }

    //
    // Member
    //

    public CompletableFuture<List<CachedMention>> lookupMemberMentions(Guild guild, String mention) {
        CompletableFuture<List<Member>> memberFuture = new CompletableFuture<>();
        guild.retrieveMembersByPrefix(mention.substring(1), 100)
                .onSuccess(memberFuture::complete).onError(memberFuture::completeExceptionally);

        Cache<Long, CachedMention> cache = memberMentionsCache.computeIfAbsent(guild.getIdLong(), key -> discordSRV.caffeineBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build()
        );

        return memberFuture.thenApply(members -> {
            List<CachedMention> cachedMentions = new ArrayList<>();
            for (Member member : members) {
                CachedMention cachedMention = cache.get(member.getIdLong(), k -> convertMember(member));
                cachedMentions.add(cachedMention);
            }
            return cachedMentions;
        });
    }

    public Map<Long, CachedMention> getMemberMentions(Guild guild) {
        return memberMentions.computeIfAbsent(guild.getIdLong(), key -> {
            Map<Long, CachedMention> mentions = new LinkedHashMap<>();
            for (Member member : guild.getMembers()) {
                mentions.put(member.getIdLong(), convertMember(member));
            }
            return mentions;
        });
    }

    private CachedMention convertMember(Member member) {
        return new CachedMention(
                "@" + member.getUser().getName(),
                member.getAsMention(),
                member.getIdLong()
        );
    }

    @Subscribe
    public void onMemberAdd(GuildMemberJoinEvent event) {
        Member member = event.getMember();
        if (member.getGuild().getMemberCache().getElementById(member.getIdLong()) == null) {
            // Member is not cached
            return;
        }

        getMemberMentions(event.getGuild()).put(member.getIdLong(), convertMember(member));
    }

    @Subscribe
    public void onMemberUpdate(GuildMemberUpdateNicknameEvent event) {
        Member member = event.getMember();
        getMemberMentions(event.getGuild()).replace(member.getIdLong(), convertMember(member));
    }

    @Subscribe
    public void onMemberDelete(GuildMemberRemoveEvent event) {
        Member member = event.getMember();
        if (member == null) {
            return;
        }

        getMemberMentions(event.getGuild()).remove(member.getIdLong());
    }

    //
    // Role
    //

    public Map<Long, CachedMention> getRoleMentions(Guild guild) {
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

    //
    // Channel
    //

    public Map<Long, CachedMention> getChannelMentions(Guild guild) {
        return channelMentions.computeIfAbsent(guild.getIdLong(), key -> {
            Map<Long, CachedMention> mentions = new LinkedHashMap<>();
            for (GuildChannel channel : guild.getChannels()) {
                if (channel instanceof Category) {
                    // Not mentionable
                    continue;
                }

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

        public Pattern search() {
            return search;
        }

        public int searchLength() {
            return searchLength;
        }

        public String mention() {
            return mention;
        }

        public long id() {
            return id;
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

        @Override
        public String toString() {
            return "CachedMention{pattern=" + search.pattern() + ",mention=" + mention + "}";
        }
    }
}
