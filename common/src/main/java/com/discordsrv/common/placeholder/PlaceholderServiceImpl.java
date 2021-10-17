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
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import com.discordsrv.api.placeholder.PlaceholderResultStringifier;
import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.placeholder.provider.AnnotationPlaceholderProvider;
import com.discordsrv.common.placeholder.provider.PlaceholderProvider;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderServiceImpl implements PlaceholderService {

    private final DiscordSRV discordSRV;
    private final LoadingCache<Class<?>, Set<PlaceholderProvider>> classProviders;
    private final Set<PlaceholderResultStringifier> stringifiers = new CopyOnWriteArraySet<>();

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
    public PlaceholderLookupResult lookupPlaceholder(@NotNull String placeholder, Object... context) {
        return lookupPlaceholder(placeholder, getArrayAsSet(context));
    }

    @Override
    public PlaceholderLookupResult lookupPlaceholder(@NotNull String placeholder, @NotNull Set<Object> context) {
        for (Object o : context) {
            if (o == null) {
                continue;
            }

            if (o instanceof PlaceholderProvider) {
                PlaceholderLookupResult result = ((PlaceholderProvider) o).lookup(placeholder, context);
                if (result.getType() != PlaceholderLookupResult.Type.UNKNOWN_PLACEHOLDER) {
                    return result;
                }
            }

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
    public String replacePlaceholders(@NotNull String input, Object... context) {
        return replacePlaceholders(input, getArrayAsSet(context));
    }

    @Override
    public void addResultStringifier(@NotNull PlaceholderResultStringifier resultStringifier) {
        stringifiers.add(resultStringifier);
    }

    @Override
    public void removeResultStringifier(@NotNull PlaceholderResultStringifier resultStringifier) {
        stringifiers.remove(resultStringifier);
    }

    @Override
    public String replacePlaceholders(@NotNull String input, @NotNull Set<Object> context) {
        return getReplacement(PATTERN, input, context);
    }

    private String getReplacement(Pattern pattern, String input, Set<Object> context) {
        Matcher matcher = pattern.matcher(input);

        String output = input;
        while (matcher.find()) {
            String placeholder = matcher.group(2);
            PlaceholderLookupResult result = resolve(placeholder, context);
            output = updateContent(result, placeholder, matcher, output);
        }
        return output;
    }

    @Override
    public Object getResult(@NotNull Matcher matcher, @NotNull Set<Object> context) {
        if (matcher.groupCount() < 3) {
            throw new IllegalStateException("Matcher must have at least 3 groups");
        }
        String placeholder = matcher.group(2);
        PlaceholderLookupResult result = resolve(matcher, context);
        return getResultRepresentation(result, placeholder, matcher);
    }

    @Override
    public String getResultAsString(@NotNull Matcher matcher, @NotNull Set<Object> context) {
        Object result = getResult(matcher, context);
        return getResultAsString(result);
    }

    private String getResultAsString(Object result) {
        if (result == null) {
            return "null";
        } else if (result instanceof String) {
            return (String) result;
        }

        String output = null;
        for (PlaceholderResultStringifier stringifier : stringifiers) {
            output = stringifier.convertPlaceholderResult(result);
            if (output != null) {
                break;
            }
        }
        if (output == null) {
            output = String.valueOf(result);
        }
        return output;
    }

    public PlaceholderLookupResult resolve(Matcher matcher, Set<Object> context) {
        return resolve(matcher.group(2), context);
    }

    private PlaceholderLookupResult resolve(String placeholder, Set<Object> context) {
        // Recursive
        placeholder = getReplacement(RECURSIVE_PATTERN, placeholder, context);

        return lookupPlaceholder(placeholder, context);
    }

    private String updateContent(
            PlaceholderLookupResult result, String placeholder, Matcher matcher, String input) {
        Object representation = getResultRepresentation(result, placeholder, matcher);

        String output = getResultAsString(representation);
        for (PlaceholderResultStringifier stringifier : stringifiers) {
            output = stringifier.convertPlaceholderResult(representation);
            if (output != null) {
                break;
            }
        }
        if (output == null) {
            output = String.valueOf(representation);
        }

        return Pattern.compile(
                matcher.group(1) + placeholder + matcher.group(3),
                Pattern.LITERAL
        )
                .matcher(input)
                .replaceFirst(output);
    }

    private Object getResultRepresentation(PlaceholderLookupResult result, String placeholder, Matcher matcher) {
        while (result != null) {
            PlaceholderLookupResult.Type type = result.getType();
            if (type == PlaceholderLookupResult.Type.UNKNOWN_PLACEHOLDER) {
                break;
            }

            boolean newLookup = false;
            Object replacement = null;
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
                    result = lookupPlaceholder((String) result.getValue(), result.getExtras());
                    newLookup = true;
                    break;
            }
            if (replacement != null) {
                return replacement;
            }
            if (!newLookup) {
                break;
            }
        }
        return matcher.group(1) + placeholder + matcher.group(3);
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
