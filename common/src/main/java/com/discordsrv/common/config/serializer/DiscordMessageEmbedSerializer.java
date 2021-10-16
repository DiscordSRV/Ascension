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

package com.discordsrv.common.config.serializer;

import com.discordsrv.api.color.Color;
import com.discordsrv.api.discord.api.entity.message.DiscordMessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Collections;

public class DiscordMessageEmbedSerializer implements TypeSerializer<DiscordMessageEmbed.Builder> {

    @Override
    public DiscordMessageEmbed.Builder deserialize(Type type, ConfigurationNode node) throws SerializationException {
        if (!node.node("Enabled").getBoolean(node.node("Enable").getBoolean(true))) {
            return null;
        }

        DiscordMessageEmbed.Builder builder = DiscordMessageEmbed.builder();

        Color color = node.node("Color").get(Color.class);
        builder.setColor(color != null ? color.rgb() : Role.DEFAULT_COLOR_RAW);

        ConfigurationNode author = node.node("Author");
        builder.setAuthor(
                author.node("Name").getString(),
                author.node("Url").getString(),
                author.node("ImageUrl").getString());

        ConfigurationNode title = node.node("Title");
        builder.setTitle(
                title.node("Text").getString(),
                title.node("Url").getString());

        builder.setDescription(node.node("Description").getString());
        for (DiscordMessageEmbed.Field field : node.getList(DiscordMessageEmbed.Field.class, Collections.emptyList())) {
            builder.addField(field);
        }

        builder.setThumbnailUrl(node.node("ThumbnailUrl").getString());
        builder.setImageUrl(node.node("ImageUrl").getString());

        // TODO: timestamp

        ConfigurationNode footer = node.node("Footer");
        builder.setFooter(
                footer.node("Text").getString(),
                footer.node("ImageUrl").getString(footer.node("IconUrl").getString()));

        return builder;
    }

    @Override
    public void serialize(Type type, DiscordMessageEmbed.@Nullable Builder obj, ConfigurationNode node)
            throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }

        node.node("Color").set(obj.getColor());

        ConfigurationNode author = node.node("Author");
        author.node("Name").set(obj.getAuthorName());
        author.node("Url").set(obj.getAuthorUrl());
        author.node("ImageUrl").set(obj.getAuthorImageUrl());

        ConfigurationNode title = node.node("Title");
        title.node("Text").set(obj.getTitle());
        title.node("Url").set(obj.getTitleUrl());

        node.node("Description").set(obj.getDescription());
        node.node("Fields").setList(DiscordMessageEmbed.Field.class, obj.getFields());

        node.node("ThumbnailUrl").set(obj.getThumbnailUrl());
        node.node("ImageUrl").set(obj.getImageUrl());

        ConfigurationNode footer = node.node("Footer");
        footer.node("Text").set(obj.getFooter());
        footer.node("ImageUrl").set(obj.getFooterImageUrl());
    }

    public static class FieldSerializer implements TypeSerializer<DiscordMessageEmbed.Field> {

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
                    node.node("Title").getString(),
                    node.node("Value").getString(),
                    node.node("Inline").getBoolean()
            );
        }

        @Override
        public void serialize(Type type, DiscordMessageEmbed.@Nullable Field obj, ConfigurationNode node)
                throws SerializationException {
            if (obj == null) {
                node.set(null);
                return;
            }

            node.node("Title").set(obj.getTitle());
            node.node("Value").set(obj.getValue());
            node.node("Inline").set(obj.isInline());
        }

    }
}
