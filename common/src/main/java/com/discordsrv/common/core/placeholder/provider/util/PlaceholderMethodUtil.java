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

package com.discordsrv.common.core.placeholder.provider.util;

import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import com.discordsrv.api.placeholder.annotation.PlaceholderRemainder;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public final class PlaceholderMethodUtil {

    private PlaceholderMethodUtil() {}

    public static Object lookup(Method method, Object instance, Set<Object> context, String remainder)
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

    private static @Nullable String getParameterValueFromRemainder(String remainder) {
        if (!remainder.startsWith(":")) {
            // Missing semicolon, empty value
            return null;
        }

        String parameterValue = remainder.substring(1);
        if (parameterValue.startsWith("'") && parameterValue.endsWith("'")) {
            parameterValue = parameterValue.substring(1, parameterValue.length() - 1);
        }
        return parameterValue;
    }

    private static void apply(Parameter[] parameters, BiConsumer<Parameter, Integer> parameterProcessor) {
        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            if (parameter == null) {
                continue;
            }
            parameterProcessor.accept(parameter, i);
        }
    }
}
