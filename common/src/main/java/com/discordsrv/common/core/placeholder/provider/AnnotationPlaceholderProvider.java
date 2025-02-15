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
import com.discordsrv.common.core.placeholder.provider.util.PlaceholderMethodUtil;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class AnnotationPlaceholderProvider implements PlaceholderProvider {

    private final Placeholder annotation;
    private final boolean startsWith;
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
        this.annotation = annotation;
        this.startsWith = !annotation.relookup().isEmpty() || remainderAnnotation != null;
        this.annotationPlaceholder = (prefixAnnotation != null ? prefixAnnotation.value() : "") + annotation.value();
        this.checkString = annotationPlaceholder + (remainderAnnotation != null && !remainderAnnotation.supportsNoValue() ? ":" : "");

        this.type = type;
        this.method = method;
        this.field = field;
    }

    @Override
    public @NotNull PlaceholderLookupResult lookup(@NotNull String placeholder, @NotNull Set<Object> context) {
        if (this.annotationPlaceholder.isEmpty()) {
            return PlaceholderLookupResult.UNKNOWN_PLACEHOLDER;
        }

        boolean correctPlaceholder = startsWith ? placeholder.startsWith(checkString) : placeholder.equals(checkString);
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
                result = PlaceholderMethodUtil.lookup(method, instance, context, remainder);
            }
        } catch (Throwable t) {
            return PlaceholderLookupResult.lookupFailed(t);
        }

        PlaceholderLookupResult lookupResult = result instanceof PlaceholderLookupResult
                ? (PlaceholderLookupResult) result
                : PlaceholderLookupResult.success(result);

        String reLookup = annotation.relookup();
        Object rawResult;
        if (!reLookup.isEmpty() && !remainder.isEmpty() && (rawResult = lookupResult.getValue()) != null) {
            Set<Object> newContext = new HashSet<>(context);
            newContext.add(rawResult);
            String newPlaceholder = reLookup + remainder;
            return PlaceholderLookupResult.newLookup(newPlaceholder, newContext);
        }

        return lookupResult;
    }
}
