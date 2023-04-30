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

package com.discordsrv.bukkit.listener.award;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;
import com.github.benmanes.caffeine.cache.Cache;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractBukkitAwardListener implements Listener {

    private final Cache<UUID, AtomicInteger> advancementCount;
    protected final Logger logger;
    protected final IBukkitAwardForwarder forwarder;

    public AbstractBukkitAwardListener(DiscordSRV discordSRV, IBukkitAwardForwarder forwarder) {
        this.advancementCount = discordSRV.caffeineBuilder()
                .expireAfterWrite(5, TimeUnit.SECONDS)
                .build();
        this.logger = new NamedLogger(discordSRV, "AWARD_LISTENER");
        this.forwarder = forwarder;
    }

    @SuppressWarnings("DataFlowIssue") // Not possible
    public boolean checkIfShouldSkip(Player player) {
        int count = advancementCount.get(player.getUniqueId(), key -> new AtomicInteger(0)).incrementAndGet();
        boolean skip = count >= 5;
        if (skip && (count % 5 == 0)) {
            logger.debug("Skipping advancement/achievement processing for player: " + player.getName()
                                 + " currently at " + advancementCount + " advancements/achievements per 5 seconds");
        }
        return skip;
    }
}
