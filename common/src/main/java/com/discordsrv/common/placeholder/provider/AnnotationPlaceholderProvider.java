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

package com.discordsrv.common.placeholder.provider;

import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.provider.PlaceholderProvider;
import com.discordsrv.common.placeholder.provider.util.PlaceholderMethodUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class AnnotationPlaceholderProvider implements PlaceholderProvider {

    private final Placeholder annotation;

    private final Class<?> type;
    private final Method method;
    private final boolean startsWith;
    private final Field field;

    public AnnotationPlaceholderProvider(Placeholder annotation, Class<?> type, boolean startsWith, Method method) {
        this.annotation = annotation;
        this.type = type;
        this.startsWith = startsWith;
        this.method = method;
        this.field = null;
    }

    public AnnotationPlaceholderProvider(Placeholder annotation, Class<?> type, boolean startsWith, Field field) {
        this.annotation = annotation;
        this.type = type;
        this.startsWith = startsWith;
        this.method = null;
        this.field = field;
    }

    @Override
    public @NotNull PlaceholderLookupResult lookup(@NotNull String placeholder, @NotNull Set<Object> context) {
        String annotationPlaceholder = annotation.value();
        if (annotationPlaceholder.isEmpty()
                || !(startsWith ? placeholder.startsWith(annotationPlaceholder) : placeholder.equals(annotationPlaceholder))
                || (type != null && context.isEmpty())) {
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

        String remainder = placeholder.replace(annotationPlaceholder, "");

        Object result;
        try {
            if (field != null) {
                result = field.get(instance);
            } else {
                assert method != null;
                result = PlaceholderMethodUtil.lookup(method, instance, context, remainder);
            }
        } catch (Throwable t) {
            return PlaceholderLookupResult.lookupFailed(t);
        }

        String reLookup = annotation.relookup();
        if (!reLookup.isEmpty()) {
            if (result == null) {
                return PlaceholderLookupResult.success(null);
            }

            Set<Object> newContext = new HashSet<>(context);
            newContext.add(result);
            String newPlaceholder = reLookup + remainder;
            return PlaceholderLookupResult.newLookup(newPlaceholder, newContext);
        }

        return result instanceof PlaceholderLookupResult
               ? (PlaceholderLookupResult) result
               : PlaceholderLookupResult.success(result);
    }
}
