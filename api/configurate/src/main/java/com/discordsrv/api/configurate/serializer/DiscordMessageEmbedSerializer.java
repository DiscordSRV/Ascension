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

public class DiscordMessageEmbedSerializer implements TypeSerializer<DiscordMessageEmbed.Builder> {

    @Override
    public DiscordMessageEmbed.Builder deserialize(Type type, ConfigurationNode node) throws SerializationException {
        Object raw = node.raw();
        if (raw instanceof DiscordMessageEmbed.Builder) {
            return (DiscordMessageEmbed.Builder) raw;
        }
        if (!resolveNode(node, "enabled", "Enabled", "Enable").getBoolean(true)) {
            return null;
        }

        DiscordMessageEmbed.Builder builder = DiscordMessageEmbed.builder();

        ConfigurationNode colorNode = resolveNode(node, "color", "Color");
        Color color = colorNode.get(Color.class);
        if (color == null) {
            String unformattedColor = colorNode.getString();
            builder.setUnformattedColor(unformattedColor);
        } else {
            builder.setColor(color);
        }

        ConfigurationNode author = resolveNode(node, "author", "Author");
        builder.setAuthor(
                resolveNode(author, "name", "Name").getString(),
                resolveNode(author, "url", "Url").getString(),
                resolveNode(author, "image-url", "ImageUrl").getString()
        );

        ConfigurationNode title = resolveNode(node, "title", "Title");
        builder.setTitle(
                resolveNode(title, "text", "Text").getString(),
                resolveNode(title, "url", "Url").getString()
        );

        builder.setDescription(resolveNode(node, "description", "Description").getString());
        for (DiscordMessageEmbed.Field field : resolveNode(node, "fields", "Fields").getList(DiscordMessageEmbed.Field.class, Collections.emptyList())) {
            builder.addField(field);
        }

        builder.setThumbnailUrl(resolveNode(node, "thumbnail-url", "ThumbnailUrl").getString());
        builder.setImageUrl(resolveNode(node, "image-url", "ImageUrl").getString());

        ConfigurationNode timestampNode = resolveNode(node, "timestamp", "Timestamp");
        OffsetDateTime timestamp = timestampNode.get(OffsetDateTime.class);
        if (timestamp == null) {
            String unformattedTimestamp = timestampNode.getString();
            builder.setUnformattedTimestamp(unformattedTimestamp);
        } else {
            builder.setTimestamp(timestamp);
        }

        ConfigurationNode footer = resolveNode(node, "footer", "Footer");
        builder.setFooter(
                resolveNode(footer, "text", "Text").getString(),
                resolveNode(footer, "image-url", "ImageUrl", "IconUrl").getString("")
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
                    resolveNode(node, "title", "Title").getString(),
                    resolveNode(node, "value", "Value").getString(),
                    resolveNode(node, "inline", "Inline").getBoolean()
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
