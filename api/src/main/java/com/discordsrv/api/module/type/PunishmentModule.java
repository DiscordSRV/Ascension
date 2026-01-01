/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.discordsrv.api.module.type;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.module.Module;
import com.discordsrv.api.punishment.Punishment;
import com.discordsrv.api.task.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public interface PunishmentModule extends Module {

    interface Bans extends PunishmentModule {
        Task<@Nullable Punishment> getBan(@NotNull UUID playerUUID);
        Task<Void> addBan(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable MinecraftComponent reason, @NotNull MinecraftComponent punisher);
        Task<Void> removeBan(@NotNull UUID playerUUID);
    }

    interface Mutes extends PunishmentModule {
        Task<@Nullable Punishment> getMute(@NotNull UUID playerUUID);
        Task<Void> addMute(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable MinecraftComponent reason, @NotNull MinecraftComponent punisher);
        Task<Void> removeMute(@NotNull UUID playerUUID);
    }
}
