/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.feature.channel.global;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.common.DiscordSRV;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class GlobalChannel implements GameChannel {

    private final DiscordSRV discordSRV;

    public GlobalChannel(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public @NotNull String getOwnerName() {
        return "DiscordSRV";
    }

    @Override
    public @NotNull String getChannelName() {
        return GameChannel.DEFAULT_NAME;
    }

    @Override
    public boolean isChat() {
        return true;
    }

    @Override
    public @NotNull Collection<? extends DiscordSRVPlayer> getRecipients() {
        return discordSRV.playerProvider().allPlayers();
    }
}
