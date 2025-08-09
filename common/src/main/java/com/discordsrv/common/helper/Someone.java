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

package com.discordsrv.common.helper;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.api.profile.Profile;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.feature.linking.AccountLink;
import com.discordsrv.common.feature.linking.LinkProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a subject that is
 * - a Minecraft player, or
 * - a Discord user, or
 * - a Minecraft player and their linked Discord user
 * with helper methods. Not to be stored persistently.
 *
 * @see #resolve()
 * @see #getAccountLink()
 * @see #profile()
 * @see #user()
 * @see #guildMember(DiscordGuild)
 * @see #onlinePlayer()
 * @see #offlinePlayer()
 */
public class Someone {

    public static Someone.Resolved of(@NotNull DiscordSRV discordSRV, @NotNull DiscordSRVPlayer player, @NotNull DiscordUser user) {
        return of(discordSRV, player.uniqueId(), user.getId());
    }

    public static Someone.Resolved of(@NotNull DiscordSRV discordSRV, @NotNull UUID playerUUID, long userId) {
        return new Someone.Resolved(discordSRV, playerUUID, userId);
    }

    public static Someone.Resolved of(@NotNull DiscordSRV discordSRV, @NotNull AccountLink accountLink) {
        return new Someone.Resolved(discordSRV, accountLink.playerUUID(), accountLink.userId());
    }

    public static Someone of(@NotNull DiscordSRV discordSRV, @NotNull DiscordSRVPlayer player) {
        return of(discordSRV, player.uniqueId());
    }

    public static Someone of(@NotNull DiscordSRV discordSRV, @NotNull UUID playerUUID) {
        return new Someone(discordSRV, playerUUID, null);
    }

    public static Someone of(@NotNull DiscordSRV discordSRV, @NotNull DiscordUser user) {
        return of(discordSRV, user.getId());
    }

    public static Someone of(@NotNull DiscordSRV discordSRV, long userId) {
        return new Someone(discordSRV, null, userId);
    }

    private final DiscordSRV discordSRV;
    private final UUID playerUUID;
    private final Long userId;

    private Task<DiscordUser> user;
    private final Map<Long, Task<DiscordGuildMember>> members = new HashMap<>();

    private Someone(@NotNull DiscordSRV discordSRV, @Nullable UUID playerUUID, @Nullable Long userId) {
        this.discordSRV = discordSRV;
        this.playerUUID = playerUUID;
        this.userId = userId;
    }

    @Nullable
    public UUID playerUUID() {
        return playerUUID;
    }

    @Nullable
    public Long userId() {
        return userId;
    }

    private <T> T throwIllegal() {
        throw new IllegalStateException("Cannot have Someone instance without either a Player UUID or User Id");
    }

    @NotNull
    public Task<@NotNull Profile> profile() {
        if (playerUUID != null) {
            return discordSRV.profileManager().getProfile(playerUUID).thenApply(profile -> profile);
        } else if (userId != null) {
            return discordSRV.profileManager().getProfile(userId).thenApply(profile -> profile);
        } else {
            return throwIllegal();
        }
    }

    @NotNull
    public Task<Someone.@Nullable Resolved> resolve() {
        if (this instanceof Resolved) {
            return Task.completed((Resolved) this);
        }
        if (playerUUID != null && userId != null) {
            return Task.completed(of(discordSRV, playerUUID, userId));
        }

        return getAccountLink().thenApply(link -> link != null ? of(discordSRV, link.playerUUID(), link.userId()) : null);
    }

    public Task<@Nullable AccountLink> getAccountLink() {
        if (playerUUID == null && userId == null) {
            return throwIllegal();
        }

        LinkProvider linkProvider = discordSRV.linkProvider();
        if (linkProvider == null) {
            return Task.completed(null);
        }

        if (playerUUID != null) {
            return linkProvider.get(playerUUID).thenApply(link -> link.orElse(null));
        }
        return linkProvider.get(userId).thenApply(link -> link.orElse(null));
    }

    @Nullable
    public IPlayer onlinePlayer() {
        if (playerUUID == null) {
            return null;
        }
        return discordSRV.playerProvider().player(playerUUID);
    }

    @Nullable
    public Task<IOfflinePlayer> offlinePlayer() {
        if (playerUUID == null) {
            return Task.completed(null);
        }
        return discordSRV.playerProvider().lookupOfflinePlayer(playerUUID);
    }

    public Task<@Nullable DiscordUser> user() {
        if (userId == null) {
            return Task.completed(null);
        }
        synchronized (members) {
            if (user != null) {
                return user;
            }
            return user = discordSRV.discordAPI().retrieveUserById(userId);
        }
    }

    public Task<DiscordGuildMember> guildMember(@NotNull DiscordGuild guild) {
        if (userId == null) {
            return Task.completed(null);
        }
        synchronized (members) {
            long guildId = guild.getId();
            Task<DiscordGuildMember> member = members.get(guildId);
            if (member != null) {
                return member;
            }

            member = guild.retrieveMemberById(userId);
            members.put(guildId, member);
            return member;
        }
    }

    @Override
    public String toString() {
        if (playerUUID != null && userId != null) {
            return playerUUID + "(" + Long.toUnsignedString(userId) + ")";
        }
        return playerUUID != null ? playerUUID.toString() : Long.toUnsignedString(Objects.requireNonNull(userId));
    }

    @SuppressWarnings("DataFlowIssue") // Guaranteed to not be null
    public static class Resolved extends Someone {

        private Resolved(@NotNull DiscordSRV discordSRV, @NotNull UUID playerUUID, @NotNull Long userId) {
            super(discordSRV, playerUUID, userId);
        }

        @Override
        public @NotNull UUID playerUUID() {
            return super.playerUUID();
        }

        @Override
        public @NotNull Long userId() {
            return super.userId();
        }
    }
}
