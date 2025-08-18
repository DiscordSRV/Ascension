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

package com.discordsrv.fabric;

import com.discordsrv.common.core.component.translation.Translation;
import com.discordsrv.common.core.component.translation.TranslationLoader;
import com.discordsrv.common.core.component.translation.TranslationRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

public class FabricTranslationLoader extends TranslationLoader {

    private final FabricDiscordSRV discordSRV;

    public FabricTranslationLoader(FabricDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
    }

    @Override
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
            reload(discordSRV.getServer().getResourceManager(), any);

            if (!any.get()) {
                logger.warning("No Minecraft translations were found, some components may not render correctly");
            }
        } catch (Throwable t) {
            logger.error("Failed to reload languages", t);
        }
    }

    @Override
    protected void loadMCTranslations(AtomicBoolean any) {
        Map<String, Translation> translations = new HashMap<>();

        getClass().getClassLoader().resources("assets/minecraft/lang/en_us.json").forEach(url -> {
            try {
                translations.putAll(getFromJson(url));
            } catch (Throwable t) {
                logger.debug("Failed to load translations from " + url, t);
            }
        });

        if (!translations.isEmpty()) {
            discordSRV.componentFactory().translationRegistry().register(Locale.US, translations);
            logger.debug("Found " + translations.size() + " Minecraft translations for en_us");
            any.set(true);
        }
    }

    protected void readFromZipfile(ZipFile zip, AtomicBoolean any) {
        zip.stream().filter(entry -> entry.getName().contains("lang") && entry.getName().contains(".json")).forEach(entry -> {
            Pattern pattern = Pattern.compile("assets/[^/]+/lang/(.+)\\.json$");
            Matcher matcher = pattern.matcher(entry.getName());
            if (!matcher.matches()) return;

            try {
                String langId = matcher.group(1);

                Locale locale = Locale.forLanguageTag(langId.replace('_', '-'));
                logger.debug("Found language " + locale + " in jar: " + zip.getName());

                Map<String, Translation> translations = getFromJson(zip.getInputStream(entry));
                discordSRV.componentFactory().translationRegistry().register(locale, translations);

                any.set(true);
                logger.debug("Found " + translations.size() + " translations for language " + locale + " in jar: " + zip.getName());
            } catch (IOException e) {
                logger.error("Failed to read language file from jar: " + zip.getName(), e);
            }
        });
    }

    public CompletableFuture<Void> reload(ResourceManager manager) {
        return reload(manager, new AtomicBoolean(false));
    }

    public CompletableFuture<Void> reload(ResourceManager manager, AtomicBoolean any) {
        return CompletableFuture.runAsync(() -> {
            try {
                Set<URL> urls = new HashSet<>();
                for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
                    String modId = mod.getMetadata().getId();

                    logger.debug("Checking mod " + modId + " for language resources");
                    Enumeration<URL> modUrls = getClass().getClassLoader().getResources("assets/" + modId + "/lang/");
                    while (modUrls.hasMoreElements()) {
                        URL url = modUrls.nextElement();
                        if (url.getProtocol().equals("jar") && url.getPath().contains("!")) {
                            urls.add(url);
                            logger.debug("Found language resource URL: " + url);
                        }
                    }
                }

                Enumeration<URL> urlEnumeration = getClass().getClassLoader().getResources("lang/");
                while (urlEnumeration.hasMoreElements()) {
                    URL url = urlEnumeration.nextElement();
                    if (url.getProtocol().equals("jar") && url.getPath().contains("!")) {
                        urls.add(url);
                        logger.debug("Found language resource URL: " + url);
                    }
                }

                for (URL url : urls) {
                    String spec = url.getFile();
                    int separator = spec.indexOf("!/");
                    if (separator == -1) continue;

                    String jarUriString = spec.substring(0, separator);
                    URI jarUri = URI.create(jarUriString);

                    if (url.getPath().contains("farmer")){
                        logger.debug(url.getPath() + " found farmer");
                    }

                    File jarFile = Paths.get(jarUri).toFile();
                    try (ZipFile zip = new ZipFile(jarFile)) {
                        readFromZipfile(zip, any);
                    } catch (IOException e) {
                        logger.error("Failed to read language files from jar: " + jarUriString, e);
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to enumerate language resources", e);
            }

            for (Identifier id : manager.findAllResources("lang", _id -> _id.getPath().endsWith(".json")).keySet()) {
                logger.debug("Loading language file from resource manager: " + id);
                Resource resource = manager.getResource(id).orElse(null);

                if (resource == null) {
                    logger.debug("Resource is null: " + id);
                    continue;
                }

                String langPath = id.getPath();
                String langId = langPath.replaceAll("[.+]?lang/", "").replaceAll("\\.json$", "");
                if (langId.equals("deprecated")) return;
                Locale locale = Locale.forLanguageTag(langId.replace('_', '-'));

                int count = 0;
                try {
                    Map<String, Translation> translations = getFromJson(resource.getInputStream());
                    discordSRV.componentFactory().translationRegistry().register(locale, translations);

                    any.set(true);
                    count = translations.size();
                } catch (IOException e) {
                    logger.debug("Failed to read language file from resource manager: " + langPath, e);
                }

                logger.debug("Loaded " + count + " translations for language " + locale + " from resource manager file: " + id);
            }
        });
    }
}
