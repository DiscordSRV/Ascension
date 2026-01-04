/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.util;

import org.intellij.lang.annotations.Language;

public final class ReflectionUtil {

    private ReflectionUtil() {}

    public static boolean classExists(
            @Language(value = "JAVA", prefix = "class X{static{Class.forName(\"", suffix = "\")}}") String className
    ) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static boolean methodExists(
            @Language(value = "JAVA", prefix = "class X{static{Class.forName(\"", suffix = "\")}}") String className,
            String methodName,
            String... parameterTypeClassNames
    ) {
        try {
            Class<?> clazz = Class.forName(className);
            return methodExists(clazz, methodName, parameterTypeClassNames);
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static boolean methodExists(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            clazz.getDeclaredMethod(methodName, parameterTypes);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    public static boolean methodExists(Class<?> clazz, String methodName, String... parameterTypeClassNames) {
        try {
            Class<?>[] parameterTypes = new Class<?>[parameterTypeClassNames.length];
            for (int i = 0; i < parameterTypeClassNames.length; i++) {
                parameterTypes[i] = Class.forName(parameterTypeClassNames[i]);
            }
            clazz.getDeclaredMethod(methodName, parameterTypes);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
