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

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.Config;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.jackson.JacksonConfigurationLoader;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public abstract class TranslatedConfigManager<T extends Config, LT extends AbstractConfigurationLoader<CommentedConfigurationNode>>
        extends ConfigurateConfigManager<T, LT> {

    private final DiscordSRV discordSRV;
    private String header;

    public TranslatedConfigManager(DiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
    }

    protected TranslatedConfigManager(Path dataDirectory) {
        super(dataDirectory, null);
        this.discordSRV = null;
    }

    public Locale locale() {
        return discordSRV.defaultLocale();
    }

    @Override
    protected String header() {
        if (header != null) {
            return header;
        }
        return super.header();
    }

    @Override
    protected void translate(CommentedConfigurationNode node) throws ConfigurateException {
        String fileName = fileName();

        ConfigurationNode translationRoot = getTranslationRoot();
        if (translationRoot == null) {
            return;
        }

        ConfigurationNode translation = translationRoot.node(fileName);
        ConfigurationNode comments = translationRoot.node(fileName + "_comments");

        this.header = comments.node("$header").getString();
        translateNode(node, translation, comments);
    }

    private ConfigurationNode getTranslationRoot() throws ConfigurateException {
        if (discordSRV == null) {
            return null;
        }

        String languageCode = locale().getLanguage();
        String countryCode = locale().getCountry();

        ClassLoader classLoader = discordSRV.getClass().getClassLoader();

        URL resourceURL = classLoader.getResource("translations/" + languageCode + "_" + countryCode + ".yaml");
        if (resourceURL == null) {
            resourceURL = classLoader.getResource("translations/" + languageCode + ".yaml");
        }
        if (resourceURL == null) {
            return null;
        }

        return JacksonConfigurationLoader.builder().url(resourceURL).build().load();
    }

    private void translateNode(
            CommentedConfigurationNode node,
            ConfigurationNode translations,
            ConfigurationNode commentTranslations
    ) throws SerializationException {
        List<Object> path = new ArrayList<>(Arrays.asList(node.path().array()));

        String translation = translations.node(path).getString();
        if (translation != null) {
            node.set(translation);
        }

        path.add("_comment");
        String commentTranslation = commentTranslations.node(path).getString();
        if (commentTranslation != null) {
            node.comment(commentTranslation);
        }

        for (CommentedConfigurationNode child : node.childrenMap().values()) {
            translateNode(child, translations, commentTranslations);
        }
    }

}
