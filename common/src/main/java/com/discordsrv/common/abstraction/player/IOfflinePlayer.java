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

package com.discordsrv.common.abstraction.player;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import com.discordsrv.common.feature.profile.Profile;
import net.kyori.adventure.identity.Identified;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@PlaceholderPrefix("player_")
public interface IOfflinePlayer extends Identified {

    DiscordSRV discordSRV();

    @Placeholder("profile")
    default Task<Profile> profile() {
        return discordSRV().profileManager().lookupProfile(uniqueId());
    }

    @Placeholder("linked_user")
    default Task<@Nullable DiscordUser> linkedUser() {
        return profile().thenApply(profile -> profile.isLinked() ? profile.userId() : null)
                .then(userId -> discordSRV().discordAPI().retrieveUserById(userId));
    }

    @Placeholder("linked_server_member")
    default Task<@Nullable DiscordGuildMember> linkedMember(DiscordGuild guild) {
        return profile().thenApply(profile -> profile.isLinked() ? profile.userId() : null)
                .then(guild::retrieveMemberById);
    }

    @Placeholder("name")
    @Nullable
    String username();

    @Placeholder("uuid")
    @NotNull
    default UUID uniqueId() {
        return identity().uuid();
    }

    @Nullable
    @Placeholder("skin")
    SkinInfo skinInfo();
}
