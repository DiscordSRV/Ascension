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

package com.discordsrv.common;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.common.abstraction.bootstrap.IBootstrap;
import com.discordsrv.common.abstraction.bootstrap.LifecycleManager;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.abstraction.player.provider.AbstractPlayerProvider;
import com.discordsrv.common.abstraction.plugin.PluginManager;
import com.discordsrv.common.command.game.abstraction.executor.CommandExecutorProvider;
import com.discordsrv.common.command.game.abstraction.handler.ICommandHandler;
import com.discordsrv.common.config.configurate.manager.ConnectionConfigManager;
import com.discordsrv.common.config.configurate.manager.MainConfigManager;
import com.discordsrv.common.config.configurate.manager.MessagesConfigManager;
import com.discordsrv.common.config.configurate.manager.abstraction.ServerConfigManager;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.ChannelConfig;
import com.discordsrv.common.config.main.generic.DestinationConfig;
import com.discordsrv.common.config.main.generic.ThreadConfig;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.core.debug.data.OnlineMode;
import com.discordsrv.common.core.debug.data.VersionInfo;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.backend.LoggingBackend;
import com.discordsrv.common.core.logging.backend.impl.JavaLoggerImpl;
import com.discordsrv.common.core.scheduler.Scheduler;
import com.discordsrv.common.core.scheduler.StandardScheduler;
import com.discordsrv.common.core.storage.impl.MemoryStorage;
import com.discordsrv.common.feature.console.Console;
import com.discordsrv.common.feature.messageforwarding.game.MinecraftToDiscordChatModule;
import com.discordsrv.common.permission.game.Permission;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.fail;

@SuppressWarnings("ConstantConditions")
public class MockDiscordSRV extends AbstractDiscordSRV<IBootstrap, MainConfig, ConnectionConfig, MessagesConfig> {

    private static MockDiscordSRV INSTANCE = null;
    private static Throwable FAILED_TO_GET_INSTANCE = null;
    public static MockDiscordSRV getInstance() {
        if (FAILED_TO_GET_INSTANCE != null) {
            fail("Failed to make MockDiscordSRV instance", FAILED_TO_GET_INSTANCE);
            assert false;
        }

        if (INSTANCE != null) {
            return INSTANCE;
        }

        try {
            return INSTANCE = new MockDiscordSRV();
        } catch (Throwable t) {
            FAILED_TO_GET_INSTANCE = t;
            return getInstance();
        }
    }

    public boolean configLoaded = false;
    public boolean connectionConfigLoaded = false;
    public boolean messagesConfigLoaded = false;
    public boolean playerProviderSubscribed = false;

    private final Scheduler scheduler = new StandardScheduler(this);
    private Path path;

    public static void main(String[] args) {
        new MockDiscordSRV();
    }

    public MockDiscordSRV() {
        super(new IBootstrap() {
            @Override
            public Logger logger() {
                return JavaLoggerImpl.getRoot();
            }

            @Override
            public ClasspathAppender classpathAppender() {
                return null;
            }

            @Override
            public ClassLoader classLoader() {
                return MockDiscordSRV.class.getClassLoader();
            }

            @Override
            public LifecycleManager lifecycleManager() {
                return null;
            }

            @Override
            public Path dataDirectory() {
                return null;
            }

            @Override
            public String platformVersion() {
                return null;
            }
        });
        load();
        versionInfo = new VersionInfo("JUnit", "JUnit", "JUnit", "JUnit");
    }

    @Override
    public ServerType serverType() {
        return null;
    }

    @Override
    public Path dataDirectory() {
        if (this.path == null) {
            Path path = Paths.get("/tmp/discordsrv-test");
            try {
                path = Files.createTempDirectory("discordsrv-test");
            } catch (IOException ignored) {}
            this.path = path;
        }

        return path;
    }

    @Override
    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public Console console() {
        return new Console() {
            @Override
            public LoggingBackend loggingBackend() {
                return JavaLoggerImpl.getRoot();
            }

            @Override
            public CommandExecutorProvider commandExecutorProvider() {
                return null;
            }

            @Override
            public boolean hasPermission(Permission permission) {
                return false;
            }

            @Override
            public void runCommand(String command) {

            }

            @Override
            public @NotNull Audience audience() {
                return null;
            }
        };
    }

    @Override
    public PluginManager pluginManager() {
        return null;
    }

    @Override
    public OnlineMode onlineMode() {
        return OnlineMode.ONLINE;
    }

    @Override
    public ICommandHandler commandHandler() {
        return null;
    }

    @Override
    public @NotNull AbstractPlayerProvider<?, ?> playerProvider() {
        return new AbstractPlayerProvider<IPlayer, DiscordSRV>(this) {
            @Override
            public void subscribe() {
                playerProviderSubscribed = true;
            }

            @Override
            public void unsubscribe() {
                playerProviderSubscribed = false;
            }
        };
    }

    @Override
    public ConnectionConfigManager<ConnectionConfig> connectionConfigManager() {
        return new ConnectionConfigManager<ConnectionConfig>(this, ConnectionConfig::new) {
            @Override
            public ConnectionConfig createConfiguration() {
                return connectionConfig();
            }

            @Override
            public void reload(boolean forceSave, AtomicBoolean anyMissingOptions, Path backupPath) {
                connectionConfigLoaded = true;
            }
        };
    }

    @Override
    public ConnectionConfig connectionConfig() {
        ConnectionConfig config = new ConnectionConfig();
        config.bot.token = FullBootExtension.BOT_TOKEN;
        config.storage.backend = MemoryStorage.IDENTIFIER;
        config.minecraftAuth.allow = false;
        config.update.firstPartyNotification = false;
        config.update.security.enabled = false;
        config.update.github.enabled = false;
        return config;
    }

    @Override
    public MainConfigManager<MainConfig> configManager() {
        return new ServerConfigManager<MainConfig>(this, () -> new MainConfig() {}) {
            @Override
            public MainConfig createConfiguration() {
                return config();
            }

            @Override
            public void reload(boolean forceSave, AtomicBoolean anyMissingOptions, Path backupPath) {
                configLoaded = true;
            }
        };
    }

    @Override
    protected void enable() throws Throwable {
        super.enable();

        registerModule(MinecraftToDiscordChatModule::new);
    }

    @Override
    public MainConfig config() {
        MainConfig config = new MainConfig() {};

        BaseChannelConfig defaultChannelConfig = config.channels.get(ChannelConfig.DEFAULT_KEY);
        defaultChannelConfig.startMessage.enabled = false;
        defaultChannelConfig.stopMessage.enabled = false;

        try {
            long textChannelId = Long.parseLong(FullBootExtension.TEXT_CHANNEL_ID);
            long newsChannelId = Long.parseLong(FullBootExtension.NEWS_CHANNEL_ID);
            long forumChannelId = Long.parseLong(FullBootExtension.FORUM_CHANNEL_ID);
            long mediaChannelId = Long.parseLong(FullBootExtension.MEDIA_CHANNEL_ID);
            long voiceChannelId = Long.parseLong(FullBootExtension.VOICE_CHANNEL_ID);
            long stageChannelId = Long.parseLong(FullBootExtension.STAGE_CHANNEL_ID);

            ChannelConfig global = (ChannelConfig) config.channels.get(GameChannel.DEFAULT_NAME);
            DestinationConfig destination = global.destination = new DestinationConfig();

            List<Long> channelIds = destination.channelIds;
            channelIds.clear();
            channelIds.add(textChannelId);
            channelIds.add(newsChannelId);
            channelIds.add(voiceChannelId);
            channelIds.add(stageChannelId);

            List<ThreadConfig> threadConfigs = destination.threads;
            threadConfigs.clear();

            threadConfigs.add(new ThreadConfig() {{
                channelId = textChannelId;
            }});
            threadConfigs.add(new ThreadConfig() {{
                channelId = newsChannelId;
            }});
            threadConfigs.add(new ThreadConfig() {{
                channelId = forumChannelId;
            }});
            threadConfigs.add(new ThreadConfig() {{
                channelId = mediaChannelId;
            }});
        } catch (NumberFormatException ignored) {}

        return config;
    }

    @Override
    public MessagesConfigManager<MessagesConfig> messagesConfigManager() {
        return new MessagesConfigManager<MessagesConfig>(this, MessagesConfig::new) {
            @Override
            public MessagesConfig createConfiguration() {
                return null;
            }

            @Override
            public void reload(boolean forceSave, AtomicBoolean anyMissingOptions, Path backupPath) {
                messagesConfigLoaded = true;
            }
        };
    }
}
