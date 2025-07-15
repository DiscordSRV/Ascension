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

import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.message.render.game.GameChatRenderEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.channels.MinecraftToDiscordChatConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.helper.DestinationLookupHelper;
import com.discordsrv.common.util.ComponentUtil;
import net.dv8tion.jda.api.entities.Member;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class MentionGameRenderingModule extends AbstractModule<DiscordSRV> {

    public MentionGameRenderingModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "MENTION_ANNOTATION"));
    }

    @Override
    public boolean isEnabled() {
        for (BaseChannelConfig channelConfig : discordSRV.channelConfig().getAllChannels()) {
            MinecraftToDiscordChatConfig config = channelConfig.minecraftToDiscord;
            if (!config.enabled) {
                continue;
            }

            MinecraftToDiscordChatConfig.Mentions mentions = config.mentions;
            if (mentions.renderMentionsInGame && mentions.any()) {
                return true;
            }

        }
        return false;
    }

    @Subscribe(ignoreCancelled = false, ignoreProcessed = false)
    public void onGameChatRender(GameChatRenderEvent event) {
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        BaseChannelConfig config = discordSRV.channelConfig().get(event.getChannel());
        if (config == null || !(config instanceof IChannelConfig) || !config.minecraftToDiscord.mentions.renderMentionsInGame) {
            return;
        }

        MentionCachingModule module = discordSRV.getModule(MentionCachingModule.class);
        if (module == null) {
            return;
        }

        DestinationLookupHelper.LookupResult lookupResult = discordSRV.destinations()
                .lookupDestination(((IChannelConfig) config).destination(), false, false)
                .join();
        Set<DiscordGuild> guilds = new LinkedHashSet<>();
        for (DiscordGuildMessageChannel channel : lookupResult.channels()) {
            guilds.add(channel.getGuild());
        }

        Component message = ComponentUtil.fromAPI(event.getMessage());
        String messageContent = discordSRV.componentFactory().plainSerializer().serialize(message);

        DiscordGuild singleGuild;
        if (guilds.size() == 1) {
            singleGuild = guilds.iterator().next();
        } else {
            singleGuild = null;
        }

        Set<Member> lookedUpMembers = singleGuild != null ? null : new CopyOnWriteArraySet<>();

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
                            .replacement(() -> replacement(cachedMention, config, singleGuild, members))
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
}
