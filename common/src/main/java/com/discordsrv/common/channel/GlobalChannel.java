/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.channel;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.player.IPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

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
        return "global";
    }

    @Override
    public boolean isChat() {
        return true;
    }

    @Override
    public void sendMessage(@NotNull MinecraftComponent minecraftComponent) {
        Component component = ComponentUtil.fromAPI(minecraftComponent);
        for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
            player.sendMessage(component);
        }
    }
}
