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

package com.discordsrv.common.core.component.translation;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.component.ComponentFactory;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.util.ComponentUtil;
import com.fasterxml.jackson.databind.JsonNode;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.TranslationStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class TranslationLoader {

    protected final DiscordSRV discordSRV;
    protected final NamedLogger logger;
    private TranslationStore<MessageFormat> currentTranslationStore;

    public TranslationLoader(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "TRANSLATION_LOADER");
    }

    public void reload() {
        try {
            TranslationStore<MessageFormat> translationStore = TranslationStore.messageFormat(Key.key("discordsrv", "user-defined"));
            translationStore.defaultLocale(ComponentFactory.TRANSLATION_LOCALE);

            Path languages = discordSRV.dataDirectory().resolve("game_languages");
            if (Files.exists(languages)) {
                loadFromFiles(languages, translationStore);
            }

            // Remove our previous store
            if (currentTranslationStore != null) {
                discordSRV.componentFactory().translators().remove(currentTranslationStore);
            }

            currentTranslationStore = translationStore;
            discordSRV.componentFactory().translators().add(0, translationStore);
        } catch (Throwable t) {
            logger.error("Failed to reload languages", t);
        }
    }

    protected void loadFromFiles(Path folder, TranslationStore<MessageFormat> translationStore) throws IOException {
        try (Stream<Path> paths = Files.list(folder)) {
            paths.forEach(path -> {
                String fileName = path.getFileName().toString();
                int lastDot = fileName.lastIndexOf(".");
                String extension = lastDot == -1 ? null : fileName.substring(lastDot + 1);
                if (extension == null || !(extension.equals("json") || extension.equals("lang"))) {
                    discordSRV.logger().warning("Unexpected file in game_languages: " + fileName);
                    return;
                }

                try {
                    // Grab locale from file name to pass to MessageFormat
                    String language = fileName.substring(0, lastDot);
                    Locale locale = ComponentUtil.extractLocale(language);

                    URL url = path.toUri().toURL();

                    Map<String, String> translations;
                    if (extension.equalsIgnoreCase("json")) {
                        translations = getFromJson(url);
                    } else {
                        translations = getFromProperties(url);
                    }
                    if (translations != null && !translations.isEmpty()) {
                        translations.forEach((key, value) -> translationStore.register(key, locale, messageFormat(value, locale)));
                        logger.debug("Loaded " + translations.size() + " translations from " + fileName + " (in " + locale + ")");
                    }
                } catch (Throwable t) {
                    logger.warning("Failed to read language file " + fileName, t);
                }
            });
        }
    }

    protected Map<String, String> getFromProperties(URL url) throws IOException {
        Map<String, String> translations = new HashMap<>();

        Properties properties = new Properties();
        try (InputStream inputStream = url.openStream()) {
            properties.load(inputStream);
        }

        properties.forEach((k, v) -> translations.put((String) k, (String) v));
        return translations;
    }

    protected Map<String, String> getFromJson(URL url) throws IOException {
        Map<String, String> translations = new HashMap<>();

        JsonNode node = discordSRV.json().readTree(url);
        node.fields().forEachRemaining(entry -> translations.put(entry.getKey(), entry.getValue().textValue()));

        return translations;
    }

    private static final Pattern PACK_PLACEHOLDER_PATTERN = Pattern.compile("%(?:s|([0-9]+)\\$s)");

    protected MessageFormat messageFormat(String value, Locale locale) {
        // Adventure wants a MessageFormat while Minecraft language files are not that
        // https://minecraft.wiki/w/Resource_pack#Language

        Matcher matcher = PACK_PLACEHOLDER_PATTERN.matcher(value);
        StringBuffer replacement = new StringBuffer(value.length());
        int lastEnd = -1;
        int index = 0;
        while (matcher.find()) {
            lastEnd = matcher.end();
            index++;
            int number;
            try {
                number = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                number = index;
            }

            // Change to 0 indexed
            matcher.appendReplacement(replacement, Matcher.quoteReplacement("{" + (number - 1) + "}"));
        }
        if (lastEnd != -1) {
            // If we found any placeholders, use the replaced output
            replacement.append(value, lastEnd, value.length());
            value = replacement.toString();
        }

        // Unescape %
        value = value.replace("%%", "%");

        return new MessageFormat(value, locale);
    }
}
