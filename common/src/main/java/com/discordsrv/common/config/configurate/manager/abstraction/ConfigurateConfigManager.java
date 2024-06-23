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

package com.discordsrv.common.config.configurate.manager.abstraction;

import com.discordsrv.api.color.Color;
import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.configurate.annotation.DefaultOnly;
import com.discordsrv.common.config.configurate.annotation.Order;
import com.discordsrv.common.config.configurate.fielddiscoverer.OrderedFieldDiscovererProxy;
import com.discordsrv.common.config.configurate.manager.loader.ConfigLoaderProvider;
import com.discordsrv.common.config.configurate.serializer.*;
import com.discordsrv.common.config.helper.MinecraftMessage;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.ChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.exception.ConfigException;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.*;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.FieldDiscoverer;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Processor;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.util.NamingScheme;
import org.spongepowered.configurate.util.NamingSchemes;
import org.spongepowered.configurate.yaml.ScalarStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class ConfigurateConfigManager<T, LT extends AbstractConfigurationLoader<CommentedConfigurationNode>>
        implements ConfigManager<T>, ConfigLoaderProvider<LT> {

    public static ThreadLocal<Boolean> CLEAN_MAPPER = ThreadLocal.withInitial(() -> false);

    public static NamingScheme NAMING_SCHEME = in -> {
        in = Character.toLowerCase(in.charAt(0)) + in.substring(1);
        in = NamingSchemes.LOWER_CASE_DASHED.coerce(in);
        return in;
    };

    private final Path filePath;
    private final Logger logger;
    private final ObjectMapper.Factory objectMapper;
    private final ObjectMapper.Factory cleanObjectMapper;
    private LT loader;

    protected T configuration;

    public ConfigurateConfigManager(DiscordSRV discordSRV) {
        this(discordSRV.dataDirectory(), new NamedLogger(discordSRV, "CONFIG"));
    }

    protected ConfigurateConfigManager(Path dataDirectory, Logger logger) {
        this.filePath = dataDirectory.resolve(fileName());
        this.logger = logger;
        this.objectMapper = objectMapperBuilder(true).build();
        this.cleanObjectMapper = cleanObjectMapperBuilder().build();
    }

    public Path filePath() {
        return filePath;
    }

    public LT loader() {
        if (loader == null) {
            loader = createLoader(filePath(), nodeOptions(true)).build();
        }
        return loader;
    }

    @Override
    public T config() {
        return configuration;
    }

    public abstract String fileName();

    protected Field headerField() throws ReflectiveOperationException {
        return null;
    }

    protected String header() {
        try {
            Field headerField = headerField();
            return headerField != null ? (String) headerField.get(null) : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
    protected String[] headerConstants() {
        try {
            Field headerField = headerField();
            if (headerField == null) {
                return new String[0];
            }

            Constants constants = headerField.getAnnotation(Constants.class);
            if (constants == null) {
                return new String[0];
            }

            return constants.value();
        } catch (ReflectiveOperationException e) {
            return new String[0];
        }
    }

    public IChannelConfig.Serializer getChannelConfigSerializer(ObjectMapper.Factory mapperFactory) {
        return new IChannelConfig.Serializer(mapperFactory, BaseChannelConfig.class, ChannelConfig.class);
    }

    @SuppressWarnings("unchecked") // Special Class cast
    public ConfigurationOptions configurationOptions(ObjectMapper.Factory objectMapper, boolean headerSubstitutions) {
        String header = header();
        if (header != null && headerSubstitutions) {
            header = doSubstitution(header, headerConstants());
        }

        return ConfigurationOptions.defaults()
                .header(header)
                .shouldCopyDefaults(false)
                .implicitInitialization(false)
                .serializers(builder -> {
                    builder.register(String.class, new TypeSerializer<String>() {

                        @Override
                        public String deserialize(Type type, ConfigurationNode node) {
                            return node.getString();
                        }

                        @Override
                        public void serialize(Type type, @org.checkerframework.checker.nullness.qual.Nullable String obj, ConfigurationNode node) {
                            RepresentationHint<ScalarStyle> hint = YamlConfigurationLoader.SCALAR_STYLE;

                            ScalarStyle style = node.hint(hint);
                            if (style == hint.defaultValue()) {
                                // Set scalar style for strings to double quotes, by default
                                node = node.hint(hint, ScalarStyle.DOUBLE_QUOTED);
                            }

                            node.raw(obj);
                        }
                    });
                    builder.register(BaseChannelConfig.class, getChannelConfigSerializer(objectMapper));
                    //noinspection unchecked
                    builder.register((Class<Enum<?>>) (Object) Enum.class, new EnumSerializer(logger));
                    builder.register(Color.class, new ColorSerializer());
                    builder.register(Pattern.class, new PatternSerializer());
                    builder.register(DiscordMessageEmbed.Builder.class, new DiscordMessageEmbedSerializer(NAMING_SCHEME));
                    builder.register(DiscordMessageEmbed.Field.class, new DiscordMessageEmbedSerializer.FieldSerializer(NAMING_SCHEME));
                    builder.register(SendableDiscordMessage.Builder.class, new SendableDiscordMessageSerializer(NAMING_SCHEME));
                    builder.register(MinecraftMessage.class, new MinecraftMessageSerializer());

                    // give Configurate' serializers the ObjectMapper mapper
                    builder.register(type -> {
                        String typeName = type.getTypeName();
                        return typeName.startsWith("com.discordsrv")
                                && !typeName.startsWith("com.discordsrv.dependencies")
                                && !typeName.contains(".serializer");
                    }, objectMapper.asTypeSerializer());
                });
    }

    public ConfigurationOptions nodeOptions(boolean headerSubstitutions) {
        return configurationOptions(objectMapper(), headerSubstitutions);
    }

    public ConfigurationOptions cleanNodeOptions() {
        return configurationOptions(cleanObjectMapper(), true);
    }

    @SuppressWarnings("unchecked")
    public ObjectMapper.Factory.Builder commonObjectMapperBuilder(boolean commentSubstitutions) {
        Comparator<OrderedFieldDiscovererProxy.FieldCollectorData<Object, ?>> fieldOrder = Comparator.comparingInt(data -> {
            Order order = data.annotations().getAnnotation(Order.class);
            return order != null ? order.value() : 0;
        });

        return ObjectMapper.factoryBuilder()
                .defaultNamingScheme(NAMING_SCHEME)
                .addDiscoverer(new OrderedFieldDiscovererProxy<>((FieldDiscoverer<Object>) FieldDiscoverer.emptyConstructorObject(), fieldOrder))
                .addDiscoverer(new OrderedFieldDiscovererProxy<>((FieldDiscoverer<Object>) FieldDiscoverer.record(), fieldOrder))
                .addProcessor(Constants.Comment.class, (data, fieldType) -> (value, destination) -> {
                    // This needs to go before comment processing.
                    if (commentSubstitutions && destination instanceof CommentedConfigurationNode) {
                        String comment = ((CommentedConfigurationNode) destination).comment();
                        if (StringUtils.isEmpty(comment)) {
                            logger.error(
                                    Arrays.stream(destination.path().array()).map(Object::toString).collect(Collectors.joining(", "))
                                            + " is not commented but has comment constants! (make sure @Constants.Comment is below @Comment)"
                            );
                            return;
                        }
                        ((CommentedConfigurationNode) destination).comment(
                                doSubstitution(comment, getValues(data.value(), data.intValue()))
                        );
                    }
                })
                .addProcessor(Constants.class, (data, fieldType) -> (value, destination) -> {
                    String[] values = getValues(data.value(), data.intValue());
                    if (values.length == 0) {
                        return;
                    }

                    String optionValue = destination.getString();
                    if (optionValue == null) {
                        return;
                    }

                    try {
                        destination.set(doSubstitution(destination.getString(), values));
                    } catch (SerializationException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private String[] getValues(String[] value, int[] intValue) {
        List<String> values = new ArrayList<>(Arrays.asList(value));
        for (int i : intValue) {
            values.add(String.valueOf(i));
        }
        return values.toArray(new String[0]);
    }

    private static String doSubstitution(String input, String[] values) {
        for (int i = 0; i < values.length; i++) {
            input = input.replace("%" + (i + 1), values[i]);
        }
        return input;
    }

    public ObjectMapper.Factory.Builder objectMapperBuilder(boolean commentSubstitutions) {
        return commonObjectMapperBuilder(commentSubstitutions)
                .addProcessor(Comment.class, (data, fieldType) -> {
                    Processor<Object> processor = Processor.comments().make(data, fieldType);

                    return (value, destination) -> {
                        processor.process(value, destination);
                        if (destination instanceof CommentedConfigurationNode) {
                            String comment = ((CommentedConfigurationNode) destination).comment();
                            if (comment != null) {
                                // Yaml doesn't render empty lines correctly, so we add a space when there are double line breaks
                                ((CommentedConfigurationNode) destination).comment(comment.replace("\n\n", "\n \n"));
                            }
                        }
                    };
                });
    }

    protected ObjectMapper.Factory.Builder cleanObjectMapperBuilder() {
        return commonObjectMapperBuilder(true)
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

    public ObjectMapper.Factory objectMapper() {
        return objectMapper;
    }

    public ObjectMapper.Factory cleanObjectMapper() {
        return cleanObjectMapper;
    }

    /**
     * Gets the default config given the default object from {@link #createConfiguration()}
     * @param defaultConfig the object
     * @param cleanMapper if options that are marked with {@link DefaultOnly} or serializers that make use of {@link #CLEAN_MAPPER} should be excluded from the node
     * @return the node with the values from the object
     * @throws SerializationException if serialization fails
     */
    private CommentedConfigurationNode getDefault(T defaultConfig, boolean cleanMapper) throws SerializationException {
        try {
            if (cleanMapper) {
                CLEAN_MAPPER.set(true);
            }
            return getDefault(defaultConfig, cleanMapper ? cleanObjectMapper() : objectMapper());
        } finally {
            if (cleanMapper) {
                CLEAN_MAPPER.set(false);
            }
        }
    }

    public CommentedConfigurationNode getDefaultNode(ObjectMapper.Factory mapperFactory) throws ConfigurateException {
        return getDefault(createConfiguration(), mapperFactory);
    }

    @SuppressWarnings("unchecked")
    private CommentedConfigurationNode getDefault(T defaultConfig, ObjectMapper.Factory mapperFactory) throws SerializationException {
        CommentedConfigurationNode node = CommentedConfigurationNode.root(cleanNodeOptions());
        mapperFactory.get((Class<T>) defaultConfig.getClass()).save(defaultConfig, node);
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

            this.configuration = objectMapper()
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
    public void save(AbstractConfigurationLoader<CommentedConfigurationNode> loader) throws ConfigException {
        try {
            CommentedConfigurationNode node = loader.createNode();
            save(configuration, (Class<T>) configuration.getClass(), node);
            loader.save(node);
        } catch (ConfigurateException e) {
            throw new ConfigException("Failed to load configuration", e);
        }
    }

    @Override
    public void save() throws ConfigException {
        LT loader = loader();
        save(loader);
    }

    protected void save(T config, Class<T> clazz, CommentedConfigurationNode node) throws SerializationException {
        objectMapper().get(clazz).save(config, node);
    }
}
