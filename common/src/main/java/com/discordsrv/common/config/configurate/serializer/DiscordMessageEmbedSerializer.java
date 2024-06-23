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

package com.discordsrv.common.config.configurate.serializer;

import com.discordsrv.api.color.Color;
import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.common.config.configurate.manager.abstraction.ConfigurateConfigManager;
import net.dv8tion.jda.api.entities.Role;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.util.NamingScheme;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

public class DiscordMessageEmbedSerializer implements TypeSerializer<DiscordMessageEmbed.Builder> {

    private final NamingScheme namingScheme;

    public DiscordMessageEmbedSerializer(NamingScheme namingScheme) {
        this.namingScheme = namingScheme;
    }

    private String map(String option) {
        return namingScheme.coerce(option);
    }

    @Override
    public DiscordMessageEmbed.Builder deserialize(Type type, ConfigurationNode node) throws SerializationException {
        if (ConfigurateConfigManager.CLEAN_MAPPER.get()) {
            return null;
        }
        if (!node.node(map("Enabled")).getBoolean(node.node(map("Enable")).getBoolean(true))) {
            return null;
        }

        DiscordMessageEmbed.Builder builder = DiscordMessageEmbed.builder();

        Color color = node.node(map("Color")).get(Color.class);
        builder.setColor(color != null ? color.rgb() : Role.DEFAULT_COLOR_RAW);

        ConfigurationNode author = node.node(map("Author"));
        builder.setAuthor(
                author.node(map("Name")).getString(),
                author.node(map("Url")).getString(),
                author.node(map("ImageUrl")).getString());

        ConfigurationNode title = node.node(map("Title"));
        builder.setTitle(
                title.node(map("Text")).getString(),
                title.node(map("Url")).getString());

        builder.setDescription(node.node(map("Description")).getString());
        for (DiscordMessageEmbed.Field field : node.node(map("Fields")).getList(DiscordMessageEmbed.Field.class, Collections.emptyList())) {
            builder.addField(field);
        }

        builder.setThumbnailUrl(node.node(map("ThumbnailUrl")).getString());
        builder.setImageUrl(node.node(map("ImageUrl")).getString());

        // TODO: timestamp

        ConfigurationNode footer = node.node(map("Footer"));
        builder.setFooter(
                footer.node(map("Text")).getString(),
                footer.node(map("ImageUrl")).getString(footer.node(map("IconUrl")).getString("")));

        return builder;
    }

    @Override
    public void serialize(Type type, DiscordMessageEmbed.@Nullable Builder obj, ConfigurationNode node)
            throws SerializationException {
        if (obj == null || ConfigurateConfigManager.CLEAN_MAPPER.get()) {
            node.set(null);
            return;
        }

        node.node(map("Color")).set(obj.getColor());

        ConfigurationNode author = node.node(map("Author"));
        author.node(map("Name")).set(obj.getAuthorName());
        author.node(map("Url")).set(obj.getAuthorUrl());
        author.node(map("ImageUrl")).set(obj.getAuthorImageUrl());

        ConfigurationNode title = node.node(map("Title"));
        title.node(map("Text")).set(obj.getTitle());
        title.node(map("Url")).set(obj.getTitleUrl());

        node.node(map("Description")).set(obj.getDescription());

        List<DiscordMessageEmbed.Field> fields = obj.getFields();
        ConfigurationNode fieldsNode = node.node(map("Fields"));
        fieldsNode.setList(DiscordMessageEmbed.Field.class, fields.isEmpty() ? null : obj.getFields());

        node.node(map("ThumbnailUrl")).set(obj.getThumbnailUrl());
        node.node(map("ImageUrl")).set(obj.getImageUrl());

        ConfigurationNode footer = node.node(map("Footer"));
        footer.node(map("Text")).set(obj.getFooter());
        footer.node(map("ImageUrl")).set(obj.getFooterImageUrl());
    }

    public static class FieldSerializer implements TypeSerializer<DiscordMessageEmbed.Field> {

        private final NamingScheme namingScheme;

        public FieldSerializer(NamingScheme namingScheme) {
            this.namingScheme = namingScheme;
        }

        private String map(String option) {
            return namingScheme.coerce(option);
        }

        @Override
        public DiscordMessageEmbed.Field deserialize(Type type, ConfigurationNode node) {
            // v1 compat
            String footerString = node.getString();
            if (footerString != null) {
                if (footerString.contains(";")) {
                    String[] parts = footerString.split(";", 3);
                    if (parts.length < 2) {
                        return null;
                    }

                    boolean inline = parts.length < 3 || Boolean.parseBoolean(parts[2]);
                    return new DiscordMessageEmbed.Field(parts[0], parts[1], inline);
                } else {
                    boolean inline = Boolean.parseBoolean(footerString);
                    return new DiscordMessageEmbed.Field("\u200e", "\u200e", inline);
                }
            }

            return new DiscordMessageEmbed.Field(
                    node.node(map("Title")).getString(),
                    node.node(map("Value")).getString(),
                    node.node(map("Inline")).getBoolean()
            );
        }

        @Override
        public void serialize(Type type, DiscordMessageEmbed.@Nullable Field obj, ConfigurationNode node)
                throws SerializationException {
            if (obj == null) {
                node.set(null);
                return;
            }

            node.node(map("Title")).set(obj.getTitle());
            node.node(map("Value")).set(obj.getValue());
            node.node(map("Inline")).set(obj.isInline());
        }

    }
}
