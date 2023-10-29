/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.config;

import com.discordsrv.common.config.configurate.manager.abstraction.ConfigurateConfigManager;
import com.discordsrv.common.config.configurate.manager.loader.YamlConfigLoaderProvider;
import com.discordsrv.common.logging.Logger;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;

public class TranslationConfigManagerProxy<C>
        extends ConfigurateConfigManager<C, YamlConfigurationLoader>
        implements YamlConfigLoaderProvider {

    private final ObjectMapper.Factory objectMapper;
    private final ConfigurateConfigManager<C, ?> configManager;

    public TranslationConfigManagerProxy(Path dataDirectory, Logger logger, ObjectMapper.Factory objectMapper, ConfigurateConfigManager<C, ?> configManager) {
        super(dataDirectory, logger);
        this.objectMapper = objectMapper;
        this.configManager = configManager;
    }

    @Override
    public ObjectMapper.Factory objectMapper() {
        return objectMapper != null ? objectMapper : super.objectMapper();
    }

    @Override
    public ObjectMapper.Factory cleanObjectMapper() {
        return objectMapper != null ? objectMapper : super.cleanObjectMapper();
    }

    @Override
    public String fileName() {
        return "none";
    }

    @Override
    public C createConfiguration() {
        return configManager.createConfiguration();
    }
}
