/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.discordsrv.api.configurate.serializer;

import com.discordsrv.api.color.Color;
import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static com.discordsrv.api.configurate.DiscordSRVConfigurate.GENERATING_DEFAULT_CONFIG;
import static com.discordsrv.api.configurate.serializer.SerializerUtil.resolveNode;

/**
 * Serializer for Discord message embeds, also supporting v1 (yaml) and Discord API (json) formats.
 */
public class DiscordMessageEmbedSerializer implements TypeSerializer<DiscordMessageEmbed.Builder> {

    @Override
    public DiscordMessageEmbed.Builder deserialize(Type type, ConfigurationNode node) throws SerializationException {
        Object raw = node.raw();
        if (raw instanceof DiscordMessageEmbed.Builder) {
            return (DiscordMessageEmbed.Builder) raw;
        }
        if (!resolveNode(node, "enabled", /* v1 */ "Enabled", "Enable").getBoolean(true)) {
            return null;
        }

        DiscordMessageEmbed.Builder builder = DiscordMessageEmbed.builder();

        ConfigurationNode colorNode = resolveNode(
                node,
                "color",
                /* v1 */ "Color",
                /* Discord API */ "color"
        );
        Color color = colorNode.get(Color.class);
        if (color == null) {
            String unformattedColor = colorNode.getString();
            builder.setUnformattedColor(unformattedColor);
        } else {
            builder.setColor(color);
        }

        ConfigurationNode author = resolveNode(
                node,
                "author",
                /* v1 */ "Author",
                /* Discord API */ "author"
        );
        builder.setAuthor(
                resolveNode(
                        author,
                        "name",
                        /* v1 */ "Name",
                        /* Discord API */ "name"
                ).getString(),
                resolveNode(
                        author,
                        "url",
                        /* v1 */ "Url",
                        /* Discord API */ "url"
                ).getString(),
                resolveNode(
                        author,
                        "image-url",
                        /* v1 */ "ImageUrl",
                        /* Discord API */ "icon_url"
                ).getString()
        );

        ConfigurationNode title = resolveNode(node, "title", /* v1 */ "Title");
        if (!title.virtual()) {
            builder.setTitle(
                    resolveNode(title, "text", /* v1 */ "Text").getString(),
                    resolveNode(title, "url", /* v1 */ "Url").getString()
            );
        } else {
            // Discord API
            builder.setTitle(
                    node.node("title").getString(),
                    node.node("url").getString()
            );
        }

        builder.setDescription(resolveNode(
                node,
                "description",
                /* v1 */ "Description",
                /* Discord API */ "description"
        ).getString());

        ConfigurationNode fieldsNode = resolveNode(
                node,
                "fields",
                /* v1 */ "Fields",
                /* Discord API */ "fields"
        );
        for (DiscordMessageEmbed.Field field : fieldsNode.getList(DiscordMessageEmbed.Field.class, Collections.emptyList())) {
            builder.addField(field);
        }

        ConfigurationNode thumbnailUrl = resolveNode(node, "thumbnail-url", /* v1 */ "ThumbnailUrl");
        if (thumbnailUrl.virtual()) {
            // Discord API
            thumbnailUrl = node.node("thumbnail", "url");
        }
        builder.setThumbnailUrl(thumbnailUrl.getString());

        ConfigurationNode imageUrl = resolveNode(node, "image-url", /* v1 */ "ImageUrl");
        if (imageUrl.virtual()) {
            // Discord API
            imageUrl = node.node("image", "url");
        }
        builder.setImageUrl(imageUrl.getString());

        ConfigurationNode timestampNode = resolveNode(
                node,
                "timestamp",
                /* v1 */ "Timestamp",
                /* Discord API */ "timestamp"
        );
        OffsetDateTime timestamp = timestampNode.get(OffsetDateTime.class);
        if (timestamp == null) {
            String unformattedTimestamp = timestampNode.getString();
            builder.setUnformattedTimestamp(unformattedTimestamp);
        } else {
            builder.setTimestamp(timestamp);
        }

        ConfigurationNode footer = resolveNode(
                node,
                "footer",
                /* v1 */ "Footer",
                /* Discord API */ "footer"
        );
        builder.setFooter(
                resolveNode(
                        footer,
                        "text",
                        /* v1 */ "Text",
                        /* Discord API */ "text"
                ).getString(),
                resolveNode(
                        footer,
                        "image-url",
                        /* v1 */ "IconUrl",
                        /* Discord API */ "icon_url"
                ).getString("")
        );

        return builder;
    }

    @Override
    public void serialize(Type type, DiscordMessageEmbed.@Nullable Builder obj, ConfigurationNode node)
            throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }
        if (GENERATING_DEFAULT_CONFIG.get()) {
            node.raw(obj);
            return;
        }

        String unformattedColor = obj.getUnformattedColor();
        if (unformattedColor != null) {
            resolveNode(node, "color").set(unformattedColor);
        } else {
            resolveNode(node, "color").set(obj.getColor());
        }

        ConfigurationNode author = resolveNode(node, "author");
        resolveNode(author, "name").set(obj.getAuthorName());
        resolveNode(author, "url").set(obj.getAuthorUrl());
        resolveNode(author, "image-url").set(obj.getAuthorImageUrl());

        ConfigurationNode title = resolveNode(node, "title");
        resolveNode(title, "text").set(obj.getTitle());
        resolveNode(title, "url").set(obj.getTitleUrl());

        resolveNode(node, "description").set(obj.getDescription());

        List<DiscordMessageEmbed.Field> fields = obj.getFields();
        ConfigurationNode fieldsNode = resolveNode(node, "fields");
        fieldsNode.setList(DiscordMessageEmbed.Field.class, fields.isEmpty() ? null : obj.getFields());

        resolveNode(node, "thumbnail-url").set(obj.getThumbnailUrl());
        resolveNode(node, "image-url").set(obj.getImageUrl());

        String unformattedTimestamp = obj.getUnformattedTimestamp();
        if (unformattedTimestamp != null) {
            resolveNode(node, "timestamp").set(unformattedTimestamp);
        } else {
            resolveNode(node, "timestamp").set(obj.getTimestamp());
        }

        ConfigurationNode footer = resolveNode(node, "footer");
        resolveNode(footer, "text").set(obj.getFooter());
        resolveNode(footer, "image-url").set(obj.getFooterImageUrl());
    }

    public static class FieldSerializer implements TypeSerializer<DiscordMessageEmbed.Field> {

        @Override
        public DiscordMessageEmbed.Field deserialize(Type type, ConfigurationNode node) {
            Object raw = node.raw();
            if (raw instanceof DiscordMessageEmbed.Field) {
                return (DiscordMessageEmbed.Field) raw;
            }

            // v1
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
                    resolveNode(node, "title", /* Discord API */ "name").getString(),
                    resolveNode(node, "value", /* Discord API */ "value").getString(),
                    resolveNode(node, "inline", /* Discord API */ "inline").getBoolean()
            );
        }

        @Override
        public void serialize(Type type, DiscordMessageEmbed.@Nullable Field obj, ConfigurationNode node)
                throws SerializationException {
            if (obj == null) {
                node.set(null);
                return;
            }
            if (GENERATING_DEFAULT_CONFIG.get()) {
                node.raw(obj);
                return;
            }

            resolveNode(node, "title").set(obj.getTitle());
            resolveNode(node, "value").set(obj.getValue());
            resolveNode(node, "inline").set(obj.isInline());
        }

    }
}
