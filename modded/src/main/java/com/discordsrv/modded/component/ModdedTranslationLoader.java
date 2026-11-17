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

package com.discordsrv.modded.component;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.component.translation.TranslationLoader;
import net.kyori.adventure.translation.TranslationStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.stream.Stream;

public class ModdedTranslationLoader extends TranslationLoader {

    public ModdedTranslationLoader(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    protected void loadTranslations(TranslationStore<MessageFormat> translationStore) throws IOException {
        super.loadTranslations(translationStore);
        Path modsFolder = discordSRV.dataDirectory().resolve("../../mods");
        if (Files.exists(modsFolder)) {
            loadFromMods(modsFolder, translationStore);
        }
    }

    protected void loadFromMods(Path folder, TranslationStore<MessageFormat> translationStore) throws IOException {
        try (Stream<Path> paths = Files.list(folder)) {
            paths.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                .forEach(path -> {
                    try {
                        loadFromArchive(path, translationStore);
                    } catch (Throwable t) {
                        logger.warning("Failed to read language files from " + path.getFileName(), t);
                    }
                });
        }
    }
}
