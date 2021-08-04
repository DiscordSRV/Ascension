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

import com.discordsrv.api.discord.api.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SendableDiscordMessageSerializer implements TypeSerializer<SendableDiscordMessage.Builder> {

    @Override
    public SendableDiscordMessage.Builder deserialize(Type type, ConfigurationNode node)
            throws SerializationException {
        String contentOnly = node.getString();
        if (contentOnly != null) {
            return SendableDiscordMessage.builder()
                    .setContent(contentOnly);
        }

        SendableDiscordMessage.Builder builder = SendableDiscordMessage.builder();

        ConfigurationNode webhook = node.node("Webhook");
        String webhookUsername = webhook.node("Username").getString();
        if (webhook.node("Enabled").getBoolean(webhook.node("Enable").getBoolean(webhookUsername != null))) {
            builder.setWebhookUsername(webhookUsername);
            builder.setWebhookAvatarUrl(webhook.node("AvatarUrl").getString());
        }

        // v1 compat
        DiscordMessageEmbed.Builder singleEmbed = node.node("Embed").get(
                DiscordMessageEmbed.Builder.class);
        List<DiscordMessageEmbed.Builder> embedList = singleEmbed != null
                ? Collections.singletonList(singleEmbed) : Collections.emptyList();

        for (DiscordMessageEmbed.Builder embed : node.node("Embeds")
                .getList(DiscordMessageEmbed.Builder.class, embedList)) {
            builder.addEmbed(embed.build());
        }

        builder.setContent(node.node("Content").getString());
        return builder;
    }

    @Override
    public void serialize(Type type, SendableDiscordMessage.@Nullable Builder obj, ConfigurationNode node)
            throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }

        String webhookUsername = obj.getWebhookUsername();
        if (webhookUsername != null) {
            ConfigurationNode webhook = node.node("Webhook");
            webhook.node("Username").set(webhookUsername);
            webhook.node("AvatarUrl").set(obj.getWebhookAvatarUrl());
        }

        List<DiscordMessageEmbed.Builder> embedBuilders = new ArrayList<>();
        obj.getEmbeds().forEach(embed -> embedBuilders.add(embed.toBuilder()));
        node.setList(DiscordMessageEmbed.Builder.class, embedBuilders);

        node.node("Content").set(obj.getContent());
    }
}
