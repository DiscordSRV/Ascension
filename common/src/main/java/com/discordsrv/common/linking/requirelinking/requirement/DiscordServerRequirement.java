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

package com.discordsrv.common.linking.requirelinking.requirement;

import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.common.DiscordSRV;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class DiscordServerRequirement extends LongRequirement {

    private final DiscordSRV discordSRV;

    public DiscordServerRequirement(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public String name() {
        return "DiscordServer";
    }

    @Override
    public CompletableFuture<Boolean> isMet(Long value, UUID player, long userId) {
        DiscordGuild guild = discordSRV.discordAPI().getGuildById(value);
        if (guild == null) {
            return CompletableFuture.completedFuture(false);
        }

        return guild.retrieveMemberById(userId)
                .thenApply(Objects::nonNull);
    }
}
