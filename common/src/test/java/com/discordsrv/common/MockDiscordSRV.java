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

package com.discordsrv.common;

import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.manager.ConnectionConfigManager;
import com.discordsrv.common.config.manager.MainConfigManager;
import com.discordsrv.common.console.Console;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.backend.impl.JavaLoggerImpl;
import com.discordsrv.common.player.provider.AbstractPlayerProvider;
import com.discordsrv.common.scheduler.Scheduler;
import com.discordsrv.common.scheduler.StandardScheduler;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;

public class MockDiscordSRV extends AbstractDiscordSRV<MainConfig, ConnectionConfig> {

    public static final MockDiscordSRV INSTANCE = new MockDiscordSRV();

    private final Scheduler scheduler = new StandardScheduler(this);
    private final Logger logger = JavaLoggerImpl.getRoot();

    public MockDiscordSRV() {
        load();
    }

    @Override
    public Logger platformLogger() {
        return logger;
    }

    @Override
    public Path dataDirectory() {
        return Paths.get("");
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public Console console() {
        return null;
    }

    @Override
    public String version() {
        return null;
    }

    @Override
    public @NotNull AbstractPlayerProvider<?> playerProvider() {
        return null;
    }

    @Override
    public ConnectionConfigManager<ConnectionConfig> connectionConfigManager() {
        return null;
    }

    @Override
    public MainConfigManager<MainConfig> configManager() {
        return null;
    }
}
