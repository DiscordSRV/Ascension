package com.discordsrv.common.config.configurate.manager;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.exception.ConfigException;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public abstract class MessagesConfigManager<C extends MessagesConfig> {

    private final Map<Locale, MessagesConfigSingleManager<C>> configs = new LinkedHashMap<>();
    private final DiscordSRV discordSRV;
    private final Logger logger;
    private boolean multi;

    public MessagesConfigManager(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "MESSAGES_CONFIG");
    }

    public abstract C createConfiguration();

    public MessagesConfigSingleManager<C> getManager(Locale locale) {
        synchronized (configs) {
            return configs.get(locale);
        }
    }

    public Path directory() {
        return discordSRV.dataDirectory().resolve("messages");
    }

    public void load() throws ConfigException {
        synchronized (configs) {
            configs.clear();

            MainConfig config = discordSRV.config();
            if (config == null) {
                throw new ConfigException("MainConfig not available");
            }

            boolean multiple = config.messages.multiple;
            Locale defaultLocale = discordSRV.defaultLocale();

            if (multiple) {
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
                configs.put(Locale.US, new MessagesConfigSingleManager<>(discordSRV, this, Locale.US, false));
            }

            for (Map.Entry<Locale, MessagesConfigSingleManager<C>> entry : configs.entrySet()) {
                entry.getValue().load();
            }
        }
    }
}
