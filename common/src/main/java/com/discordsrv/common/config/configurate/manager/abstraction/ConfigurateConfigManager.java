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

package com.discordsrv.common.config.configurate.manager.abstraction;

import com.discordsrv.api.color.Color;
import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.configurate.annotation.DefaultOnly;
import com.discordsrv.common.config.configurate.annotation.Order;
import com.discordsrv.common.config.configurate.fielddiscoverer.FieldValueDiscovererProxy;
import com.discordsrv.common.config.configurate.fielddiscoverer.OrderedFieldDiscovererProxy;
import com.discordsrv.common.config.configurate.manager.loader.ConfigLoaderProvider;
import com.discordsrv.common.config.configurate.serializer.*;
import com.discordsrv.common.config.configurate.serializer.helper.BothMessageSerializer;
import com.discordsrv.common.config.configurate.serializer.helper.DiscordMessageSerializer;
import com.discordsrv.common.config.configurate.serializer.helper.MinecraftMessageSerializer;
import com.discordsrv.common.config.helper.BothMessage;
import com.discordsrv.common.config.helper.DiscordMessage;
import com.discordsrv.common.config.helper.MinecraftMessage;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.ChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.exception.ConfigException;
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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class ConfigurateConfigManager<T, LT extends AbstractConfigurationLoader<CommentedConfigurationNode>>
        implements ConfigManager<T>, ConfigLoaderProvider<LT> {

    public static final ThreadLocal<Boolean> DEFAULT_CONFIG = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> SAVE_OR_LOAD = ThreadLocal.withInitial(() -> false);

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

    public static void nullAllFields(Object object) {
        if (!SAVE_OR_LOAD.get()) {
            return;
        }

        Class<?> clazz = object.getClass();
        while (clazz != null) {
            for (Field field : clazz.getFields()) {
                if (field.getType().isPrimitive()) {
                    continue;
                }

                int modifiers = field.getModifiers();
                if (!Modifier.isPublic(modifiers) || Modifier.isFinal(modifiers) || Modifier.isStatic(modifiers)) {
                    continue;
                }

                try {
                    field.set(object, null);
                } catch (IllegalAccessException ignored) {}
            }
            clazz = clazz.getSuperclass();
        }
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

    @SuppressWarnings("unchecked")
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
                        public void serialize(Type type, @Nullable String obj, ConfigurationNode node) {
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
                    builder.register(SendableDiscordMessage.Builder.class, new SendableDiscordMessageSerializer(NAMING_SCHEME, false));
                    builder.register(MinecraftMessage.class, new MinecraftMessageSerializer(NAMING_SCHEME));
                    builder.register(DiscordMessage.class, new DiscordMessageSerializer(NAMING_SCHEME));
                    builder.register(BothMessage.class, new BothMessageSerializer(NAMING_SCHEME));

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
    public ObjectMapper.Factory.Builder commonObjectMapperBuilder(boolean substitutions) {
        Comparator<OrderedFieldDiscovererProxy.FieldCollectorData<Object, ?>> fieldOrder = Comparator.comparingInt(data -> {
            Order order = data.annotations().getAnnotation(Order.class);
            return order != null ? order.value() : 0;
        });

        return ObjectMapper.factoryBuilder()
                .defaultNamingScheme(NAMING_SCHEME)
                .addDiscoverer(new OrderedFieldDiscovererProxy<>((FieldDiscoverer<Object>) (Object) FieldValueDiscovererProxy.EMPTY_CONSTRUCTOR_INSTANCE, fieldOrder))
                .addDiscoverer(new OrderedFieldDiscovererProxy<>((FieldDiscoverer<Object>) FieldDiscoverer.record(), fieldOrder))
                .addProcessor(Constants.Comment.class, (data, fieldType) -> (value, destination) -> {
                    // This needs to go before comment processing.
                    if (substitutions && destination instanceof CommentedConfigurationNode) {
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
                    if (!substitutions) {
                        return;
                    }

                    String[] constants = getValues(data.value(), data.intValue());
                    if (constants.length == 0) {
                        return;
                    }
                    applyConstantsRecursively(constants, destination);
                });
    }

    private void applyConstantsRecursively(String[] constants, ConfigurationNode destination) {
        Object optionValue = destination.raw();
        if (optionValue instanceof String) {
            try {
                destination.set(doSubstitution(destination.getString(), constants));
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
            return;
        }

        Map<Object, ? extends ConfigurationNode> children = destination.childrenMap();
        for (ConfigurationNode value : children.values()) {
            applyConstantsRecursively(constants, value);
        }
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

    public ObjectMapper.Factory.Builder objectMapperBuilder(boolean substitutions) {
        return commonObjectMapperBuilder(substitutions)
                .addProcessor(Comment.class, (data, fieldType) -> {
                    Processor<Object> processor = Processor.comments().make(data, fieldType);

                    return (value, destination) -> {
                        processor.process(value, destination);
                        if (substitutions && destination instanceof CommentedConfigurationNode) {
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
                    if (data.entireOption()) {
                        try {
                            destination.set(null);
                        } catch (SerializationException e) {
                            e.printStackTrace();
                        }
                        return;
                    }

                    // Children which will be excluded/included from the default node
                    String[] children = data.value();
                    boolean whitelist = data.whitelist();

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
     * @param cleanMapper if options that are marked with {@link DefaultOnly} or serializers that make use of {@link #DEFAULT_CONFIG} should be excluded from the node
     * @return the node with the values from the object
     * @throws SerializationException if serialization fails
     */
    private CommentedConfigurationNode getDefault(T defaultConfig, boolean cleanMapper) throws SerializationException {
        try {
            if (cleanMapper) {
                DEFAULT_CONFIG.set(true);
            }
            return getDefault(defaultConfig, cleanMapper ? cleanObjectMapper() : objectMapper());
        } finally {
            if (cleanMapper) {
                DEFAULT_CONFIG.set(false);
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

    protected void translate(CommentedConfigurationNode node) throws ConfigurateException {}

    @SuppressWarnings("unchecked")
    @Override
    public void reload(boolean forceSave, AtomicBoolean anyMissingOptions, @Nullable Path backupPath) throws ConfigException {
        T defaultConfig = createConfiguration();
        Class<T> defaultConfigClass = (Class<T>) defaultConfig.getClass();

        try {
            SAVE_OR_LOAD.set(true);

            if (!Files.exists(filePath)) {
                CommentedConfigurationNode node = getDefault(defaultConfig, false);
                translate(node);

                configuration = objectMapper().get(defaultConfigClass).load(node);

                // TODO: v1 migration

                save(loader());
                return;
            }

            // Load existing file & translate
            CommentedConfigurationNode node = loader().load();

            CommentedConfigurationNode defaultNode = getDefault(defaultConfig, true);
            translate(defaultNode);

            // Log missing options, apply (missing) defaults
            if (!forceSave) {
                // Only log if it's not being force saved
                checkIfValuesMissing(node, defaultNode, anyMissingOptions);
            }
            node.mergeFrom(defaultNode);

            configuration = objectMapper().get(defaultConfigClass).load(node);
            if (forceSave) {
                if (backupPath != null) {
                    Files.copy(filePath(), backupPath.resolve(fileName()));
                }
                save(loader);
            }
        } catch (IOException e) {
            Class<?> configClass = defaultConfig.getClass();
            if (e instanceof ConfigurateException && !configClass.isAnnotationPresent(ConfigSerializable.class)) {
                // Not very obvious and can easily happen
                throw new ConfigException(configClass.getName() + " is not annotated with @ConfigSerializable", e);
            }

            throw new ConfigException("Failed to load configuration", e);
        } finally {
            SAVE_OR_LOAD.set(false);
        }
    }

    private void checkIfValuesMissing(
            CommentedConfigurationNode node,
            CommentedConfigurationNode defaultNode,
            AtomicBoolean anyMissingOptions
    ) throws ConfigurateException {
        for (CommentedConfigurationNode child : defaultNode.childrenMap().values()) {
            CommentedConfigurationNode value = node.node(child.key());
            if (value.virtual()) {
                List<String> keys = new ArrayList<>();
                for (Object o : child.path()) {
                    keys.add(String.valueOf(o));
                }
                logger.warning("Missing option \"" + String.join(".", keys) + "\" in " + fileName());
                anyMissingOptions.set(true);
                continue;
            }

            checkIfValuesMissing(value, child, anyMissingOptions);
        }
        List<CommentedConfigurationNode> children = defaultNode.childrenList();
        if (!children.isEmpty()) {
            for (CommentedConfigurationNode childNode : node.childrenList()) {
                checkIfValuesMissing(childNode, children.get(0), anyMissingOptions);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void save(AbstractConfigurationLoader<CommentedConfigurationNode> loader) throws ConfigException {
        try {
            SAVE_OR_LOAD.set(true);
            CommentedConfigurationNode node = loader.createNode();

            // Save configuration to the node
            objectMapper().get((Class<T>) configuration.getClass()).save(configuration, node);

            // Save the node to the provided loader
            loader.save(node);
        } catch (ConfigurateException e) {
            throw new ConfigException("Failed to load configuration", e);
        } finally {
            SAVE_OR_LOAD.set(false);
        }
    }
}
