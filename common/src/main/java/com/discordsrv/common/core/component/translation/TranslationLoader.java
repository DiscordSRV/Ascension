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

package com.discordsrv.common.core.component.translation;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.logging.NamedLogger;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class TranslationLoader {

    private final DiscordSRV discordSRV;
    private final NamedLogger logger;

    public TranslationLoader(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "TRANSLATION_LOADER");
    }

    public void reload() {
        try {
            TranslationRegistry registry = discordSRV.componentFactory().translationRegistry();
            registry.clear();

            AtomicBoolean any = new AtomicBoolean(false);

            Path languages = discordSRV.dataDirectory().resolve("game_languages");
            if (Files.exists(languages)) {
                loadFromFiles(languages, registry, any);
            }
            loadMCTranslations(any);

            if (!any.get()) {
                logger.warning("No Minecraft translations were found, some components may not render correctly");
            }
        } catch (Throwable t) {
            logger.error("Failed to reload languages", t);
        }
    }

    private void loadFromFiles(Path folder, TranslationRegistry registry, AtomicBoolean any) throws IOException {
        try (Stream<Path> paths = Files.list(folder)) {
            paths.forEach(path -> {
                String fileName = path.getFileName().toString();
                int lastDot = fileName.lastIndexOf("\\.");
                String extension = lastDot == -1 ? null : fileName.substring(lastDot + 1);
                if (extension == null || !(extension.equals("json") || extension.equals("lang"))) {
                    discordSRV.logger().warning("Unexpected file in game_languages: " + fileName);
                    return;
                }

                try {
                    String language = fileName.substring(0, lastDot);
                    Locale locale = Locale.forLanguageTag(language);
                    URL url = path.toUri().toURL();

                    Map<String, Translation> translations = null;
                    if (path.endsWith(".json")) {
                        translations = getFromJson(url);
                    } else if (path.endsWith(".lang")) {
                        translations = getFromProperties(url);
                    }
                    if (translations != null && !translations.isEmpty()) {
                        registry.register(locale, translations);
                        logger.debug("Loaded " + translations.size() + " translations for " + locale);
                        any.set(true);
                    }
                } catch (Throwable t) {
                    logger.warning("Failed to read language file " + fileName, t);
                }
            });
        }
    }

    private Map<String, Translation> getFromProperties(URL url) throws IOException {
        Map<String, Translation> translations = new HashMap<>();

        Properties properties = new Properties();
        try (InputStream inputStream = url.openStream()) {
            properties.load(inputStream);
        }

        properties.forEach((k, v) -> translations.put((String) k, Translation.stringFormat((String) v)));

        return translations;
    }

    private Map<String, Translation> getFromJson(URL url) throws IOException {
        Map<String, Translation> translations = new HashMap<>();

        JsonNode node = discordSRV.json().readTree(url);
        node.fields().forEachRemaining(entry -> translations.put(
                entry.getKey(),
                Translation.stringFormat(entry.getValue().textValue()))
        );

        return translations;
    }

    private URL findResource(String name) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = null;
        while (classLoader != null && url == null) {
            url = classLoader.getResource(name);
            classLoader = classLoader.getParent();
        }
        return url;
    }

    private void loadMCTranslations(AtomicBoolean any) {
        Map<String, Translation> translations = new HashMap<>();
        try {
            URL enUS = findResource("assets/minecraft/lang/en_US.lang");
            if (enUS == null) {
                enUS = findResource("assets/minecraft/lang/en_us.lang");
            }
            if (enUS != null) {
                translations = getFromProperties(enUS);
            }
        } catch (Throwable t) {
            logger.debug("Failed to load locale", t);
        }
        try {
            URL enUS = findResource("assets/minecraft/lang/en_us.json");
            if (enUS != null) {
                translations = getFromJson(enUS);
            }
        } catch (Throwable t) {
            logger.debug("Failed to load locale", t);
        }

        if (!translations.isEmpty()) {
            discordSRV.componentFactory().translationRegistry().register(Locale.US, translations);
            logger.debug("Found " + translations.size() + " Minecraft translations for en_us");
            any.set(true);
        }
    }
}
