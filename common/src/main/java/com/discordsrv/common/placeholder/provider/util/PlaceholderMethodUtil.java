/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.placeholder.provider.util;

import com.discordsrv.api.placeholder.annotation.PlaceholderRemainder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;
import java.util.function.BiConsumer;

public final class PlaceholderMethodUtil {

    private PlaceholderMethodUtil() {}

    public static Object lookup(Method method, Object instance, Set<Object> context, String remainder)
            throws InvocationTargetException, IllegalAccessException {
        Parameter[] parameters = method.getParameters();
        Object[] parameterValues = new Object[parameters.length];

        apply(parameters, (parameter, i) -> {
            PlaceholderRemainder annotation = parameter.getAnnotation(PlaceholderRemainder.class);
            if (annotation != null) {
                parameters[i] = null;
                if (parameter.getType().isAssignableFrom(String.class)) {
                    parameterValues[i] = remainder;
                } else {
                    parameterValues[i] = null;
                }
            }
        });
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
