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

import com.discordsrv.bukkit.config.main.BukkitConfig;
import com.discordsrv.bungee.config.main.BungeeConfig;
import com.discordsrv.common.config.Config;
import com.discordsrv.common.config.configurate.manager.abstraction.ConfigurateConfigManager;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.config.main.channels.base.proxy.ProxyBaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.proxy.ProxyChannelConfig;
import com.discordsrv.common.config.main.channels.base.server.ServerBaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.server.ServerChannelConfig;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.core.logging.backend.impl.JavaLoggerImpl;
import com.discordsrv.dependencies.net.dv8tion.jda.internal.utils.tuple.Pair;
import com.discordsrv.modded.config.main.ModdedConfig;
import com.discordsrv.velocity.config.main.VelocityConfig;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.jackson.JacksonConfigurationLoader;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * A java application to generate a default config file and a translation file that has comments as options.
 */
public final class DiscordSRVConfigTool {

    private static final Path DATA_DIRECTORY = Paths.get(".");

    private static final IChannelConfig.Serializer SERVER_CHANNEL_SERIALIZER = new IChannelConfig.Serializer(ObjectMapper.factoryBuilder().build(), ServerBaseChannelConfig.class, ServerChannelConfig.class);
    private static final IChannelConfig.Serializer PROXY_CHANNEL_SERIALIZER = new IChannelConfig.Serializer(ObjectMapper.factoryBuilder().build(), ProxyBaseChannelConfig.class, ProxyChannelConfig.class);

    private static final List<Pair<Config, IChannelConfig.Serializer>> CONFIGS = Arrays.asList(
            Pair.of(new BukkitConfig(), SERVER_CHANNEL_SERIALIZER),
            Pair.of(new ModdedConfig(), SERVER_CHANNEL_SERIALIZER),
            Pair.of(new BungeeConfig(), PROXY_CHANNEL_SERIALIZER),
            Pair.of(new VelocityConfig(), PROXY_CHANNEL_SERIALIZER),
            Pair.of(new ConnectionConfig(), null),
            Pair.of(new MessagesConfig(), null)
    );

    public static void main(String[] args) throws ConfigurateException {
        boolean i18n = false, defaultConfigs = false;

        for (String arg : args) {
            if (arg.equalsIgnoreCase("--i18n")) i18n = true;
            if (arg.equalsIgnoreCase("--defaults")) defaultConfigs = true;
        }

        if (!i18n && !defaultConfigs) {
            System.out.println("Usage: java -jar DiscordSRVConfigTool.jar [--i18n] [--defaults]");
            System.out.println("  --i18n: Generate a translation file with comments as options.");
            System.out.println("  --defaults: Generate default commented configuration files for each config class.");
            System.exit(0);
        }

        new DiscordSRVConfigTool().run(i18n, defaultConfigs);
    }

    private DiscordSRVConfigTool() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void run(boolean i18n, boolean defaultConfigs) throws ConfigurateException {
        CommentedConfigurationNode translationNode = CommentedConfigurationNode.root();
        for (Pair<Config, IChannelConfig.Serializer> configSerializerPair : CONFIGS) {
            Config config = configSerializerPair.getLeft();
            ConfigurateConfigManager configManager = new ConfigurateConfigManager(DATA_DIRECTORY, null) {
                @Override
                public String fileName() {
                    return config.getFileName();
                }

                @Override
                public Object createConfiguration() {
                    return config;
                }

                @Override
                public AbstractConfigurationLoader.Builder createBuilder() {
                    return null;
                }

                @Override
                protected Field headerField() throws ReflectiveOperationException {
                    return config.getClass().getField("HEADER");
                }

                @Override
                public IChannelConfig.Serializer getChannelConfigSerializer(ObjectMapper.Factory mapperFactory) {
                    return configSerializerPair.getRight() != null ? configSerializerPair.getRight() : super.getChannelConfigSerializer(mapperFactory);
                }
            };
            String fileIdentifier = config.getClass().getSimpleName() + "_" + config.getFileName();

            ConfigManagerProxy<Config> configManagerProxy = new ConfigManagerProxy<>(DATA_DIRECTORY, JavaLoggerImpl.getRoot(), configManager);
            ObjectMapper.Factory mapperFactory = configManager.objectMapperBuilder(true).build();
            CommentedConfigurationNode configurationNode = (CommentedConfigurationNode) configManagerProxy.getDefaultNode(mapperFactory, configManager.configurationOptions(mapperFactory, true));

            // A default configuration file is generated for each config class, which contains the default configuration values and comments for each config class.
            // This file can be used as a reference for users to understand the configuration options available in the plugin.
            if (defaultConfigs) {
                // Use the same loader builder as the plugin/mod, so the output is formatted identically (e.g. no line splitting)
                YamlConfigurationLoader loader = configManagerProxy.createBuilder()
                        .path(Paths.get("build", "config", fileIdentifier))
                        .defaultOptions(configManagerProxy.nodeOptions(true))
                        .build();

                loader.save(configurationNode);
                System.out.println("Default configuration file generated: " + Paths.get("build", "config", fileIdentifier).toAbsolutePath());
            }

            if (!i18n) continue;
            // Here we are generating a JSON file that contains the default configuration values and comments for each config class, which can be used for translation purposes.
            ConfigurationNode commentSection = translationNode.node(fileIdentifier + "_comments");

            String header = configManagerProxy.nodeOptions(true).header();
            if (header != null) {
                commentSection.node("$header").set(header);
            }

            convertCommentsToOptions(configurationNode, commentSection);
            processUnwantedValues(configurationNode);

            ConfigurationNode section = translationNode.node(fileIdentifier);
            ConfigurationNode configSection = section.set(configurationNode);
            section.set(configSection);
        }

        if(i18n) {
            JacksonConfigurationLoader.builder()
                    .path(Paths.get("build", "i18n", "source.json"))
                    .build()
                    .save(translationNode);
            System.out.println("Translation source file generated: " + Paths.get("build", "i18n", "source.json").toAbsolutePath());
        }
    }

    public void processUnwantedValues(ConfigurationNode node) throws SerializationException {
        Map<Object, ? extends ConfigurationNode> children = node.childrenMap();
        Object value;
        if (children.isEmpty() && (!((value = node.get(Object.class)) instanceof String) || ((String) value).isEmpty())) {
            node.set(null);
            return;
        }

        boolean allChildrenEmpty = true;
        for (ConfigurationNode child : children.values()) {
            processUnwantedValues(child);

            if (child.virtual() || child.isNull() || child.empty()) {
                allChildrenEmpty = false;
                break;
            }
        }
        if (!allChildrenEmpty) {
            node.set(null);
        }
    }

    public void convertCommentsToOptions(ConfigurationNode node, ConfigurationNode commentParent) throws SerializationException {
        if (node instanceof CommentedConfigurationNode commentedNode) {
            String comment = commentedNode.comment();
            if (comment != null) {
                List<Object> path = new ArrayList<>(Arrays.asList(commentedNode.path().array()));
                path.add("_comment");
                commentParent.node(path).set(comment);
            }
        }
        if (node.empty()) {
            node.set(null);
            return;
        }
        for (ConfigurationNode value : node.childrenMap().values()) {
            convertCommentsToOptions(value, commentParent);
        }
    }
}
