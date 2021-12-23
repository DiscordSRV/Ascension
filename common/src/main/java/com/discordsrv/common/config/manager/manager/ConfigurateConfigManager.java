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

import com.discordsrv.api.color.Color;
import com.discordsrv.api.discord.api.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.annotation.DefaultOnly;
import com.discordsrv.common.config.annotation.Order;
import com.discordsrv.common.config.fielddiscoverer.OrderedFieldDiscovererProxy;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.ChannelConfig;
import com.discordsrv.common.config.manager.loader.ConfigLoaderProvider;
import com.discordsrv.common.config.serializer.ColorSerializer;
import com.discordsrv.common.config.serializer.DiscordMessageEmbedSerializer;
import com.discordsrv.common.config.serializer.PatternSerializer;
import com.discordsrv.common.config.serializer.SendableDiscordMessageSerializer;
import com.discordsrv.common.exception.ConfigException;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.FieldDiscoverer;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.util.NamingScheme;
import org.spongepowered.configurate.util.NamingSchemes;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class ConfigurateConfigManager<T, LT extends AbstractConfigurationLoader<CommentedConfigurationNode>>
        implements ConfigManager<T>, ConfigLoaderProvider<LT> {

    public static NamingScheme NAMING_SCHEME = in -> {
        in = Character.toLowerCase(in.charAt(0)) + in.substring(1);
        in = NamingSchemes.LOWER_CASE_DASHED.coerce(in);
        return in;
    };

    protected final DiscordSRV discordSRV;
    private final Path filePath;
    private final ObjectMapper.Factory configObjectMapper;
    private final ObjectMapper.Factory defaultObjectMapper;
    private final LT loader;

    protected T configuration;

    public ConfigurateConfigManager(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.filePath = discordSRV.dataDirectory().resolve(fileName());
        this.configObjectMapper = configObjectMapperBuilder().build();
        this.defaultObjectMapper = defaultObjectMapperBuilder().build();
        this.loader = createLoader(filePath, configNodeOptions());
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

    public ChannelConfig.Serializer getChannelConfigSerializer( ObjectMapper.Factory mapperFactory) {
        return new ChannelConfig.Serializer(mapperFactory, BaseChannelConfig.class, ChannelConfig.class);
    }

    public ConfigurationOptions defaultOptions() {
        return ConfigurationOptions.defaults()
                .shouldCopyDefaults(false)
                .implicitInitialization(false)
                .serializers(builder -> {
                    ObjectMapper.Factory objectMapper = configObjectMapper();
                    builder.register(BaseChannelConfig.class, getChannelConfigSerializer(objectMapper));
                    builder.register(Color.class, new ColorSerializer());
                    builder.register(Pattern.class, new PatternSerializer());
                    builder.register(DiscordMessageEmbed.Builder.class, new DiscordMessageEmbedSerializer(NAMING_SCHEME));
                    builder.register(DiscordMessageEmbed.Field.class, new DiscordMessageEmbedSerializer.FieldSerializer(NAMING_SCHEME));
                    builder.register(SendableDiscordMessage.Builder.class, new SendableDiscordMessageSerializer(NAMING_SCHEME));

                    // give Configurate' serializers the ObjectMapper mapper
                    builder.register(type -> {
                        String typeName = type.getTypeName();
                        return typeName.startsWith("com.discordsrv")
                                && !typeName.startsWith("com.discordsrv.dependencies")
                                && !typeName.contains(".serializer");
                    }, objectMapper.asTypeSerializer());
                });
    }

    public ConfigurationOptions configNodeOptions() {
        return defaultOptions();
    }

    public ConfigurationOptions defaultNodeOptions() {
        return defaultOptions();
    }

    @SuppressWarnings("unchecked")
    public ObjectMapper.Factory.Builder objectMapperBuilder() {
        Comparator<OrderedFieldDiscovererProxy.FieldCollectorData<Object, ?>> fieldOrder = Comparator.comparingInt(data -> {
            Order order = data.annotations().getAnnotation(Order.class);
            return order != null ? order.value() : 0;
        });

        return ObjectMapper.factoryBuilder()
                .defaultNamingScheme(NAMING_SCHEME)
                .addDiscoverer(new OrderedFieldDiscovererProxy<>((FieldDiscoverer<Object>) FieldDiscoverer.emptyConstructorObject(), fieldOrder))
                .addDiscoverer(new OrderedFieldDiscovererProxy<>((FieldDiscoverer<Object>) FieldDiscoverer.record(), fieldOrder));
    }

    public ObjectMapper.Factory.Builder configObjectMapperBuilder() {
        return objectMapperBuilder();
    }

    protected ObjectMapper.Factory.Builder defaultObjectMapperBuilder() {
        return objectMapperBuilder()
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

    public ObjectMapper.Factory configObjectMapper() {
        return configObjectMapper;
    }

    public ObjectMapper.Factory defaultObjectMapper() {
        return defaultObjectMapper;
    }

    private CommentedConfigurationNode getDefault(T defaultConfig, boolean cleanMapper) throws SerializationException {
        return getDefault(defaultConfig, cleanMapper ? defaultObjectMapper() : configObjectMapper());
    }

    @SuppressWarnings("unchecked")
    private CommentedConfigurationNode getDefault(T defaultConfig, ObjectMapper.Factory mapperFactory) throws SerializationException {
        CommentedConfigurationNode node = CommentedConfigurationNode.root(defaultNodeOptions());
        mapperFactory.get((Class<T>) defaultConfig.getClass()).save(defaultConfig, node);
        return node;
    }

    @Nullable
    protected ConfigurationNode getTranslation() throws ConfigurateException {
        return null;
    }

    public CommentedConfigurationNode getDefaultNode(ObjectMapper.Factory mapperFactory) throws ConfigurateException {
        return getDefault(createConfiguration(), mapperFactory);
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

    @SuppressWarnings("unchecked")
    @Override
    public void save() throws ConfigException {
        try {
            CommentedConfigurationNode node = loader.createNode();
            save(configuration, (Class<T>) configuration.getClass(), node);
            loader.save(node);
        } catch (ConfigurateException e) {
            throw new ConfigException("Failed to load configuration", e);
        }
    }

    protected void save(T config, Class<T> clazz, CommentedConfigurationNode node) throws SerializationException {
        configObjectMapper().get(clazz).save(config, node);
    }
}
