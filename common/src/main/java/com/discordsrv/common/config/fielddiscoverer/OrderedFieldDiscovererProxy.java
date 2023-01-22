/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.config.fielddiscoverer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.FieldData;
import org.spongepowered.configurate.objectmapping.FieldDiscoverer;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.util.CheckedFunction;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.AnnotatedType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OrderedFieldDiscovererProxy<T> implements FieldDiscoverer<T> {

    private final FieldDiscoverer<T> fieldDiscoverer;
    private final Comparator<FieldCollectorData<T, ?>> order;

    public OrderedFieldDiscovererProxy(FieldDiscoverer<T> fieldDiscoverer, Comparator<FieldCollectorData<T, ?>> order) {
        this.fieldDiscoverer = fieldDiscoverer;
        this.order = order;
    }

    @Override
    public @Nullable <V> InstanceFactory<T> discover(AnnotatedType target, FieldCollector<T, V> collector) throws SerializationException {
        List<FieldCollectorData<T, V>> data = new ArrayList<>();
        FieldCollector<T, V> fieldCollector = (name, type, annotations, deserializer, serializer) ->
                data.add(new FieldCollectorData<>(name, type, annotations, deserializer, serializer));

        InstanceFactory<T> instanceFactory = fieldDiscoverer.discover(target, fieldCollector);
        if (instanceFactory == null) {
            return null;
        }

        data.sort(order);
        for (FieldCollectorData<T, V> field : data) {
            collector.accept(field.name, field.type, field.annotations, field.deserializer, field.serializer);
        }

        return instanceFactory;
    }

    public static class FieldCollectorData<T, V> {

        private final String name;
        private final AnnotatedType type;
        private final AnnotatedElement annotations;
        private final FieldData.Deserializer<T> deserializer;
        private final CheckedFunction<V, Object, Exception> serializer;

        public FieldCollectorData(String name, AnnotatedType type, AnnotatedElement annotations,
                                  FieldData.Deserializer<T> deserializer,
                                  CheckedFunction<V, Object, Exception> serializer) {
            this.name = name;
            this.type = type;
            this.annotations = annotations;
            this.deserializer = deserializer;
            this.serializer = serializer;
        }

        public String name() {
            return name;
        }

        public AnnotatedType type() {
            return type;
        }

        public AnnotatedElement annotations() {
            return annotations;
        }
    }
}
