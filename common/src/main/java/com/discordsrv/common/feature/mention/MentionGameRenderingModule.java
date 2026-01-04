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

package com.discordsrv.common.feature.mention;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.message.render.game.GameChatRenderEvent;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.channels.MinecraftToDiscordChatConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.events.player.PlayerConnectedEvent;
import com.discordsrv.common.helper.DestinationLookupHelper;
import com.discordsrv.common.util.ComponentUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.list.SetUniqueList;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MentionGameRenderingModule extends AbstractModule<DiscordSRV> {

    private final List<String> mentionSuggestions = new ArrayList<>();
    private Future<?> updateSuggestionsFuture = null;

    public MentionGameRenderingModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "MENTION_ANNOTATION"));
    }

    @Override
    public boolean isEnabled() {
        return isEnabledInAny(config -> config.mentions.renderMentionsInGame && config.mentions.any());
    }

    public boolean isEnabledInAny(Predicate<MinecraftToDiscordChatConfig> predicate) {
        for (BaseChannelConfig channelConfig : discordSRV.channelConfig().getAllChannels()) {
            MinecraftToDiscordChatConfig config = channelConfig.minecraftToDiscord;
            if (!config.enabled) {
                continue;
            }

            if (predicate.test(config)) {
                return true;
            }
        }
        return false;
    }

    public boolean isChatSuggestionsEnabledInAny() {
        return isEnabledInAny(config -> config.mentions.suggestMentionsCompletionsInGame);
    }

    @Override
    public void reload(Consumer<ReloadResult> resultConsumer) {
        if (isChatSuggestionsEnabledInAny()) {
            updateSuggestionsFuture = discordSRV.scheduler().runAtFixedRate(this::updateMentionSuggestions, Duration.ZERO, Duration.ofSeconds(45));
        }
    }

    @Override
    public void disable() {
        if (updateSuggestionsFuture != null) {
            updateSuggestionsFuture.cancel(false);
            updateSuggestionsFuture = null;
        }
        for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
            player.removeChatSuggestions(mentionSuggestions);
        }
        mentionSuggestions.clear();
    }

    private void updateMentionSuggestions() {
        MentionCachingModule module = discordSRV.getModule(MentionCachingModule.class);
        if (module == null) {
            return;
        }

        BaseChannelConfig config = discordSRV.channelConfig().resolveDefault();
        List<DiscordGuild> guilds = getGuilds(config);

        List<CachedMention> mentions = new ArrayList<>(512);
        for (DiscordGuild discordGuild : guilds) {
            Guild guild = discordGuild.asJDA();
            if (guilds.size() == 1) {
                mentions.addAll(module.getMemberCache().getGuildCache(guild).values());
            }
            mentions.addAll(module.getRoleCache().getGuildCache(guild).values());
            mentions.addAll(module.getChannelCache().getGuildCache(guild).values());
        }

        List<String> newMentionSuggestions = mentions.stream()
                .map(CachedMention::plain)
                .sorted()
                .distinct()
                .limit(Short.MAX_VALUE)
                .collect(Collectors.toList());

        List<String> newSuggestions = ListUtils.removeAll(newMentionSuggestions, mentionSuggestions);
        List<String> removedSuggestions = ListUtils.removeAll(mentionSuggestions, newMentionSuggestions);

        for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
            if (!newSuggestions.isEmpty()) {
                player.addChatSuggestions(newSuggestions);
            }
            if (!removedSuggestions.isEmpty()) {
                player.removeChatSuggestions(removedSuggestions);
            }
        }
        synchronized (mentionSuggestions) {
            mentionSuggestions.clear();
            mentionSuggestions.addAll(newMentionSuggestions);
        }
    }

    private List<DiscordGuild> getGuilds(BaseChannelConfig config) {
        if (!(config instanceof IChannelConfig)) {
            return Collections.emptyList();
        }

        DestinationLookupHelper.LookupResult lookupResult = discordSRV.destinations()
                .lookupDestination(((IChannelConfig) config).destination(), false, false)
                .join();
        List<DiscordGuild> guilds = SetUniqueList.setUniqueList(new ArrayList<>());
        for (DiscordGuildMessageChannel channel : lookupResult.channels()) {
            guilds.add(channel.getGuild());
        }
        return guilds;
    }

    @Subscribe(ignoreCancelled = false, ignoreProcessed = false)
    public void onGameChatRender(GameChatRenderEvent event) {
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        GameChannel gameChannel = event.getChannel();
        BaseChannelConfig config = gameChannel != null
                                   ? discordSRV.channelConfig().get(gameChannel)
                                   : discordSRV.channelConfig().resolveDefault();
        if (config == null || !config.minecraftToDiscord.mentions.renderMentionsInGame) {
            return;
        }

        MentionCachingModule module = discordSRV.getModule(MentionCachingModule.class);
        if (module == null) {
            return;
        }

        List<DiscordGuild> guilds = getGuilds(config);
        if (guilds.isEmpty()) {
            return;
        }

        DiscordGuild singleGuild = guilds.size() == 1 ? guilds.get(0) : null;
        Set<Member> lookedUpMembers = singleGuild != null ? null : new CopyOnWriteArraySet<>();

        Component message = ComponentUtil.fromAPI(event.getMessage());
        String messageContent = discordSRV.componentFactory().plainSerializer().serialize(message);

        List<CachedMention> cachedMentions = new ArrayList<>();
        for (DiscordGuild guild : guilds) {
            cachedMentions.addAll(
                    module.lookup(
                            config.minecraftToDiscord.mentions,
                            guild.asJDA(),
                            (IPlayer) event.getPlayer(),
                            messageContent,
                            lookedUpMembers
                    ).join()
            );
        }

        Set<DiscordGuildMember> members;
        if (lookedUpMembers != null) {
            members = new HashSet<>(lookedUpMembers.size());
            for (Member member : lookedUpMembers) {
                members.add(discordSRV.discordAPI().getGuildMember(member));
            }
        } else {
            members = Collections.emptySet();
        }

        for (CachedMention cachedMention : cachedMentions) {
            message = message.replaceText(
                    TextReplacementConfig.builder().match(cachedMention.search())
                            .replacement((builder) -> replacement(cachedMention, config, singleGuild, members))
                            .build()
            );
        }
        event.process(ComponentUtil.toAPI(message));
    }

    private Component replacement(CachedMention mention, BaseChannelConfig config, DiscordGuild guild, Set<DiscordGuildMember> members) {
        switch (mention.type()) {
            case ROLE:
                return discordSRV.componentFactory().makeRoleMention(mention.id(), config);
            case USER:
                return discordSRV.componentFactory().makeUserMention(mention.id(), config, guild, null, members);
            case CHANNEL:
                return discordSRV.componentFactory().makeChannelMention(mention.id(), config);
        }
        return Component.text(mention.plain());
    }

    @Subscribe
    public void onPlayerConnected(PlayerConnectedEvent event) {
        if (!isChatSuggestionsEnabledInAny()) {
            return;
        }

        event.player().addChatSuggestions(mentionSuggestions);
    }
}
