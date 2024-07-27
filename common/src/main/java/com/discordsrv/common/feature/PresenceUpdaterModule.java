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

package com.discordsrv.common.feature;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.api.eventbus.EventPriority;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.PresenceUpdaterConfig;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;
import net.dv8tion.jda.api.JDA;
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
        return true;
    }

    public void serverStarted() {
        serverState.set(ServerState.STARTED);
        setPresenceOrSchedule();
    }

    @Subscribe(priority = EventPriority.EARLIEST)
    public void onDiscordSRVShuttingDown(DiscordSRVShuttingDownEvent event) {
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
    public void reload(Consumer<DiscordSRVApi.ReloadResult> resultConsumer) {
        if (discordSRV.jda() == null) {
            return;
        }

        setPresenceOrSchedule();

        // Log problems with presences
        for (PresenceUpdaterConfig.Presence presence : discordSRV.config().presenceUpdater.presences) {
            presence.activity(logger());
        }
    }

    private void setPresence(PresenceUpdaterConfig.Presence config) {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            // Guess not
            return;
        }
        jda.getPresence().setPresence(config.status, config.activity(null));
    }

    private void setPresenceOrSchedule() {
        boolean alreadyScheduled = future != null;
        if (future != null) {
            future.cancel(true);
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

        Duration duration = Duration.ofSeconds(config.updaterRateInSeconds);
        future = discordSRV.scheduler().runAtFixedRate(() -> {
            int index = currentIndex.getAndUpdate(value -> {
                if (count <= value) {
                    return 0;
                }
                return value + 1;
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
