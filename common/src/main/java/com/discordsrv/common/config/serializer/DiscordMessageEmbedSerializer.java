package com.discordsrv.common.config.serializer;

import com.discordsrv.api.discord.api.entity.message.DiscordMessageEmbed;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public class DiscordMessageEmbedSerializer implements TypeSerializer<DiscordMessageEmbed.Builder> {

    @Override
    public DiscordMessageEmbed.Builder deserialize(Type type, ConfigurationNode node) throws SerializationException {
        // TODO
        return null;
    }

    @Override
    public void serialize(Type type, DiscordMessageEmbed.@Nullable Builder obj, ConfigurationNode node) throws SerializationException {
        // TODO
    }
}
