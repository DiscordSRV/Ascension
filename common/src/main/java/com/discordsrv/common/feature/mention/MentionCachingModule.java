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
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.channels.MinecraftToDiscordChatConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.config.main.generic.DestinationConfig;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.permission.game.Permissions;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MentionCachingModule extends AbstractModule<DiscordSRV> {

    private static final Pattern USER_MENTION_PATTERN = Pattern.compile("(?<!<)@([a-z0-9_.]{2,32})");

    private final Map<Long, Map<Long, CachedMention>> memberMentions = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, CachedMention>> roleMentions = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, CachedMention>> channelMentions = new ConcurrentHashMap<>();

    public MentionCachingModule(DiscordSRV discordSRV) {
        super(discordSRV);
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
        for (Long guildId : new HashSet<>(memberMentions.keySet())) {
            if (shouldNotCache(guildId, config -> config.users)) {
                memberMentions.remove(guildId);
            }
        }
        for (Long guildId : new HashSet<>(roleMentions.keySet())) {
            if (shouldNotCache(guildId, config -> config.roles)) {
                memberMentions.remove(guildId);
            }
        }
        for (Long guildId : new HashSet<>(channelMentions.keySet())) {
            if (shouldNotCache(guildId, config -> config.channels)) {
                memberMentions.remove(guildId);
            }
        }
    }

    @Override
    public void disable() {
        memberMentions.clear();
        roleMentions.clear();
        channelMentions.clear();
    }

    private boolean shouldNotCache(long guildId, Predicate<MinecraftToDiscordChatConfig.Mentions> predicate) {
        DiscordGuild guild = discordSRV.discordAPI().getGuildById(guildId);
        if (guild == null) {
            return false;
        }

        for (BaseChannelConfig config : discordSRV.channelConfig().getAllChannels()) {
            if (!(config instanceof IChannelConfig)) {
                continue;
            }

            DestinationConfig destination = ((IChannelConfig) config).destination();
            if (!destination.contains(guild)) {
                continue;
            }

            MinecraftToDiscordChatConfig chatConfig = config.minecraftToDiscord;
            if (!chatConfig.enabled) {
                continue;
            }

            if (predicate.test(chatConfig.mentions)) {
                return false;
            }
        }
        return true;
    }

    private boolean canLookupUncached(MinecraftToDiscordChatConfig.Mentions config, IPlayer player) {
        return config.uncachedUsers && player.hasPermission(Permissions.MENTION_USER_LOOKUP);
    }

    public Task<List<CachedMention>> lookup(
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

        List<CachedMention> cachedMentions = new ArrayList<>();
        addAllIds(cachedMentions, () -> getChannelMentions(guild), channelIds);
        addAllIds(cachedMentions, () -> getRoleMentions(guild), roleIds);

        List<Task<CachedMention>> futures = new ArrayList<>();
        if (canLookupUncached(config, player)) {
            for (Long userId : userIds) {
                futures.add(
                        discordSRV.discordAPI()
                                .toTask(guild.retrieveMemberById(userId))
                                .thenApply(this::getMemberMention)
                );
            }
        } else {
            for (Long userId : userIds) {
                Member member = guild.getMemberById(userId);
                if (member != null) {
                    cachedMentions.add(getMemberMention(member));
                }
            }
        }

        return Task.allOf(futures).thenApply(userMentions -> {
            cachedMentions.addAll(userMentions);
            return cachedMentions;
        });
    }

    private void addAllIds(List<CachedMention> cachedMentions, Supplier<Map<Long, CachedMention>> mentionsSupplier, List<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, CachedMention> data = mentionsSupplier.get();
        for (Long id : ids) {
            CachedMention mention = data.get(id);
            if (mention != null) {
                cachedMentions.add(mention);
            }
        }
    }

    public Task<List<CachedMention>> lookup(
            MinecraftToDiscordChatConfig.Mentions config,
            Guild guild,
            IPlayer player,
            String messageContent,
            Set<Member> lookedUpMembers
    ) {
        List<Task<List<CachedMention>>> futures = new ArrayList<>();
        List<CachedMention> mentions = new ArrayList<>();

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

        return Task.allOf(futures).thenApply(lists -> {
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
        memberMentions.remove(guildId);
        roleMentions.remove(guildId);
        channelMentions.remove(guildId);
    }

    //
    // Member
    //

    private Task<List<CachedMention>> lookupMemberMentions(
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

            List<CachedMention> cachedMentions = new ArrayList<>();
            for (Member member : members) {
                cachedMentions.add(getMemberMention(member));
            }
            return cachedMentions;
        });
    }

    private CachedMention getMemberMention(Member member) {
        CachedMention cachedMention = getMemberMentions(member.getGuild()).get(member.getIdLong());
        if (cachedMention == null) {
            cachedMention = convertMember(member);
        }
        return cachedMention;
    }

    public Map<Long, CachedMention> getMemberMentions(Guild guild) {
        if (shouldNotCache(guild.getIdLong(), config -> config.users)) {
            return Collections.emptyMap();
        }
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
                CachedMention.Type.USER,
                member.getIdLong(),
                false
        );
    }

    @Subscribe
    public void onMemberAdd(GuildMemberJoinEvent event) {
        if (shouldNotCache(event.getGuild().getIdLong(), config -> config.users)) {
            return;
        }

        Member member = event.getMember();
        if (member.getGuild().getMemberCache().getElementById(member.getIdLong()) == null) {
            // Member is not cached
            return;
        }

        getMemberMentions(event.getGuild()).put(member.getIdLong(), convertMember(member));
    }

    @Subscribe
    public void onMemberUpdate(GuildMemberUpdateEvent event) {
        if (shouldNotCache(event.getGuild().getIdLong(), config -> config.users)) {
            return;
        }

        Member member = event.getMember();
        if (member.getGuild().getMemberCache().getElementById(member.getIdLong()) == null) {
            // Member is not cached
            return;
        }

        getMemberMentions(event.getGuild()).put(member.getIdLong(), convertMember(member));
    }

    @Subscribe
    public void onMemberDelete(GuildMemberRemoveEvent event) {
        if (shouldNotCache(event.getGuild().getIdLong(), config -> config.users)) {
            return;
        }

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
        if (shouldNotCache(guild.getIdLong(), config -> config.roles)) {
            return Collections.emptyMap();
        }
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
                role.getIdLong(),
                role.isMentionable()
        );
    }

    @Subscribe
    public void onRoleCreate(RoleCreateEvent event) {
        if (shouldNotCache(event.getGuild().getIdLong(), config -> config.roles)) {
            return;
        }

        Role role = event.getRole();
        getRoleMentions(event.getGuild()).put(role.getIdLong(), convertRole(role));
    }

    @Subscribe
    public void onRoleUpdate(RoleUpdateNameEvent event) {
        if (shouldNotCache(event.getGuild().getIdLong(), config -> config.roles)) {
            return;
        }

        Role role = event.getRole();
        getRoleMentions(event.getGuild()).put(role.getIdLong(), convertRole(role));
    }

    @Subscribe
    public void onRoleDelete(RoleDeleteEvent event) {
        if (shouldNotCache(event.getGuild().getIdLong(), config -> config.roles)) {
            return;
        }

        Role role = event.getRole();
        getRoleMentions(event.getGuild()).remove(role.getIdLong());
    }

    //
    // Channel
    //

    public Map<Long, CachedMention> getChannelMentions(Guild guild) {
        if (shouldNotCache(guild.getIdLong(), config -> config.channels)) {
            return Collections.emptyMap();
        }
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
                channel.getIdLong(),
                false
        );
    }

    @Subscribe
    public void onChannelCreate(ChannelCreateEvent event) {
        if (shouldNotCache(event.getGuild().getIdLong(), config -> config.channels)) {
            return;
        }
        if (!event.getChannelType().isGuild()) {
            return;
        }

        GuildChannel channel = (GuildChannel) event.getChannel();
        getChannelMentions(event.getGuild()).put(channel.getIdLong(), convertChannel(channel));
    }

    @Subscribe
    public void onChannelUpdate(ChannelUpdateNameEvent event) {
        if (shouldNotCache(event.getGuild().getIdLong(), config -> config.channels)) {
            return;
        }
        if (!event.getChannelType().isGuild()) {
            return;
        }

        GuildChannel channel = (GuildChannel) event.getChannel();
        getChannelMentions(event.getGuild()).put(channel.getIdLong(), convertChannel(channel));
    }

    @Subscribe
    public void onChannelDelete(ChannelDeleteEvent event) {
        if (shouldNotCache(event.getGuild().getIdLong(), config -> config.channels)) {
            return;
        }
        if (!event.getChannelType().isGuild()) {
            return;
        }

        GuildChannel channel = (GuildChannel) event.getChannel();
        getChannelMentions(event.getGuild()).remove(channel.getIdLong());
    }
}
