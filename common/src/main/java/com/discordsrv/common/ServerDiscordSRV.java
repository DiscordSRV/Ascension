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

package com.discordsrv.common;

import com.discordsrv.common.bootstrap.IBootstrap;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.messageforwarding.game.AwardMessageModule;
import com.discordsrv.common.messageforwarding.game.DeathMessageModule;
import com.discordsrv.common.player.provider.ServerPlayerProvider;
import com.discordsrv.common.scheduler.ServerScheduler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.concurrent.CompletableFuture;

public abstract class ServerDiscordSRV<
        B extends IBootstrap,
        C extends MainConfig,
        CC extends ConnectionConfig,
        MC extends MessagesConfig
> extends AbstractDiscordSRV<B, C, CC, MC> {

    private boolean serverStarted = false;

    public ServerDiscordSRV(B bootstrap) {
        super(bootstrap);
    }

    @Override
    public abstract ServerScheduler scheduler();

    @Override
    public abstract @NotNull ServerPlayerProvider<?, ?> playerProvider();

    @Override
    protected void enable() throws Throwable {
        super.enable();

        registerModule(AwardMessageModule::new);
        registerModule(DeathMessageModule::new);
    }

    public final CompletableFuture<Void> invokeServerStarted() {
        return scheduler().supply(() -> {
            if (status().isShutdown()) {
                // Already shutdown/shutting down, don't bother
                return null;
            }
            try {
                this.serverStarted();
            } catch (Throwable t) {
                if (status().isShutdown() && t instanceof NoClassDefFoundError) {
                    // Already shutdown, ignore errors for classes that already got unloaded
                    return null;
                }
                setStatus(Status.FAILED_TO_START);
                disable();
                logger().error("Failed to start", t);
            }
            return null;
        });
    }

    @OverridingMethodsMustInvokeSuper
    protected void serverStarted() {
        serverStarted = true;
        moduleManager().enableModules();
        startedMessage();
    }

    public boolean isServerStarted() {
        return serverStarted;
    }
}
