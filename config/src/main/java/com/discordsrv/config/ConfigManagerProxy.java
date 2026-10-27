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

package com.discordsrv.config;

import com.discordsrv.common.config.configurate.manager.abstraction.ConfigurateConfigManager;
import com.discordsrv.common.config.configurate.manager.loader.YamlConfigLoaderProvider;
import com.discordsrv.common.core.logging.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Path;

@SuppressWarnings("unchecked")
public class ConfigManagerProxy<C>
        extends ConfigurateConfigManager<C, YamlConfigurationLoader>
        implements YamlConfigLoaderProvider {

    private final ConfigurateConfigManager<C, ?> configManager;

    public ConfigManagerProxy(Path dataDirectory, Logger logger, ConfigurateConfigManager<C, ?> configManager) {
        super(dataDirectory, logger);
        this.configManager = configManager;
    }

    @Override
    public ObjectMapper.Factory objectMapper() {
        return configManager.objectMapper() != null ? configManager.objectMapper() : super.objectMapper();
    }

    @Override
    public ObjectMapper.Factory cleanObjectMapper() {
        return configManager.cleanObjectMapper() != null ? configManager.cleanObjectMapper() : super.cleanObjectMapper();
    }

    @Override
    public String fileName() {
        return "none";
    }

    @Override
    public C createConfiguration() {
        return configManager.createConfiguration();
    }

    public C getDefaultNode(ObjectMapper.Factory mapperFactory, ConfigurationOptions options)
            throws ConfigurateException {
        configuration = getDefaultNodeWithOptions(mapperFactory, options);
        return configuration;
    }

    private C getDefaultNodeWithOptions(ObjectMapper.Factory mapperFactory, ConfigurationOptions options)
            throws ConfigurateException {
        configuration = createConfiguration();
        CommentedConfigurationNode node = CommentedConfigurationNode.root(options);
        mapperFactory.get((Class<C>) configuration.getClass()).save(configuration, node);
        return (C) node;
    }
}
