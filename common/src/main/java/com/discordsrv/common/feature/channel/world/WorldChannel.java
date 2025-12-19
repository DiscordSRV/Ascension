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

package com.discordsrv.common.feature.channel.world;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.common.DiscordSRV;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

public class WorldChannel implements GameChannel {

    public static final String DEFAULT_OWNER_NAME = "minecraft";

    private final DiscordSRV discordSRV;
    private final String ownerName;
    private final String worldName;

    public WorldChannel(DiscordSRV discordSRV, String worldName) {
        this(discordSRV, DEFAULT_OWNER_NAME, worldName);
    }

    public WorldChannel(DiscordSRV discordSRV, Key world) {
        this(discordSRV, world.namespace(), world.value());
    }

    public WorldChannel(DiscordSRV discordSRV, String ownerName, String worldName) {
        this.discordSRV = discordSRV;
        this.ownerName = ownerName;
        this.worldName = worldName;
    }

    @Override
    public @NotNull String getOwnerName() {
        return ownerName;
    }

    @Override
    public @NotNull String getChannelName() {
        return worldName;
    }

    @Override
    public boolean isChat() {
        return true;
    }

    @Override
    public @NotNull Collection<? extends DiscordSRVPlayer> getRecipients() {
        return discordSRV.playerProvider().allPlayers().stream().filter(player -> worldName.equals(player.worldName())).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return GameChannel.toString(this);
    }
}
