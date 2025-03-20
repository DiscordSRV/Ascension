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

package com.discordsrv.common.feature;

import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.PresenceUpdaterConfig;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.StatusChangeEvent;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class PresenceUpdaterModule extends AbstractModule<DiscordSRV> {

    private Future<?> future;
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private final AtomicReference<ServerState> serverState = new AtomicReference<>(ServerState.PRE_START);

    public PresenceUpdaterModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "PRESENCE_UPDATER"));
    }

    @Override
    public boolean canEnableBeforeReady() {
        return discordSRV.config() != null;
    }

    public void serverStarted() {
        logger().debug("Server started");
        serverState.set(ServerState.STARTED);
        setPresenceOrSchedule();
    }

    @Subscribe(priority = EventPriorities.EARLIEST)
    public void onDiscordSRVShuttingDown(DiscordSRVShuttingDownEvent event) {
        logger().debug("Plugin shutting down");
        serverState.set(ServerState.STOPPING);
        setPresenceOrSchedule();
    }

    @Subscribe
    public void onStatusChange(StatusChangeEvent event) {
        if (event.getNewStatus() == JDA.Status.IDENTIFYING_SESSION) {
            setPresenceOrSchedule();
        }
    }

    @Override
    public void reload(Consumer<ReloadResult> resultConsumer) {
        if (discordSRV.jda() == null) {
            return;
        }

        setPresenceOrSchedule();

        // Log problems with presences
        for (PresenceUpdaterConfig.Presence presence : discordSRV.config().presenceUpdater.presences) {
            presence.activity(logger(), discordSRV);
        }
    }

    private void setPresence(PresenceUpdaterConfig.Presence config) {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            // Guess not
            return;
        }

        Activity newActivity = config.activity(null, discordSRV);
        logger().debug("Changing activity to " + newActivity);
        jda.getPresence().setPresence(config.status, newActivity);
    }

    private void setPresenceOrSchedule() {
        boolean alreadyScheduled = future != null;
        if (future != null) {
            future.cancel(true);
        }

        if (discordSRV.isServerStarted() && serverState.get() == ServerState.PRE_START) {
            logger().debug("Server is started, changing to STARTED state");
            serverState.set(ServerState.STARTED);
        }

        PresenceUpdaterConfig config = discordSRV.config().presenceUpdater;
        if (config instanceof PresenceUpdaterConfig.Server) {
            PresenceUpdaterConfig.Server serverConfig = (PresenceUpdaterConfig.Server) config;
            switch (serverState.get()) {
                case PRE_START:
                    if (serverConfig.useStartingPresence) {
                        setPresence(serverConfig.startingPresence);
                        return;
                    }
                case STOPPING:
                    if (serverConfig.useStoppingPresence) {
                        setPresence(serverConfig.stoppingPresence);
                        return;
                    }
            }
        }

        List<PresenceUpdaterConfig.Presence> presences = config.presences;
        int count = config.presences.size();
        if (count == 1) {
            setPresence(presences.get(0));
            return;
        }

        Duration duration = Duration.ofSeconds(Math.max(config.updaterRateInSeconds, 30));
        future = discordSRV.scheduler().runAtFixedRate(() -> {
            int index = currentIndex.getAndUpdate(value -> {
                if (count > ++value) {
                    return value;
                }
                return 0;
            });
            setPresence(presences.get(index));
        }, alreadyScheduled ? duration : Duration.ZERO, duration);
    }

    private enum ServerState {
        PRE_START,
        STARTED,
        STOPPING
    }
}
