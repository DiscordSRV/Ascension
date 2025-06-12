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

import com.discordsrv.common.abstraction.sync.enums.SyncDirection;
import com.discordsrv.common.abstraction.sync.enums.SyncSide;
import com.discordsrv.common.config.configurate.annotation.Constants;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Arrays;
import java.util.List;

@ConfigSerializable
public abstract class SyncConfig {

    @Comment("The direction to synchronize in.\n"
            + "Valid options: %1, %2, %3")
    @Constants.Comment({"bidirectional", "minecraft_to_discord", "discord_to_minecraft"})
    public SyncDirection direction = SyncDirection.BIDIRECTIONAL;

    @Comment("Timed resynchronization")
    public TimerConfig timer = new TimerConfig();

    @ConfigSerializable
    public static class TimerConfig {

        @Comment("The direction which takes priority for determining for synchronization\n"
                + "Valid options: %1, %2, %3")
        @Constants.Comment({"minecraft", "discord", "disabled"})
        public SyncSide side = SyncSide.MINECRAFT;

        @Comment("The number of minutes between timed synchronization cycles")
        public int cycleTime = 5;

        @Override
        public String toString() {
            return "TimerConfig{" +
                    "side=" + side +
                    ", cycleTime=" + cycleTime +
                    '}';
        }
    }

    @Comment("Decides which side takes priority when synchronizing and there are differences. Also allows disabling synchronization on these events\n"
            + "Valid options: %1, %2, %3")
    @Constants.Comment({"minecraft", "discord", "disabled"})
    public TieBreakers tieBreakers = new TieBreakers();

    public static class TieBreakers {

        public SyncSide join = SyncSide.MINECRAFT;
        public SyncSide link = SyncSide.MINECRAFT;
        public SyncSide resyncCommand = SyncSide.MINECRAFT;

        public List<SyncSide> all() {
            return Arrays.asList(join, link, resyncCommand);
        }

        @Override
        public String toString() {
            return "TieBreakers{" +
                    "join=" + join +
                    ", link=" + link +
                    ", resyncCommand=" + resyncCommand +
                    '}';
        }
    }
}
