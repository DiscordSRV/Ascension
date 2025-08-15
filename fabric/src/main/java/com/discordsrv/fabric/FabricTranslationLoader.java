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

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.component.translation.Translation;
import com.discordsrv.common.core.component.translation.TranslationLoader;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class FabricTranslationLoader extends TranslationLoader {

    public FabricTranslationLoader(DiscordSRV discordSRV) {
        super(discordSRV);
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
}
