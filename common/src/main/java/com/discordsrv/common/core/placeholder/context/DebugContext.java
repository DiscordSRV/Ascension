/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.core.placeholder.context;

import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;

import java.time.Duration;

@PlaceholderPrefix("debug_")
public class DebugContext {

    private final DiscordSRV discordSRV;

    public DebugContext(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Placeholder("task_completed")
    public Task<String> taskCompleted() {
        return Task.completed("Success");
    }

    @Placeholder("task_failed")
    public Task<String> taskFailed() {
        return Task.failed(new RuntimeException("Failed"));
    }

    @Placeholder("task_delayed")
    public Task<String> taskDelayed() {
        return discordSRV.scheduler().supplyLater(() -> "Success", Duration.ofSeconds(2));
    }

    @Placeholder("exception")
    public String exception() {
        throw new RuntimeException("Failed");
    }

    @Placeholder("success")
    public String success() {
        return "Success";
    }
}
