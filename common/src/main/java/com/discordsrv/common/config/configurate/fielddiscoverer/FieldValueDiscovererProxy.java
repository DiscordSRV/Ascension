/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.config.configurate.fielddiscoverer;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.FieldDiscoverer;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.util.CheckedFunction;
import org.spongepowered.configurate.util.Types;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static io.leangen.geantyref.GenericTypeReflector.*;

/*
 * https://github.com/SpongePowered/Configurate/blob/c3d2105e0c03a0f6e0ae20ad74dbb4df9a83df36/core/src/main/java/org/spongepowered/configurate/objectmapping/ObjectFieldDiscoverer.java
 * Identical except uses the type of the value instead of the fields type.
 *
 * Configurate
 * Copyright (C) zml and Configurate contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class FieldValueDiscovererProxy implements FieldDiscoverer<Map<Field, Object>> {

    public static final FieldValueDiscovererProxy EMPTY_CONSTRUCTOR_INSTANCE = new FieldValueDiscovererProxy(type -> {
        try {
            final Constructor<?> constructor;
            constructor = erase(type.getType()).getDeclaredConstructor();
            constructor.setAccessible(true);
            return () -> {
                try {
                    return constructor.newInstance();
                } catch (final InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (final NoSuchMethodException e) {
            return null;
        }
    }, "Objects must have a zero-argument constructor to be able to create new instances", false);

    private final CheckedFunction<AnnotatedType, @Nullable Supplier<Object>, SerializationException> instanceFactory;
    private final String instanceUnavailableErrorMessage;
    private final boolean requiresInstanceCreation;

    FieldValueDiscovererProxy(
            final CheckedFunction<AnnotatedType, @Nullable Supplier<Object>, SerializationException> instanceFactory,
            final @Nullable String instanceUnavailableErrorMessage,
            final boolean requiresInstanceCreation
    ) {
        this.instanceFactory = instanceFactory;
        if (instanceUnavailableErrorMessage == null) {
            this.instanceUnavailableErrorMessage = "Unable to create instances for this type!";
        } else {
            this.instanceUnavailableErrorMessage = instanceUnavailableErrorMessage;
        }
        this.requiresInstanceCreation = requiresInstanceCreation;
    }

    @Override
    public <V> @Nullable InstanceFactory<Map<Field, Object>> discover(final AnnotatedType target,
                                                                      final FieldCollector<Map<Field, Object>, V> collector) throws SerializationException {
        final Class<?> clazz = erase(target.getType());
        if (clazz.isInterface()) {
            throw new SerializationException(target.getType(), "ObjectMapper can only work with concrete types");
        }

        final @Nullable Supplier<Object> maker = this.instanceFactory.apply(target);
        if (maker == null && this.requiresInstanceCreation) {
            return null;
        }

        Object instanceForCollection = maker != null ? maker.get() : null;

        AnnotatedType collectType = target;
        Class<?> collectClass = clazz;
        while (true) {
            collectFields(collectType, collector, instanceForCollection);
            collectClass = collectClass.getSuperclass();
            if (collectClass.equals(Object.class)) {
                break;
            }
            collectType = getExactSuperType(collectType, collectClass);
        }

        return new MutableInstanceFactory<Map<Field, Object>>() {

            @Override
            public Map<Field, Object> begin() {
                return new HashMap<>();
            }

            @Override
            public void complete(final Object instance, final Map<Field, Object> intermediate) throws SerializationException {
                for (final Map.Entry<Field, Object> entry : intermediate.entrySet()) {
                    try {
                        // Handle implicit field initialization by detecting any existing information in the object
                        if (entry.getValue() instanceof ImplicitProvider) {
                            final @Nullable Object implicit = ((ImplicitProvider) entry.getValue()).provider.get();
                            if (implicit != null) {
                                if (entry.getKey().get(instance) == null) {
                                    entry.getKey().set(instance, implicit);
                                }
                            }
                        } else {
                            entry.getKey().set(instance, entry.getValue());
                        }
                    } catch (final IllegalAccessException e) {
                        throw new SerializationException(target.getType(), e);
                    }
                }
            }

            @Override
            public Object complete(final Map<Field, Object> intermediate) throws SerializationException {
                final Object instance = maker == null ? null : maker.get();
                if (instance == null) {
                    throw new SerializationException(target.getType(), instanceUnavailableErrorMessage);
                }
                complete(instance, intermediate);
                return instance;
            }

            @Override
            public boolean canCreateInstances() {
                return maker != null;
            }

        };
    }

    private void collectFields(final AnnotatedType clazz, final FieldCollector<Map<Field, Object>, ?> fieldMaker, Object instanceForCollection) {
        for (final Field field : erase(clazz.getType()).getDeclaredFields()) {
            if ((field.getModifiers() & (Modifier.STATIC | Modifier.TRANSIENT)) != 0) {
                continue;
            }

            field.setAccessible(true);

            AnnotatedType fieldType = getFieldType(field, clazz);
            try {
                if (instanceForCollection != null) {
                    Object instance = field.get(instanceForCollection);
                    Class<?> type = instance != null ? instance.getClass() : null;
                    if (type != null && type.isAnnotationPresent(ConfigSerializable.class)) {
                        fieldType = annotate(type);
                    }
                }
            } catch (IllegalAccessException ignored) {}

            fieldMaker.accept(field.getName(), fieldType, Types.combinedAnnotations(fieldType, field),
                              (intermediate, val, implicitProvider) -> {
                                  if (val != null) {
                                      intermediate.put(field, val);
                                  } else {
                                      intermediate.put(field, new ImplicitProvider(implicitProvider));
                                  }
                              }, field::get);
        }
    }

    static class ImplicitProvider {

        final Supplier<Object> provider;

        ImplicitProvider(final Supplier<Object> provider) {
            this.provider = provider;
        }

    }
}

