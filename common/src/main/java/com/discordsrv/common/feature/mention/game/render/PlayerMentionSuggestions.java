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

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.channels.MinecraftToDiscordChatConfig;
import com.discordsrv.common.feature.mention.Mention;
import com.discordsrv.common.feature.mention.MentionUtil;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import org.apache.commons.collections4.ListUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerMentionSuggestions {

    private final List<String> activeSuggestions = new ArrayList<>(128);

    public void updateSuggestions(
            DiscordSRV discordSRV,
            IPlayer player,
            DiscordUser playerLinkedUser,
            List<Mention> newSuggestions,
            MinecraftToDiscordChatConfig.Mentions config
    ) {
        synchronized (activeSuggestions) {
            List<String> newMentionSuggestions = newSuggestions.stream()
                    .filter(mention -> MentionUtil.isAllowedToMention(config, player, mention))
                    .filter(mention -> {
                        if (mention.type() != Mention.Type.CHANNEL) {
                            return true;
                        }

                        JDA jda = discordSRV.jda();
                        GuildChannel guildChannel = jda != null ? jda.getGuildChannelById(mention.id()) : null;
                        if (guildChannel == null) {
                            return false;
                        }

                        return MentionUtil.canMentionChannel(guildChannel, playerLinkedUser);
                    })
                    .sorted(Comparator.comparingInt(mention -> mention.type().priority()))
                    .map(Mention::plain)
                    .distinct()
                    .limit(Short.MAX_VALUE)
                    .collect(Collectors.toList());

            List<String> removedSuggestions = ListUtils.removeAll(activeSuggestions, newMentionSuggestions);
            player.removeChatSuggestions(removedSuggestions);
            activeSuggestions.removeAll(removedSuggestions);

            List<String> addedSuggestions = ListUtils.removeAll(newMentionSuggestions, activeSuggestions);
            player.addChatSuggestions(addedSuggestions);
            activeSuggestions.addAll(addedSuggestions);
        }
    }

    public void removeSuggestions(IPlayer player) {
        synchronized (activeSuggestions) {
            player.removeChatSuggestions(activeSuggestions);
            activeSuggestions.clear();
        }
    }
}
