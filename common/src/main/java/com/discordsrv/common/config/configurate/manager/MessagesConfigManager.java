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

package com.discordsrv.common.config.configurate.manager;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.exception.ConfigException;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class MessagesConfigManager<C extends MessagesConfig> {

    private final Map<Locale, MessagesConfigSingleManager<C>> configs = new LinkedHashMap<>();
    private final Supplier<C> configSupplier;
    private final DiscordSRV discordSRV;
    private final Logger logger;

    public MessagesConfigManager(DiscordSRV discordSRV, Supplier<C> configSupplier) {
        this.discordSRV = discordSRV;
        this.configSupplier = configSupplier;
        this.logger = new NamedLogger(discordSRV, "MESSAGES_CONFIG");
    }

    public C createConfiguration() {
        return configSupplier.get();
    }

    public Map<Locale, MessagesConfigSingleManager<C>> getAllManagers() {
        return Collections.unmodifiableMap(configs);
    }

    public MessagesConfigSingleManager<C> getManager(Locale locale) {
        synchronized (configs) {
            return configs.get(locale);
        }
    }

    public Path directory() {
        return discordSRV.dataDirectory().resolve("messages");
    }

    public void reload(boolean forceSave, AtomicBoolean anyMissingOptions, @Nullable Path backupPath) throws ConfigException {
        synchronized (configs) {
            configs.clear();

            MainConfig config = discordSRV.config();
            if (config == null) {
                throw new ConfigException("MainConfig not available");
            }

            if (config.messages.multiple) {
                try {
                    Path messagesDirectory = directory();
                    if (!Files.exists(messagesDirectory)) {
                        Files.createDirectory(messagesDirectory);
                    }

                    List<Locale> existing = new ArrayList<>();
                    try (Stream<Path> paths = Files.list(messagesDirectory)) {
                        paths.forEach(path -> {
                            String fileName = path.getFileName().toString();
                            String[] parts = fileName.split("\\.", 2);
                            if (parts.length != 2 || !parts[1].equals("yaml")) {
                                logger.warning("Unexpected messages file: " + fileName + " (invalid language code or not .yaml)");
                                return;
                            }

                            Locale locale = Locale.forLanguageTag(parts[0]);
                            if (locale == null) {
                                logger.warning("Unexpected messages file: " + fileName + " (unknown locale)");
                                return;
                            }

                            configs.put(locale, new MessagesConfigSingleManager<>(discordSRV, this, locale, true));
                            existing.add(locale);
                        });
                    }

                    if (config.messages.loadAllDefaults) {
                        // TODO: load all default default locales missing
                    }
                } catch (Throwable t) {
                    throw new ConfigException("Failed to initialize messages configs", t);
                }
            } else {
                Locale defaultLocale = discordSRV.defaultLocale();
                configs.put(defaultLocale, new MessagesConfigSingleManager<>(discordSRV, this, defaultLocale, false));
            }

            for (Map.Entry<Locale, MessagesConfigSingleManager<C>> entry : configs.entrySet()) {
                entry.getValue().reload(forceSave, anyMissingOptions, backupPath);
            }
        }
    }
}
