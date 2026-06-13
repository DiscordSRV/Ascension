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

import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.discordsrv.api.configurate.DiscordSRVConfigurate.GENERATING_DEFAULT_CONFIG;
import static com.discordsrv.api.configurate.serializer.SerializerUtil.resolveNode;

public class SendableDiscordMessageSerializer implements TypeSerializer<SendableDiscordMessage.Builder> {

    private final boolean preferContentOnly;

    public SendableDiscordMessageSerializer(boolean preferContentOnly) {
        this.preferContentOnly = preferContentOnly;
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

        ConfigurationNode webhook = resolveNode(node, "webhook", "Webhook");
        String webhookUsername = resolveNode(webhook, "username", "Username").getString();
        if (resolveNode(webhook, "enabled", "Enabled", "Enable").getBoolean(webhookUsername != null)) {
            builder.setWebhookUsername(webhookUsername);
            builder.setWebhookAvatarUrl(resolveNode(webhook, "avatar-url", "AvatarUrl").getString());
        }

        // v1 compat
        DiscordMessageEmbed.Builder singleEmbed = resolveNode(node, "embed", "Embed").get(DiscordMessageEmbed.Builder.class);
        List<DiscordMessageEmbed.Builder> embedList = singleEmbed != null ? Collections.singletonList(singleEmbed) : Collections.emptyList();

        for (DiscordMessageEmbed.Builder embed : resolveNode(node, "embeds", "Embeds").getList(DiscordMessageEmbed.Builder.class, embedList)) {
            builder.addEmbed(embed.build());
        }

        builder.setContent(resolveNode(node, "content", "Content").getString());
        return builder;
    }

    @Override
    public void serialize(Type type, SendableDiscordMessage.@Nullable Builder obj, ConfigurationNode node)
            throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }
        if (GENERATING_DEFAULT_CONFIG.get()) {
            node.raw(obj);
            return;
        }

        if (obj.getWebhookUsername() == null && obj.getEmbeds().isEmpty() && preferContentOnly) {
            node.set(obj.getContent());
            return;
        }

        String webhookUsername = obj.getWebhookUsername();
        if (webhookUsername != null) {
            ConfigurationNode webhook = resolveNode(node, "webhook");
            resolveNode(webhook, "username").set(webhookUsername);
            resolveNode(webhook, "avatar-url").set(obj.getWebhookAvatarUrl());
        }

        List<DiscordMessageEmbed.Builder> embedBuilders = new ArrayList<>();
        obj.getEmbeds().forEach(embed -> embedBuilders.add(embed.toBuilder()));
        if (!embedBuilders.isEmpty()) {
            resolveNode(node, "embeds").setList(DiscordMessageEmbed.Builder.class, embedBuilders);
        }

        resolveNode(node, "content").set(obj.getContent());
    }
}
