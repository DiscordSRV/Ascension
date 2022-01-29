/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.server;

import com.discordsrv.common.AbstractDiscordSRV;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.server.modules.DeathMessageModule;
import com.discordsrv.common.server.player.ServerPlayerProvider;
import com.discordsrv.common.server.scheduler.ServerScheduler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.concurrent.CompletableFuture;

public abstract class ServerDiscordSRV<C extends MainConfig, CC extends ConnectionConfig> extends AbstractDiscordSRV<C, CC> {

    @Override
    public abstract ServerScheduler scheduler();

    @Override
    public abstract @NotNull ServerPlayerProvider<?, ?> playerProvider();

    @Override
    protected void enable() throws Throwable {
        super.enable();

        registerModule(DeathMessageModule::new);
    }

    public final CompletableFuture<Void> invokeServerStarted() {
        return invokeLifecycle(this::serverStarted, "Failed to enable", true);
    }

    @OverridingMethodsMustInvokeSuper
    protected void serverStarted() {

    }
}
