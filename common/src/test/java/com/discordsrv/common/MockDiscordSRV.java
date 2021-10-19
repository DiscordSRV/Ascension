package com.discordsrv.common;

import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.manager.ConnectionConfigManager;
import com.discordsrv.common.config.manager.MainConfigManager;
import com.discordsrv.common.console.Console;
import com.discordsrv.common.logging.logger.Logger;
import com.discordsrv.common.logging.logger.impl.JavaLoggerImpl;
import com.discordsrv.common.player.provider.AbstractPlayerProvider;
import com.discordsrv.common.scheduler.Scheduler;
import com.discordsrv.common.scheduler.StandardScheduler;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class MockDiscordSRV extends AbstractDiscordSRV<MainConfig, ConnectionConfig> {

    public static final MockDiscordSRV INSTANCE = new MockDiscordSRV();

    private final Scheduler scheduler = new StandardScheduler(this);
    private final Logger logger = JavaLoggerImpl.getRoot();

    public MockDiscordSRV() {
        load();
    }

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public Path dataDirectory() {
        return null;
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
