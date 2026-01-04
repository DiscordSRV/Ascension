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

package com.discordsrv.common.core.placeholder.context;

import com.discordsrv.api.discord.DiscordAPI;
import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordChannel;
import com.discordsrv.api.discord.entity.guild.DiscordCustomEmoji;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderRemainder;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.utils.MiscUtil;

import java.util.function.BiFunction;

public class DiscordEntityContext {

    private final DiscordSRV discordSRV;

    public DiscordEntityContext(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    private SelfUser getSelfUser() {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            return null;
        }

        return jda.getSelfUser();
    }

    @Placeholder("bot_user")
    public DiscordUser selfUser() {
        return discordSRV.discordAPI().getUser(getSelfUser());
    }

    private <T> T entity(String plainId, BiFunction<DiscordAPI, Long, T> apiFunction) {
        try {
            long id = MiscUtil.parseLong(plainId);
            return apiFunction.apply(discordSRV.discordAPI(), id);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Placeholder("channel")
    public DiscordChannel channel(@PlaceholderRemainder String id) {
        return entity(id, DiscordAPI::getChannelById);
    }

    @Placeholder("user")
    public Task<DiscordUser> user(@PlaceholderRemainder String id) {
        return entity(id, DiscordAPI::retrieveUserById);
    }

    @Placeholder("server")
    public DiscordGuild server(@PlaceholderRemainder String id) {
        return entity(id, DiscordAPI::getGuildById);
    }

    @Placeholder("role")
    public DiscordRole role(@PlaceholderRemainder String id) {
        return entity(id, DiscordAPI::getRoleById);
    }

    @Placeholder("emoji")
    public DiscordCustomEmoji emoji(@PlaceholderRemainder String id) {
        return entity(id, DiscordAPI::getEmojiById);
    }

    @Placeholder("member")
    public Task<DiscordGuildMember> member(DiscordGuild guild, @PlaceholderRemainder String plainId) {
        return entity(plainId, (api, id) -> guild.retrieveMemberById(id));
    }
}
