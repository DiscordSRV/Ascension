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

package com.discordsrv.common.config.main.generic;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.sync.enums.SyncDirection;
import com.discordsrv.common.abstraction.sync.enums.SyncSide;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.Arrays;

/**
 * A configuration for a synchronizable.
 * @param <C> the implementation type
 * @param <G> the game identifier
 * @param <D> the Discord identifier
 */
@ConfigSerializable
public abstract class AbstractSyncConfig<C extends AbstractSyncConfig<C, G, D>, G, D> extends SyncConfig {

    public abstract boolean isSet();

    public abstract G gameId();
    public abstract D discordId();
    public abstract boolean isSameAs(C otherConfig);

    public abstract String describe();

    public boolean validate(String syncName, DiscordSRV discordSRV) {
        String label = syncName + " (" + describe() + ")";
        boolean invalidTieBreaker = false, invalidDirection = false;
        for (SyncSide tieBreaker : tieBreakers.all()) {
            if (tieBreaker == null) {
                invalidTieBreaker = true;
                discordSRV.logger().error(label + " has invalid tie-breaker, "
                                                  + "should be one of " + Arrays.toString(SyncSide.values()));
            }
        }
        if (invalidTieBreaker || (invalidDirection = (direction == null))) {
            if (invalidDirection) {
                discordSRV.logger().error(label + " has invalid direction: " + direction
                                                  + ", should be one of " + Arrays.toString(SyncDirection.values()));
            }
            return false;
        }

        if (direction != SyncDirection.BIDIRECTIONAL) {
            boolean directionIsMinecraft = direction == SyncDirection.MINECRAFT_TO_DISCORD;

            for (SyncSide tieBreaker : tieBreakers.all()) {
                if (tieBreaker == SyncSide.DISABLED) {
                    continue;
                }

                boolean minecraft = tieBreaker == SyncSide.MINECRAFT;
                if (directionIsMinecraft == minecraft) {
                    continue;
                }

                SyncSide opposite = (minecraft ? SyncSide.DISCORD : SyncSide.MINECRAFT);
                discordSRV.logger().warning(
                        label + " with direction " + direction + " has an invalid tie-breaker " + tieBreaker + " (should be " + opposite + ")"
                );
            }
        }

        return true;
    }

}
