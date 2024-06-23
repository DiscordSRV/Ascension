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

package com.discordsrv.common.component.translation;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Our own simple class that stores abstract {@link Translation}s instead of MessageFormats,
 * to be more flexible, and support Minecraft's own language files.
 */
public class TranslationRegistry {

    private static final Locale DEFAULT_LOCALE = Locale.US;

    private final Map<Locale, Map<String, Translation>> translations = new LinkedHashMap<>();

    public TranslationRegistry() {}

    public void register(Locale locale, Map<String, Translation> translations) {
        synchronized (this.translations) {
            this.translations.computeIfAbsent(locale, key -> new LinkedHashMap<>()).putAll(translations);
        }
    }

    @Nullable
    public Translation lookup(Locale locale, String key) {
        synchronized (translations) {
            return translations.getOrDefault(
                    locale, // Try the suggested locale first
                    translations.getOrDefault(
                            DEFAULT_LOCALE, // Then try the default locale
                            Collections.emptyMap() // Then fail
                    )
            ).get(key);
        }
    }

    public void clear() {
        translations.clear();
    }
}
