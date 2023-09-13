package com.discordsrv.common.config.configurate.serializer;

import com.discordsrv.common.logging.Logger;
import io.leangen.geantyref.GenericTypeReflector;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EnumSerializer implements TypeSerializer<Enum<?>> {

    private final Logger logger;

    public EnumSerializer(Logger logger) {
        this.logger = logger;
    }

    @SuppressWarnings("unchecked") // Enum generic
    @Override
    public Enum<?> deserialize(Type type, ConfigurationNode node) throws SerializationException {
        Class<? extends Enum<?>> theEnum = (Class<? extends Enum<?>>) GenericTypeReflector.erase(type).asSubclass(Enum.class);
        String configValue = node.getString();
        if (configValue == null) {
            return null;
        }
        configValue = configValue.toLowerCase(Locale.ROOT);

        List<String> values = new ArrayList<>();
        for (Enum<?> constant : theEnum.getEnumConstants()) {
            String lower = constant.name().toLowerCase(Locale.ROOT);
            if (lower.equals(configValue)) {
                return constant;
            }
            values.add(lower);
        }

        logger.error(
                "Option \"" + node.key() + "\" "
                        + "has invalid value: \"" + configValue + "\", "
                        + "acceptable values: " + String.join(", ", values)
        );
        return null;
    }

    @Override
    public void serialize(Type type, @Nullable Enum<?> obj, ConfigurationNode node) throws SerializationException {
        node.raw(obj != null ? obj.name().toLowerCase(Locale.ROOT) : null);
    }
}
