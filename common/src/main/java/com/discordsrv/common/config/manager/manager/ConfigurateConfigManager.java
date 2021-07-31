/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.config.manager.manager;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.annotation.DefaultOnly;
import com.discordsrv.common.config.manager.loader.ConfigLoaderProvider;
import com.discordsrv.common.exception.ConfigException;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class ConfigurateConfigManager<T, LT extends AbstractConfigurationLoader<CommentedConfigurationNode>>
        implements ConfigManager<T>, ConfigLoaderProvider<LT> {

    protected final DiscordSRV discordSRV;
    private final Path filePath;
    private final LT loader;
    private final ObjectMapper.Factory configObjectMapper;
    private final ObjectMapper.Factory defaultObjectMapper;

    protected T configuration;

    public ConfigurateConfigManager(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.filePath = new File(discordSRV.dataDirectory().toFile(), fileName()).toPath();
        this.loader = createLoader(filePath, configNodeOptions());
        this.configObjectMapper = configObjectMapperBuilder().build();
        this.defaultObjectMapper = defaultObjectMapperBuilder().build();
    }

    public Path filePath() {
        return filePath;
    }

    public LT loader() {
        return loader;
    }

    @Override
    public T config() {
        return configuration;
    }

    protected abstract String fileName();

    public ConfigurationOptions defaultOptions() {
        return ConfigurationOptions.defaults()
                .shouldCopyDefaults(false);
    }

    protected ObjectMapper.Factory.Builder objectMapperBuilder() {
        return ObjectMapper.factoryBuilder();
    }

    public ConfigurationOptions configNodeOptions() {
        return defaultOptions();
    }

    protected ObjectMapper.Factory.Builder configObjectMapperBuilder() {
        return objectMapperBuilder();
    }

    public ObjectMapper.Factory configObjectMapper() {
        return configObjectMapper;
    }

    public ConfigurationOptions defaultNodeOptions() {
        return defaultOptions();
    }

    protected ObjectMapper.Factory.Builder defaultObjectMapperBuilder() {
        return configObjectMapperBuilder()
                .addProcessor(DefaultOnly.class, (data, value) -> (value1, destination) -> {
                    String[] children = data.value();
                    boolean whitelist = data.whitelist();

                    if (children.length == 0) {
                        try {
                            destination.set(null);
                        } catch (SerializationException e) {
                            e.printStackTrace();
                        }
                        return;
                    }

                    List<String> list = Arrays.asList(children);
                    for (Map.Entry<Object, ? extends ConfigurationNode> entry : destination.childrenMap().entrySet()) {
                        Object key = entry.getKey();
                        if (!(key instanceof String)) {
                            continue;
                        }

                        if (list.contains(entry.getKey()) == whitelist) {
                            continue;
                        }

                        try {
                            entry.getValue().set(null);
                        } catch (SerializationException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    public ObjectMapper.Factory defaultObjectMapper() {
        return defaultObjectMapper;
    }

    private CommentedConfigurationNode getDefault(T defaultConfig, boolean cleanMapper) throws SerializationException {
        CommentedConfigurationNode node = CommentedConfigurationNode.root(defaultNodeOptions());
        (cleanMapper ? defaultObjectMapper() : configObjectMapper())
                .get(defaultConfig.getClass()).load(node);
        return node;
    }

    @Nullable
    protected ConfigurationNode getTranslation() throws ConfigurateException {
        return null;
    }

    @Override
    public void load() throws ConfigException {
        reload();
        save();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void reload() throws ConfigException {
        T defaultConfig = createConfiguration();
        try {
            CommentedConfigurationNode node;
            if (filePath().toFile().exists()) {
                // Config file exists, load from that
                node = loader().load();

                ConfigurationNode translation = getTranslation();
                if (translation != null) {
                    // Merge translation
                    node.mergeFrom(translation);
                }

                // Apply defaults that may not be there
                node.mergeFrom(getDefault(defaultConfig, true));
            } else {
                node = getDefault(defaultConfig, false);
            }

            this.configuration = configObjectMapper()
                    .get((Class<T>) defaultConfig.getClass())
                    .load(node);
        } catch (ConfigurateException e) {
            Class<?> configClass = defaultConfig.getClass();
            if (!configClass.isAnnotationPresent(ConfigSerializable.class)) {
                // Not very obvious and can easily happen
                throw new ConfigException(configClass.getName()
                        + " is not annotated with @ConfigSerializable", e);
            }

            throw new ConfigException("Failed to load configuration", e);
        }
    }

    @Override
    public void save() throws ConfigException {
        try {
            CommentedConfigurationNode node = loader.createNode();
            node.set(configuration);
            loader.save(node);
        } catch (ConfigurateException e) {
            throw new ConfigException("Failed to load configuration", e);
        }
    }
}
