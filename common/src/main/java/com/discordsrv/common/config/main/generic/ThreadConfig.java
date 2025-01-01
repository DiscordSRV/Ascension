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

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Objects;

@ConfigSerializable
public class ThreadConfig {

    public ThreadConfig() {}

    public ThreadConfig(String name) {
        this.threadName = name;
    }

    @Comment("Specify the text or forum channel id and the name of the thread (the thread will be automatically created if it doesn't exist)")
    public Long channelId = 0L;

    public String threadName = "Minecraft Server chat bridge";

    @Comment("Should an existing thread with the same name be unarchived instead of creating a new thread every time")
    public boolean unarchiveExisting = true;

    @Comment("Does not effect forums")
    public boolean privateThread = false;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThreadConfig that = (ThreadConfig) o;
        return privateThread == that.privateThread
                && Objects.equals(channelId, that.channelId)
                && Objects.equals(threadName, that.threadName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelId, threadName, privateThread);
    }
}
