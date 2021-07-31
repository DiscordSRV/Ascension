package com.discordsrv.common.config.serializer;

import com.discordsrv.api.discord.api.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SendableDiscordMessageSerializer implements TypeSerializer<SendableDiscordMessage.Builder> {

    @Override
    public SendableDiscordMessage.Builder deserialize(Type type, ConfigurationNode node) throws SerializationException {
        String contentOnly = node.getString();
        if (contentOnly != null) {
            return SendableDiscordMessage.builder()
                    .setContent(contentOnly);
        }

        SendableDiscordMessage.Builder builder = SendableDiscordMessage.builder();

        ConfigurationNode webhook = node.node("Webhook");
        if (webhook.node("Enabled").getBoolean(false)) {
            builder.setWebhookUsername(webhook.node("Username").getString());
            builder.setWebhookAvatarUrl(webhook.node("AvatarUrl").getString());
        }

        List<DiscordMessageEmbed.Builder> embeds = node.node("Embeds").getList(DiscordMessageEmbed.Builder.class);
        if (embeds != null) {
            for (DiscordMessageEmbed.Builder embed : embeds) {
                builder.addEmbed(embed.build());
            }
        }

        builder.setContent(node.node("Content").getString());
        return builder;
    }

    @Override
    public void serialize(Type type, SendableDiscordMessage.@Nullable Builder obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }

        String webhookUsername = obj.getWebhookUsername();
        if (webhookUsername != null) {
            ConfigurationNode webhook = node.node("Webhook");
            webhook.node("Username").set(webhookUsername);
            webhook.node("AvatarUrl").set(Optional.ofNullable(obj.getWebhookAvatarUrl()).orElse(""));
        }

        List<DiscordMessageEmbed.Builder> embedBuilders = new ArrayList<>();
        obj.getEmbeds().forEach(embed -> embedBuilders.add(embed.toBuilder()));
        node.setList(DiscordMessageEmbed.Builder.class, embedBuilders);

        node.node("Content").set(obj.getContent());
    }
}
