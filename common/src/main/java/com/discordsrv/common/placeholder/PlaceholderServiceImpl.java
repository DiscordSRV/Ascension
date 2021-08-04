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

package com.discordsrv.common.placeholder;

import com.discordsrv.api.event.events.placeholder.PlaceholderLookupEvent;
import com.discordsrv.api.placeholder.Placeholder;
import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.placeholder.provider.AnnotationPlaceholderProvider;
import com.discordsrv.common.placeholder.provider.PlaceholderProvider;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderServiceImpl implements PlaceholderService {

    private final DiscordSRV discordSRV;
    private final LoadingCache<Class<?>, Set<PlaceholderProvider>> classProviders;

    public PlaceholderServiceImpl(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.classProviders = discordSRV.caffeineBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .build(new ClassProviderLoader());
    }

    private static Set<Object> getArrayAsSet(Object[] array) {
        return array.length == 0
                ? Collections.emptySet()
                : new HashSet<>(Arrays.asList(array));
    }

    @Override
    public PlaceholderLookupResult lookupPlaceholder(String placeholder, Object... context) {
        return lookupPlaceholder(placeholder, getArrayAsSet(context));
    }

    @Override
    public PlaceholderLookupResult lookupPlaceholder(String placeholder, Set<Object> context) {
        for (Object o : context) {
            Set<PlaceholderProvider> providers = classProviders.get(o.getClass());
            if (providers == null) {
                continue;
            }

            for (PlaceholderProvider provider : providers) {
                PlaceholderLookupResult result = provider.lookup(placeholder, context);
                if (result.getType() != PlaceholderLookupResult.Type.UNKNOWN_PLACEHOLDER) {
                    return result;
                }
            }
        }

        // Only go through this if a placeholder couldn't be looked up from the context
        PlaceholderLookupEvent lookupEvent = new PlaceholderLookupEvent(placeholder, context);
        discordSRV.eventBus().publish(lookupEvent);

        return lookupEvent.isProcessed()
                ? lookupEvent.getResultFromProcessing()
                : PlaceholderLookupResult.UNKNOWN_PLACEHOLDER;
    }

    @Override
    public String replacePlaceholders(String input, Object... context) {
        return replacePlaceholders(input, getArrayAsSet(context));
    }

    @Override
    public String replacePlaceholders(String input, Set<Object> context) {
        return processReplacement(PATTERN, input, context);
    }

    private String processReplacement(Pattern pattern, String input, Set<Object> context) {
        Matcher matcher = pattern.matcher(input);

        String output = input;
        while (matcher.find()) {
            String placeholder = matcher.group(2);
            String originalPlaceholder = placeholder;

            // Recursive
            placeholder = processReplacement(RECURSIVE_PATTERN, placeholder, context);

            PlaceholderLookupResult result = lookupPlaceholder(placeholder, context);
            output = updateBasedOnResult(result, input, originalPlaceholder, matcher);
        }
        return output;
    }

    private String updateBasedOnResult(
            PlaceholderLookupResult result, String input, String originalPlaceholder, Matcher matcher) {
        String output = input;
        while (result != null) {
            PlaceholderLookupResult.Type type = result.getType();
            if (type == PlaceholderLookupResult.Type.UNKNOWN_PLACEHOLDER) {
                break;
            }

            boolean newLookup = false;
            String replacement = null;
            switch (type) {
                case SUCCESS:
                    replacement = result.getValue();
                    break;
                case DATA_NOT_AVAILABLE:
                    replacement = "Unavailable";
                    break;
                case LOOKUP_FAILED:
                    replacement = "Error";
                    break;
                case NEW_LOOKUP:
                    // prevent infinite recursion
                    if (result.getValue().equals(originalPlaceholder)) {
                        break;
                    }
                    result = lookupPlaceholder(result.getValue(), result.getExtras());
                    newLookup = true;
                    break;
            }
            if (replacement != null) {
                output = Pattern.compile(
                        matcher.group(1)
                                + originalPlaceholder
                                + matcher.group(3),
                        Pattern.LITERAL
                ).matcher(output).replaceFirst(replacement);
            }
            if (!newLookup) {
                break;
            }
        }
        return output;
    }

    private static class ClassProviderLoader implements CacheLoader<Class<?>, Set<PlaceholderProvider>> {

        @Override
        public @Nullable Set<PlaceholderProvider> load(@NonNull Class<?> key) {
            Set<PlaceholderProvider> providers = new HashSet<>();

            Class<?> currentClass = key;
            do {
                List<Class<?>> classes = new ArrayList<>(Arrays.asList(currentClass.getInterfaces()));
                classes.add(currentClass);

                for (Class<?> clazz : classes) {
                    for (Method method : clazz.getMethods()) {
                        Placeholder annotation = method.getAnnotation(Placeholder.class);
                        if (annotation == null) {
                            continue;
                        }

                        boolean isStatic = Modifier.isStatic(method.getModifiers());
                        providers.add(new AnnotationPlaceholderProvider(annotation, isStatic ? null : clazz, method));
                    }
                    for (Field field : clazz.getFields()) {
                        Placeholder annotation = field.getAnnotation(Placeholder.class);
                        if (annotation == null) {
                            continue;
                        }

                        boolean isStatic = Modifier.isStatic(field.getModifiers());
                        providers.add(new AnnotationPlaceholderProvider(annotation, isStatic ? null : clazz, field));
                    }
                }

                currentClass = currentClass.getSuperclass();
            } while (currentClass != null);

            return providers;
        }
    }
}
