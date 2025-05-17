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

package com.discordsrv.common.core.placeholder.provider;

import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.api.placeholder.annotation.PlaceholderRemainder;
import com.discordsrv.api.placeholder.provider.PlaceholderProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class AnnotationPlaceholderProvider implements PlaceholderProvider {

    private final boolean isRemainder;
    private final String annotationPlaceholder;
    private final String checkString;

    private final Class<?> type;
    private final Method method;
    private final Field field;

    public AnnotationPlaceholderProvider(Placeholder annotation, PlaceholderPrefix prefixAnnotation, PlaceholderRemainder remainderAnnotation, Class<?> type, Method method) {
        this(annotation, prefixAnnotation, remainderAnnotation, type, method, null);
    }

    public AnnotationPlaceholderProvider(Placeholder annotation, PlaceholderPrefix prefixAnnotation, Class<?> type, Field field) {
        this(annotation, prefixAnnotation, null, type, null, field);
    }

    private AnnotationPlaceholderProvider(Placeholder annotation, PlaceholderPrefix prefixAnnotation, PlaceholderRemainder remainderAnnotation, Class<?> type, Method method, Field field) {
        this.isRemainder = remainderAnnotation != null;
        this.annotationPlaceholder = (prefixAnnotation != null ? prefixAnnotation.value() : "") + annotation.value();
        this.checkString = annotationPlaceholder + (isRemainder && !remainderAnnotation.supportsNoValue() ? ":" : "");

        this.type = type;
        this.method = method;
        this.field = field;
    }

    public int priority() {
        // Longer placeholders will be checked first, to avoid conflicts with placeholders that start with the same string
        return -checkString.length();
    }

    @Override
    public @NotNull PlaceholderLookupResult lookup(@NotNull String placeholder, @NotNull Set<Object> context) {
        if (this.annotationPlaceholder.isEmpty()) {
            return PlaceholderLookupResult.UNKNOWN_PLACEHOLDER;
        }

        boolean perfectMatch = false;
        boolean correctPlaceholder;
        if (isRemainder) {
            correctPlaceholder = placeholder.startsWith(checkString);
        } else {
            perfectMatch = placeholder.equals(checkString);
            correctPlaceholder = perfectMatch
                    || placeholder.startsWith(checkString + '_')
                    || placeholder.startsWith(checkString + ':');
        }
        if (!correctPlaceholder) {
            return PlaceholderLookupResult.UNKNOWN_PLACEHOLDER;
        }

        Object instance = null;
        if (type != null) {
            for (Object o : context) {
                if (type.isAssignableFrom(o.getClass())) {
                    instance = o;
                }
            }
            if (instance == null) {
                return PlaceholderLookupResult.UNKNOWN_PLACEHOLDER;
            }
        }

        String remainder = placeholder.substring(this.annotationPlaceholder.length());

        Object result;
        try {
            if (field != null) {
                result = field.get(instance);
            } else {
                assert method != null;
                result = lookupUsingMethod(method, instance, context, remainder);
            }
        } catch (Throwable t) {
            return PlaceholderLookupResult.lookupFailed(t);
        }

        if (result instanceof PlaceholderLookupResult) {
            return (PlaceholderLookupResult) result;
        } else if (isRemainder || result == null || perfectMatch) {
            return PlaceholderLookupResult.success(result);
        }

        return PlaceholderLookupResult.reLookup(remainder, result);
    }

    private Object lookupUsingMethod(Method method, Object instance, Set<Object> context, String remainder)
            throws InvocationTargetException, IllegalAccessException {
        Parameter[] parameters = method.getParameters();
        Object[] parameterValues = new Object[parameters.length];
        AtomicBoolean failed = new AtomicBoolean(false);

        apply(parameters, (parameter, i) -> {
            PlaceholderRemainder remainderAnnotation = parameter.getAnnotation(PlaceholderRemainder.class);
            if (remainderAnnotation != null) {
                parameters[i] = null;

                if (parameter.getType().isAssignableFrom(String.class)) {
                    String parameterValue = getParameterValueFromRemainder(remainder);
                    if (parameterValue == null) {
                        if (!remainderAnnotation.supportsNoValue()) {
                            failed.set(true);
                            return;
                        } else {
                            parameterValue = "";
                        }
                    }

                    parameterValues[i] = parameterValue;
                } else {
                    parameterValues[i] = null;
                }
            }
        });
        if (failed.get()) {
            return PlaceholderLookupResult.UNKNOWN_PLACEHOLDER;
        }

        for (Object o : context) {
            Class<?> objectType = o.getClass();
            apply(parameters, (parameter, i) -> {
                if (parameter.getType().isAssignableFrom(objectType)) {
                    parameters[i] = null;
                    parameterValues[i] = o;
                }
            });
        }
        for (Object parameter : parameters) {
            if (parameter != null) {
                return null;
            }
        }

        return method.invoke(instance, parameterValues);
    }

    private @Nullable String getParameterValueFromRemainder(String remainder) {
        if (!remainder.startsWith(":")) {
            // Missing semicolon, empty value
            return null;
        }

        String parameterValue = remainder.substring(1);
        if (parameterValue.length() > 1 && parameterValue.startsWith("'") && parameterValue.endsWith("'")) {
            parameterValue = parameterValue.substring(1, parameterValue.length() - 1);
        }
        return parameterValue;
    }

    private void apply(Parameter[] parameters, BiConsumer<Parameter, Integer> parameterProcessor) {
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter == null) {
                continue;
            }
            parameterProcessor.accept(parameter, i);
        }
    }
}
