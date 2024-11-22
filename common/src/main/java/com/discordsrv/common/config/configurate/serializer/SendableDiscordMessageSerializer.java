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

import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.config.configurate.manager.abstraction.ConfigurateConfigManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.util.NamingScheme;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SendableDiscordMessageSerializer implements TypeSerializer<SendableDiscordMessage.Builder> {

    private final NamingScheme namingScheme;
    private final boolean preferContentOnly;

    public SendableDiscordMessageSerializer(NamingScheme namingScheme, boolean preferContentOnly) {
        this.namingScheme = namingScheme;
        this.preferContentOnly = preferContentOnly;
    }

    private String map(String option) {
        return namingScheme.coerce(option);
    }

    @Override
    public SendableDiscordMessage.Builder deserialize(Type type, ConfigurationNode node)
            throws SerializationException {
        Object raw = node.raw();
        if (raw instanceof SendableDiscordMessage.Builder) {
            return (SendableDiscordMessage.Builder) raw;
        }

        String contentOnly = node.getString();
        if (contentOnly != null) {
            return SendableDiscordMessage.builder()
                    .setContent(contentOnly);
        }

        SendableDiscordMessage.Builder builder = SendableDiscordMessage.builder();

        ConfigurationNode webhook = node.node(map("Webhook"));
        String webhookUsername = webhook.node(map("Username")).getString();
        if (webhook.node(map("Enabled")).getBoolean(
                webhook.node(map("Enable")).getBoolean(
                        webhookUsername != null))) {
            builder.setWebhookUsername(webhookUsername);
            builder.setWebhookAvatarUrl(webhook.node(map("AvatarUrl")).getString());
        }

        // v1 compat
        DiscordMessageEmbed.Builder singleEmbed = node.node(map("Embed")).get(
                DiscordMessageEmbed.Builder.class);
        List<DiscordMessageEmbed.Builder> embedList = singleEmbed != null
                ? Collections.singletonList(singleEmbed) : Collections.emptyList();

        for (DiscordMessageEmbed.Builder embed : node.node(map("Embeds"))
                .getList(DiscordMessageEmbed.Builder.class, embedList)) {
            builder.addEmbed(embed.build());
        }

        builder.setContent(node.node(map("Content")).getString());
        return builder;
    }

    @Override
    public void serialize(Type type, SendableDiscordMessage.@Nullable Builder obj, ConfigurationNode node)
            throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }
        if (ConfigurateConfigManager.DEFAULT_CONFIG.get()) {
            node.raw(obj);
            return;
        }

        if (obj.getWebhookUsername() == null && obj.getEmbeds().isEmpty() && preferContentOnly) {
            node.set(obj.getContent());
            return;
        }

        String webhookUsername = obj.getWebhookUsername();
        if (webhookUsername != null) {
            ConfigurationNode webhook = node.node(map("Webhook"));
            webhook.node(map("Username")).set(webhookUsername);
            webhook.node(map("AvatarUrl")).set(obj.getWebhookAvatarUrl());
        }

        List<DiscordMessageEmbed.Builder> embedBuilders = new ArrayList<>();
        obj.getEmbeds().forEach(embed -> embedBuilders.add(embed.toBuilder()));
        if (!embedBuilders.isEmpty()) {
            node.node(map("Embeds")).setList(DiscordMessageEmbed.Builder.class, embedBuilders);
        }

        node.node(map("Content")).set(obj.getContent());
    }
}
