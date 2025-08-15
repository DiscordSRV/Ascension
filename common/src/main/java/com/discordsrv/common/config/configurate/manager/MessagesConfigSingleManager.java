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

package com.discordsrv.common.config.configurate.manager;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.configurate.manager.abstraction.TranslatedConfigManager;
import com.discordsrv.common.config.configurate.manager.loader.YamlConfigLoaderProvider;
import com.discordsrv.common.config.messages.MessagesConfig;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Supplier;

public class MessagesConfigSingleManager<C extends MessagesConfig>
        extends TranslatedConfigManager<C, YamlConfigurationLoader>
        implements YamlConfigLoaderProvider {

    private final MessagesConfigManager<C> aggregateManager;
    private final Supplier<C> configProvider;
    private final Locale locale;
    private final boolean multi;

    protected MessagesConfigSingleManager(DiscordSRV discordSRV, MessagesConfigManager<C> aggregateManager, Locale locale, boolean multi) {
        super(discordSRV);
        this.aggregateManager = aggregateManager;
        this.configProvider = aggregateManager::createConfiguration;
        this.locale = locale;
        this.multi = multi;
    }

    @Override
    public String fileName() {
        if (multi) {
            return aggregateManager.directory().resolve(locale.getISO3Language() + ".yaml").toString();
        }

        return MessagesConfig.FILE_NAME;
    }

    @Override
    public Locale locale() {
        return locale;
    }

    @Override
    public C createConfiguration() {
        return configProvider.get();
    }
}
