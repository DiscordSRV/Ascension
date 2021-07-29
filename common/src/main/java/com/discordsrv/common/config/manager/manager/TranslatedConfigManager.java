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
import com.discordsrv.common.config.Config;
import com.discordsrv.common.exception.ConfigException;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.net.URL;

public abstract class TranslatedConfigManager<T extends Config, LT extends AbstractConfigurationLoader<CommentedConfigurationNode>> extends ConfigurateConfigManager<T, LT> {

    public TranslatedConfigManager(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void load() throws ConfigException {
        super.reload();
        translate();
        super.save();
    }

    @Override
    protected @Nullable ConfigurationNode getTranslation() throws ConfigurateException {
        ConfigurationNode translation = getTranslationRoot();
        if (translation == null) {
            return null;
        }
        translation = translation.copy();
        translation.node("_comments").set(null);
        return translation;
    }

    public void translate() throws ConfigException {
        T config = config();
        if (config == null) {
            return;
        }

        try {
            ConfigurationNode translation = getTranslationRoot();
            if (translation == null) {
                return;
            }

            translation = translation.node(config.getFileName());

            CommentedConfigurationNode node = loader().createNode();
            node.set(config);
            translateNode(node, translation, translation.node("_comments"));
        } catch (ConfigurateException e) {
            throw new ConfigException(e);
        }
    }

    private ConfigurationNode getTranslationRoot() throws ConfigurateException {
        String languageCode = discordSRV.locale().getISO3Language();
        URL resourceURL = discordSRV.getClass().getClassLoader()
                .getResource("translations/" + languageCode + ".yml");
        if (resourceURL == null) {
            return null;
        }

        return YamlConfigurationLoader.builder().url(resourceURL).build().load();
    }

    private void translateNode(
            CommentedConfigurationNode node,
            ConfigurationNode translations,
            ConfigurationNode commentTranslations
    ) throws SerializationException {
        Object[] path = node.path().array();

        String translation = translations.node(path).getString();
        if (translation != null) {
            node.set(translation);
        }

        String commentTranslation = commentTranslations.node(path).getString();
        if (commentTranslation != null) {
            node.comment(commentTranslation);
        }

        for (CommentedConfigurationNode child : node.childrenMap().values()) {
            translateNode(child, translations, commentTranslations);
        }
    }

}
