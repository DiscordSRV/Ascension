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

package com.discordsrv.config;

import com.discordsrv.bukkit.config.connection.BukkitConnectionConfig;
import com.discordsrv.bukkit.config.main.BukkitConfig;
import com.discordsrv.common.config.Config;
import com.discordsrv.common.config.annotation.Untranslated;
import com.discordsrv.common.config.main.channels.BaseChannelConfig;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A java application to generate a translation file that has comments as options.
 */
public final class DiscordSRVTranslation {

    public static final List<Config> CONFIG_INSTANCES = Arrays.asList(
            new BukkitConfig(),
            new BukkitConnectionConfig()
    );

    public static void main(String[] args) throws ConfigurateException {
        new DiscordSRVTranslation().run();
    }

    private DiscordSRVTranslation() {}

    @SuppressWarnings("unchecked")
    public void run() throws ConfigurateException {
        ObjectMapper.Factory objectMapper = ObjectMapper.factoryBuilder()
                .addProcessor(Untranslated.class, (data, value) -> (value1, destination) -> {
                    try {
                        Untranslated.Type type = data.value();
                        if (type.isValue()) {
                            if (type.isComment()) {
                                destination.set(null);
                            } else {
                                destination.set("");
                            }
                        } else if (type.isComment() && destination instanceof CommentedConfigurationNode) {
                            ((CommentedConfigurationNode) destination).comment(null);
                        }
                    } catch (SerializationException e) {
                        e.printStackTrace();
                        System.exit(1);
                    }
                })
                .build();


        BaseChannelConfig.Serializer channelSerializer = new BaseChannelConfig.Serializer(objectMapper);
        CommentedConfigurationNode node = CommentedConfigurationNode.root(ConfigurationOptions.defaults()
                .serializers(builder -> builder.register(BaseChannelConfig.class, channelSerializer)));
        for (Config config : CONFIG_INSTANCES) {
            ConfigurationNode section = node.node(config.getFileName());
            ConfigurationNode configSection = section.copy();

            ObjectMapper<Config> mapper = objectMapper.get((Class<Config>) config.getClass());
            mapper.save(config, configSection);
            convertCommentsToOptions(configSection, configSection);

            section.set(configSection);
        }

        YamlConfigurationLoader.builder()
                .file(new File("i18n", "source.yml"))
                .build()
                .save(node);
    }

    public void convertCommentsToOptions(ConfigurationNode node, ConfigurationNode parent) throws SerializationException {
        if (node instanceof CommentedConfigurationNode) {
            CommentedConfigurationNode commentedNode = (CommentedConfigurationNode) node;
            String comment = commentedNode.comment();
            if (comment != null) {
                List<Object> arr = new ArrayList<>();
                arr.add("_comments");
                arr.addAll(Arrays.asList(commentedNode.path().array()));
                parent.node(arr).set(comment);
            }
        }
        if (node.empty()) {
            node.set(null);
            return;
        }
        for (ConfigurationNode value : node.childrenMap().values()) {
            convertCommentsToOptions(value, parent);
        }
    }
}
