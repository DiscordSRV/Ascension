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

package com.discordsrv.common.feature.mention.game.render;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.entity.DiscordUser;
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
import com.discordsrv.common.events.player.PlayerDisconnectedEvent;
import com.discordsrv.common.feature.linking.LinkProvider;
import com.discordsrv.common.feature.mention.Mention;
import com.discordsrv.common.feature.mention.cache.MentionCachingModule;
import com.discordsrv.common.helper.DestinationLookupHelper;
import com.discordsrv.common.util.ComponentUtil;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.apache.commons.collections4.list.SetUniqueList;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MentionGameRenderingModule extends AbstractModule<DiscordSRV> {

    private final List<Mention> allMentionSuggestions = new ArrayList<>();
    private final Map<UUID, PlayerMentionSuggestions> currentSuggestions = new HashMap<>();
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

        synchronized (currentSuggestions) {
            for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
                PlayerMentionSuggestions suggestions = currentSuggestions.remove(player.uniqueId());
                if (suggestions == null) {
                    continue;
                }

                suggestions.removeSuggestions(player);
            }
        }
    }

    @Subscribe
    public void onPlayerConnected(PlayerConnectedEvent event) {
        if (!isChatSuggestionsEnabledInAny()) {
            return;
        }

        updateMentionSuggestions(event.player());
    }

    @Subscribe
    public void onPlayerDisconnected(PlayerDisconnectedEvent event) {
        synchronized (currentSuggestions) {
            currentSuggestions.remove(event.player().uniqueId());
        }
    }

    private void updateMentionSuggestions(IPlayer player) {
        BaseChannelConfig config = discordSRV.channelConfig().resolveDefault();
        if (config == null) {
            return;
        }

        LinkProvider linkProvider = discordSRV.linkProvider();
        DiscordUser playerLinkedUser =
                linkProvider != null
                ? linkProvider.getCached(player.uniqueId())
                        .map(link -> discordSRV.discordAPI().getUserById(link.userId()))
                        .orElse(null)
                : null;

        synchronized (currentSuggestions) {
            PlayerMentionSuggestions suggestions = currentSuggestions.computeIfAbsent(player.uniqueId(), key -> new PlayerMentionSuggestions());
            synchronized (allMentionSuggestions) {
                suggestions.updateSuggestions(discordSRV, player, playerLinkedUser, allMentionSuggestions, config.minecraftToDiscord.mentions);
            }
            currentSuggestions.put(player.uniqueId(), suggestions);
        }
    }

    private void updateMentionSuggestions() {
        MentionCachingModule module = discordSRV.getModule(MentionCachingModule.class);
        if (module == null) {
            return;
        }

        BaseChannelConfig config = discordSRV.channelConfig().resolveDefault();
        List<DiscordGuild> guilds = getGuilds(config);

        List<Mention> mentions = new ArrayList<>(512);
        for (DiscordGuild discordGuild : guilds) {
            Guild guild = discordGuild.asJDA();
            if (guilds.size() == 1) {
                mentions.addAll(module.getMemberCache().getGuildCache(guild).values());
            }
            mentions.addAll(module.getChannelCache().getGuildCache(guild).values());

            mentions.addAll(module.getRoleCache().getGuildCache(guild).values());
            // Special roles
            mentions.add(module.getEveryoneRole(guild));
            mentions.add(module.getHereRole(guild));
        }

        synchronized (allMentionSuggestions) {
            allMentionSuggestions.clear();
            allMentionSuggestions.addAll(mentions);
        }

        for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
            updateMentionSuggestions(player);
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

        IPlayer player = (IPlayer) event.getPlayer();
        GameChannel gameChannel = event.getChannel();
        BaseChannelConfig config = gameChannel != null
                                   ? discordSRV.channelConfig().resolve(gameChannel)
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

        LinkProvider linkProvider = discordSRV.linkProvider();
        DiscordUser playerLinkedUser =
                linkProvider != null && singleGuild != null
                ? linkProvider.getCached(player.uniqueId())
                        .map(link -> discordSRV.discordAPI().getUserById(link.userId()))
                        .orElse(null)
                : null;

        Component message = ComponentUtil.fromAPI(event.getMessage());
        String messageContent = discordSRV.componentFactory().plainSerializer().serialize(message);

        Set<Member> lookedUpMembers = singleGuild != null ? null : new CopyOnWriteArraySet<>();
        List<Mention> mentions = new ArrayList<>();
        for (DiscordGuild guild : guilds) {
            Guild jdaGuild = guild.asJDA();
            Member playerLinkedMember = playerLinkedUser != null ? jdaGuild.getMemberById(playerLinkedUser.getId()) : null;

            mentions.addAll(
                    module.lookup(
                            config.minecraftToDiscord.mentions,
                            jdaGuild,
                            player,
                            playerLinkedMember,
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

        for (Mention mention : mentions) {
            message = message.replaceText(
                    TextReplacementConfig.builder().match(mention.search())
                            .replacement((builder) -> replacement(mention, config, singleGuild, playerLinkedUser, members))
                            .build()
            );
        }
        event.process(ComponentUtil.toAPI(message));
    }

    private Component replacement(Mention mention, BaseChannelConfig config, DiscordGuild guild, DiscordUser requester, Set<DiscordGuildMember> members) {
        switch (mention.type()) {
            case ROLE:
                return discordSRV.componentFactory().makeRoleMention(mention.id(), config);
            case USER:
                return discordSRV.componentFactory().makeUserMention(mention.id(), config, guild, null, members);
            case CHANNEL:
                return discordSRV.componentFactory().makeChannelMention(mention.id(), config, requester);
            case EVERYONE:
                return discordSRV.componentFactory().makeEveryoneRoleMention(mention.id(), config);
            case HERE:
                return discordSRV.componentFactory().makeHereRoleMention(mention.id(), config);
        }
        return Component.text(mention.plain());
    }
}
