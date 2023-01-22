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

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.manager.loader.YamlConfigLoaderProvider;
import com.discordsrv.common.config.manager.manager.ConfigurateConfigManager;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public class TranslationConfigManagerProxy<C>
        extends ConfigurateConfigManager<C, YamlConfigurationLoader>
        implements YamlConfigLoaderProvider {

    private final ObjectMapper.Factory objectMapper;
    private final ConfigurateConfigManager<C, ?> configManager;

    public TranslationConfigManagerProxy(DiscordSRV discordSRV, ObjectMapper.Factory objectMapper, ConfigurateConfigManager<C, ?> configManager) {
        super(discordSRV);
        this.objectMapper = objectMapper;
        this.configManager = configManager;
    }

    @Override
    public ObjectMapper.Factory configObjectMapper() {
        return objectMapper != null ? objectMapper : super.configObjectMapper();
    }

    @Override
    public ObjectMapper.Factory defaultObjectMapper() {
        return objectMapper != null ? objectMapper : super.defaultObjectMapper();
    }

    @Override
    protected String fileName() {
        return "none";
    }

    @Override
    public C createConfiguration() {
        return configManager.createConfiguration();
    }
}
