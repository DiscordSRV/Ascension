package com.discordsrv.common.config.configurate.serializer;

import com.discordsrv.common.config.helper.MinecraftMessage;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public class MinecraftMessageSerializer implements TypeSerializer<MinecraftMessage> {

    @Override
    public MinecraftMessage deserialize(Type type, ConfigurationNode node) throws SerializationException {
        return new MinecraftMessage(node.getString());
    }

    @Override
    public void serialize(Type type, @Nullable MinecraftMessage obj, ConfigurationNode node) throws SerializationException {
        if (obj == null) {
            node.set(null);
            return;
        }
        node.set(obj.rawFormat());
    }
}
