/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

public final class PlaceholderMethodUtil {

    private PlaceholderMethodUtil() {}

    public static Object lookup(Method method, Object instance, Set<Object> context)
            throws InvocationTargetException, IllegalAccessException {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];

        for (Object o : context) {
            Class<?> objectType = o.getClass();
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                if (parameterType == null) {
                    continue;
                }

                if (parameterType.isAssignableFrom(objectType)) {
                    parameters[i] = o;
                    parameterTypes[i] = null;
                }
            }
        }
        for (Class<?> parameterType : parameterTypes) {
            if (parameterType != null) {
                return null;
            }
        }

        return method.invoke(instance, parameters);
    }
}
