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

package com.discordsrv.common.feature.mention;

import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.channels.MinecraftToDiscordChatConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.permission.game.Permission;
import com.discordsrv.common.util.CompletableFutureUtil;
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
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MentionCachingModule extends AbstractModule<DiscordSRV> {

    private static final Pattern USER_MENTION_PATTERN = Pattern.compile("(?<!<)@([a-z0-9_.]{2,32})");

    private final Cache<Pair<Long, Long>, CachedMention> memberMentionsCache;
    private final Map<Long, Map<Long, CachedMention>> roleMentions = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, CachedMention>> channelMentions = new ConcurrentHashMap<>();

    public MentionCachingModule(DiscordSRV discordSRV) {
        super(discordSRV);
        this.memberMentionsCache = discordSRV.caffeineBuilder()
                .expireAfterWrite(Duration.ofMinutes(5))
                .build();
    }

    @Override
    public @NotNull Collection<DiscordGatewayIntent> requiredIntents() {
        return Collections.emptySet();
    }

    @Override
    public boolean isEnabled() {
        for (BaseChannelConfig channel : discordSRV.channelConfig().getAllChannels()) {
            MinecraftToDiscordChatConfig config = channel.minecraftToDiscord;
            if (!config.enabled) {
                continue;
            }

            if (config.mentions.any()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void disable() {
        memberMentionsCache.invalidateAll();
        roleMentions.clear();
        channelMentions.clear();
    }

    public CompletableFuture<List<CachedMention>> lookup(
            MinecraftToDiscordChatConfig.Mentions config,
            Guild guild,
            IPlayer player,
            String messageContent,
            Set<Member> lookedUpMembers
    ) {
        List<CompletableFuture<List<CachedMention>>> futures = new ArrayList<>();
        List<CachedMention> mentions = new ArrayList<>();

        if (config.users) {
            boolean uncached = config.uncachedUsers && player.hasPermission(Permission.MENTION_USER_LOOKUP);

            Matcher matcher = USER_MENTION_PATTERN.matcher(messageContent);
            while (matcher.find()) {
                String username = matcher.group(1);

                List<Member> members = guild.getMembersByName(username, false);
                if (lookedUpMembers != null) {
                    lookedUpMembers.addAll(members);
                }

                boolean any = false;
                for (Member member : members) {
                    mentions.add(getMemberMention(member));
                    any = true;
                    break;
                }

                if (!any && uncached) {
                    futures.add(lookupMemberMentions(guild, username, lookedUpMembers));
                }
            }
        }
        if (config.roles) {
            mentions.addAll(getRoleMentions(guild).values());
        }
        if (config.channels) {
            mentions.addAll(getChannelMentions(guild).values());
        }

        return CompletableFutureUtil.combine(futures).thenApply(lists -> {
            lists.forEach(mentions::addAll);

            // From longest to shortest
            return mentions.stream()
                    .sorted(Comparator.comparingInt(mention -> ((CachedMention) mention).searchLength()).reversed())
                    .collect(Collectors.toList());
        });
    }

    @Subscribe
    public void onGuildDelete(GuildLeaveEvent event) {
        long guildId = event.getGuild().getIdLong();
        memberMentionsCache.asMap().keySet().removeIf(pair -> pair.getKey() == guildId);
        roleMentions.remove(guildId);
        channelMentions.remove(guildId);
    }

    //
    // Member
    //

    private CompletableFuture<List<CachedMention>> lookupMemberMentions(
            Guild guild,
            String username,
            Set<Member> lookedUpMembers
    ) {
        CompletableFuture<List<Member>> memberFuture = new CompletableFuture<>();
        guild.retrieveMembersByPrefix(username, 100)
                .onSuccess(memberFuture::complete).onError(memberFuture::completeExceptionally);

        return memberFuture.thenApply(members -> {
            if (lookedUpMembers != null) {
                lookedUpMembers.addAll(members);
            }

            List<CachedMention> cachedMentions = new ArrayList<>();
            for (Member member : members) {
                cachedMentions.add(getMemberMention(member));
            }
            return cachedMentions;
        });
    }

    private Pair<Long, Long> getMemberKey(Member member) {
        return Pair.of(member.getGuild().getIdLong(), member.getIdLong());
    }

    private CachedMention getMemberMention(Member member) {
        return memberMentionsCache.get(
                getMemberKey(member),
                key -> convertMember(member)
        );
    }

    private CachedMention convertMember(Member member) {
        return new CachedMention(
                "@" + member.getUser().getName(),
                member.getAsMention(),
                CachedMention.Type.USER,
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

        memberMentionsCache.put(getMemberKey(member), convertMember(member));
    }

    @Subscribe
    public void onMemberUpdate(GuildMemberUpdateNicknameEvent event) {
        Member member = event.getMember();
        if (member.getGuild().getMemberCache().getElementById(member.getIdLong()) == null) {
            // Member is not cached
            return;
        }

        memberMentionsCache.put(getMemberKey(member), convertMember(member));
    }

    @Subscribe
    public void onMemberDelete(GuildMemberRemoveEvent event) {
        Member member = event.getMember();
        if (member == null) {
            return;
        }

        memberMentionsCache.invalidate(getMemberKey(member));
    }

    //
    // Role
    //

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
                CachedMention.Type.ROLE,
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

    private Map<Long, CachedMention> getChannelMentions(Guild guild) {
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
                CachedMention.Type.CHANNEL,
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
}
