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
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

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

            loadTranslations(translationStore);

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

    protected void loadTranslations(TranslationStore<MessageFormat> translationStore) throws IOException {
        Path languages = discordSRV.dataDirectory().resolve("game_languages");
        if (Files.exists(languages)) {
            loadFromFiles(languages, translationStore);
        }
    }

    protected void loadFromFiles(Path folder, TranslationStore<MessageFormat> translationStore) throws IOException {
        try (Stream<Path> paths = Files.list(folder)) {
            paths.forEach(path -> {
                String fileName = path.getFileName().toString();
                int lastDot = fileName.lastIndexOf(".");
                String extension = lastDot == -1 ? null : fileName.substring(lastDot + 1);

                try {
                    if (extension != null && (extension.equalsIgnoreCase("jar") || extension.equalsIgnoreCase("zip"))) {
                        loadFromArchive(path, translationStore);
                        return;
                    }
                    if (extension == null || (!extension.equalsIgnoreCase("json") && !extension.equalsIgnoreCase("lang"))) {
                        discordSRV.logger().debug("Unexpected file in game_languages: " + fileName);
                        return;
                    }

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
                        registerTranslations(translations, locale, fileName, translationStore);
                        logger.debug("Loaded " + translations.size() + " translations from " + fileName + " (in " + locale + ")");
                    }
                } catch (Throwable t) {
                    logger.warning("Failed to read language file " + fileName, t);
                }
            });
        }
    }

    protected void loadFromArchive(Path path, TranslationStore<MessageFormat> translationStore) throws IOException {
        try (JarFile jarFile = new JarFile(path.toFile())) {
            jarFile.stream()
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> entry.getName().startsWith("assets/"))
                .filter(entry -> entry.getName().contains("/lang/"))
                .filter(entry -> entry.getName().toLowerCase(Locale.ROOT).endsWith(".json"))
                .forEach(entry -> loadJarTranslation(jarFile, entry, translationStore));
        }
    }

    private void loadJarTranslation(JarFile jarFile, JarEntry entry, TranslationStore<MessageFormat> translationStore) {
        Path archivePath = Path.of(jarFile.getName());
        String entryName = entry.getName();
        int langPath = entryName.indexOf("/lang/");
        String fileName = entryName.substring(entryName.lastIndexOf('/') + 1);
        int lastDot = fileName.lastIndexOf('.');

        // If the entry is not in the lang folder or doesn't have a file extension, skip it
        if (langPath < "assets/".length() || lastDot <= 0) {
            return;
        }

        try {
            String language = fileName.substring(0, lastDot);
            Locale locale = ComponentUtil.extractLocale(language);
            Map<String, String> translations;
            try (InputStream inputStream = jarFile.getInputStream(entry)) {
                translations = getFromJson(inputStream);
            }
            if (translations != null && !translations.isEmpty()) {
                registerTranslations(translations, locale, entryName, translationStore);
                logger.debug("Loaded " + translations.size() + " translations from " + entryName + " in " + archivePath.getFileName() + " (in " + locale + ")");
            }
        } catch (Throwable t) {
            logger.warning("Failed to read language file " + entryName + " from " + archivePath.getFileName(), t);
        }
    }

    private void registerTranslations(Map<String, String> translations, Locale locale, String source, TranslationStore<MessageFormat> translationStore) {
        translations.forEach((key, value) -> {
            if (translationStore.contains(key, locale)) {
                return;
            }
            try {
                translationStore.register(key, locale, messageFormat(value, locale));
            } catch (IllegalArgumentException exception) {
                logger.warning("Failed to register translation " + key + " from " + source + " (in " + locale + ")", exception);
            }
        });
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
        try (InputStream inputStream = url.openStream()) {
            return getFromJson(inputStream);
        }
    }

    protected Map<String, String> getFromJson(InputStream inputStream) throws IOException {
        Map<String, String> translations = new HashMap<>();

        JsonNode node = discordSRV.json().readTree(inputStream);
        node.fields().forEachRemaining(entry -> translations.put(entry.getKey(), entry.getValue().textValue()));

        return translations;
    }

    private static final Pattern PACK_PLACEHOLDER_PATTERN = Pattern.compile("%(?:s|([0-9]+)\\$s)");

    protected MessageFormat messageFormat(String value, Locale locale) {
        // Adventure wants a MessageFormat while Minecraft language files are not that
        // https://minecraft.wiki/w/Resource_pack#Language

        if (value == null) return new MessageFormat("", locale);
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

        // if the value contains { xor }, for example a minecraft client keypad { or }. Which means they are unmatched and Adventure will freak out.
        if (value.contains("{") ^ value.contains("}")) {
            return new MessageFormat(value.replace("{", "").replace("}", ""), locale);
        }
        return new MessageFormat(value, locale);
    }
}
