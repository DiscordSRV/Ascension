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

package com.discordsrv.common.feature.mention.cache;

import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.channels.MinecraftToDiscordChatConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.feature.mention.Mention;
import com.discordsrv.common.permission.game.Permissions;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateNameEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberUpdateEvent;
import net.dv8tion.jda.api.events.role.RoleCreateEvent;
import net.dv8tion.jda.api.events.role.RoleDeleteEvent;
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MentionCachingModule extends AbstractModule<DiscordSRV> {

    private static final Pattern USER_MENTION_PATTERN = Pattern.compile("(?<!<)@([a-z0-9_.]{2,32})");

    private final Map<Long, Pair<Mention, Mention>> guildToEveryoneAndHere = new ConcurrentHashMap<>();
    private final MentionCache<Member> memberCache;
    private final MentionCache<Role> roleCache;
    private final MentionCache<GuildChannel> channelCache;

    public MentionCachingModule(DiscordSRV discordSRV) {
        super(discordSRV);
        this.memberCache = new MentionCache<>(
                discordSRV,
                config -> config.users,
                Member::getGuild,
                Guild::getMembers,
                Mention::new
        );
        this.roleCache = new MentionCache<>(
                discordSRV,
                config -> config.roles,
                Role::getGuild,
                guild -> guild.getRoles().stream().filter(role -> !role.isPublicRole()).collect(Collectors.toList()),
                Mention::new
        );
        this.channelCache = new MentionCache<>(
                discordSRV,
                config -> config.channels,
                GuildChannel::getGuild,
                Guild::getChannels,
                Mention::new
        );
    }

    protected DiscordSRV discordSRV() {
        return discordSRV;
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
    public void reload(Consumer<ReloadResult> resultConsumer) {
        memberCache.checkNoLongerNeededCaches();
        roleCache.checkNoLongerNeededCaches();
        channelCache.checkNoLongerNeededCaches();
    }

    @Override
    public void disable() {
        guildToEveryoneAndHere.clear();
        memberCache.clear();
        roleCache.clear();
        channelCache.clear();
    }

    private Pair<Mention, Mention> getEveryoneAndHere(Guild guild) {
        long guildId = guild.getIdLong();
        Pair<Mention, Mention> cached = guildToEveryoneAndHere.get(guildId);
        if (cached != null) {
            return cached;
        }

        Role role = guild.getPublicRole();
        Mention everyone = new Mention(role, "@everyone");
        Mention here = new Mention(role, "@here");

        Pair<Mention, Mention> created = Pair.of(everyone, here);
        guildToEveryoneAndHere.put(guildId, created);
        return created;
    }

    public Mention getEveryoneRole(Guild guild) {
        return getEveryoneAndHere(guild).getLeft();
    }

    public Mention getHereRole(Guild guild) {
        return getEveryoneAndHere(guild).getRight();
    }

    public MentionCache<Member> getMemberCache() {
        return memberCache;
    }

    public MentionCache<Role> getRoleCache() {
        return roleCache;
    }

    public MentionCache<GuildChannel> getChannelCache() {
        return channelCache;
    }

    private boolean canLookupUncached(MinecraftToDiscordChatConfig.Mentions config, IPlayer player) {
        return config.uncachedUsers && player.hasPermission(Permissions.MENTION_USER_LOOKUP);
    }

    public Task<List<Mention>> lookup(
            MinecraftToDiscordChatConfig.Mentions config,
            Guild guild,
            IPlayer player,
            List<Pair<Message.MentionType, String>> preResolvedMentions
    ) {
        List<Long> userIds = new ArrayList<>();
        List<Long> roleIds = new ArrayList<>();
        List<Long> channelIds = new ArrayList<>();
        for (Pair<Message.MentionType, String> mention : preResolvedMentions) {
            Message.MentionType mentionType = mention.getKey();
            try {
                switch (mentionType) {
                    case USER: {
                        userIds.add(Long.parseLong(mention.getValue()));
                        break;
                    }
                    case ROLE: {
                        roleIds.add(Long.parseLong(mention.getValue()));
                        break;
                    }
                    case CHANNEL: {
                        channelIds.add(Long.parseLong(mention.getValue()));
                        break;
                    }
                    default: {
                        break;
                    }
                }
            } catch (NumberFormatException ignored) {}
        }

        List<Mention> mentions = new ArrayList<>();
        addAllIds(mentions, () -> channelCache.getGuildCache(guild), channelIds);
        addAllIds(mentions, () -> roleCache.getGuildCache(guild), roleIds);

        List<Task<Mention>> futures = new ArrayList<>();
        if (canLookupUncached(config, player)) {
            for (Long userId : userIds) {
                futures.add(
                        discordSRV.discordAPI()
                                .toTask(guild.retrieveMemberById(userId))
                                .thenApply(memberCache::convert)
                );
            }
        } else {
            for (Long userId : userIds) {
                Member member = guild.getMemberById(userId);
                if (member != null) {
                    mentions.add(memberCache.convert(member));
                }
            }
        }

        return Task.allOf(futures).thenApply(userMentions -> {
            mentions.addAll(userMentions);
            return mentions;
        });
    }

    private void addAllIds(List<Mention> mentions, Supplier<Map<Long, Mention>> mentionsSupplier, List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, Mention> data = mentionsSupplier.get();
        for (Long id : ids) {
            Mention mention = data.get(id);
            if (mention != null) {
                mentions.add(mention);
            }
        }
    }

    public Task<List<Mention>> lookup(
            MinecraftToDiscordChatConfig.Mentions config,
            Guild guild,
            IPlayer player,
            Member playerLinkedMember,
            String messageContent,
            Set<Member> lookedUpMembers
    ) {
        List<Task<List<Mention>>> futures = new ArrayList<>();
        List<Mention> mentions = new ArrayList<>();

        if (config.users) {
            boolean uncached = canLookupUncached(config, player);

            Matcher matcher = USER_MENTION_PATTERN.matcher(messageContent);
            while (matcher.find()) {
                String username = matcher.group(1);

                List<Member> members = guild.getMembersByName(username, false);
                if (lookedUpMembers != null) {
                    lookedUpMembers.addAll(members);
                }

                boolean any = false;
                for (Member member : members) {
                    Mention mention = memberCache.get(member.getGuild(), member.getIdLong());
                    if (mention == null) {
                        mention = memberCache.convert(member);
                    }
                    mentions.add(mention);
                    any = true;
                    break;
                }

                if (!any && uncached) {
                    futures.add(lookupMemberMentions(guild, username, lookedUpMembers));
                }
            }
        }
        if (config.roles) {
            mentions.addAll(roleCache.getGuildCache(guild).values());
            // Special roles
            mentions.add(getEveryoneRole(guild));
            mentions.add(getHereRole(guild));
        }
        if (config.channels) {
            IPermissionHolder permissionHolder = playerLinkedMember != null ? playerLinkedMember : guild.getPublicRole();

            // Only include channel mentions which the user can see
            for (Mention mention : channelCache.getGuildCache(guild).values()) {
                long channelId = mention.id();
                GuildChannel guildChannel = guild.getGuildChannelById(channelId);
                if (guildChannel == null || !permissionHolder.hasPermission(guildChannel, Permission.VIEW_CHANNEL)) {
                    continue;
                }

                mentions.add(mention);
            }
        }

        return Task.allOf(futures).thenApply(lists -> {
            lists.forEach(mentions::addAll);

            // From longest to shortest
            return mentions.stream()
                    .sorted(Comparator.comparingInt(mention -> ((Mention) mention).searchLength()).reversed())
                    .collect(Collectors.toList());
        });
    }

    @Subscribe
    public void onGuildLeave(GuildLeaveEvent event) {
        long guildId = event.getGuild().getIdLong();
        guildToEveryoneAndHere.remove(guildId);
        memberCache.removeGuild(guildId);
        roleCache.removeGuild(guildId);
        channelCache.removeGuild(guildId);
    }

    //
    // Member
    //

    private Task<List<Mention>> lookupMemberMentions(
            Guild guild,
            String username,
            Set<Member> lookedUpMembers
    ) {
        Task<List<Member>> memberFuture = new Task<>();
        guild.retrieveMembersByPrefix(username, 100)
                .onSuccess(memberFuture::complete).onError(memberFuture::completeExceptionally);

        return memberFuture.thenApply(members -> {
            if (lookedUpMembers != null) {
                lookedUpMembers.addAll(members);
            }

            List<Mention> mentions = new ArrayList<>();
            for (Member member : members) {
                mentions.add(memberCache.convert(member));
            }
            return mentions;
        });
    }

    @Subscribe
    public void onMemberAdd(GuildMemberJoinEvent event) {
        memberCache.addOrUpdate(event.getMember());
    }

    @Subscribe
    public void onMemberUpdate(GuildMemberUpdateEvent event) {
        memberCache.addOrUpdate(event.getMember());
    }

    @Subscribe
    public void onMemberDelete(GuildMemberRemoveEvent event) {
        memberCache.remove(event.getGuild(), event.getUser().getIdLong());
    }

    //
    // Role
    //

    @Subscribe
    public void onRoleCreate(RoleCreateEvent event) {
        roleCache.addOrUpdate(event.getRole());
    }

    @Subscribe
    public void onRoleUpdate(RoleUpdateNameEvent event) {
        roleCache.addOrUpdate(event.getRole());
    }

    @Subscribe
    public void onRoleDelete(RoleDeleteEvent event) {
        roleCache.remove(event.getGuild(), event.getRole().getIdLong());
    }

    //
    // Channel
    //

    @Subscribe
    public void onChannelCreate(ChannelCreateEvent event) {
        Channel channel = event.getChannel();
        if (channel instanceof GuildChannel) {
            channelCache.addOrUpdate((GuildChannel) channel);
        }
    }

    @Subscribe
    public void onChannelUpdate(ChannelUpdateNameEvent event) {
        Channel channel = event.getChannel();
        if (channel instanceof GuildChannel) {
            channelCache.addOrUpdate((GuildChannel) channel);
        }
    }

    @Subscribe
    public void onChannelDelete(ChannelDeleteEvent event) {
        Channel channel = event.getChannel();
        if (channel instanceof GuildChannel) {
            GuildChannel guildChannel = (GuildChannel) channel;
            channelCache.remove(guildChannel.getGuild(), guildChannel.getIdLong());
        }
    }
}
