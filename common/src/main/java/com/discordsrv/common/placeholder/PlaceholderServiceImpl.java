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

package com.discordsrv.common.placeholder;

import com.discordsrv.api.event.events.placeholder.PlaceholderLookupEvent;
import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderRemainder;
import com.discordsrv.api.placeholder.mapper.PlaceholderResultMapper;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.placeholder.provider.AnnotationPlaceholderProvider;
import com.discordsrv.common.placeholder.provider.PlaceholderProvider;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderServiceImpl implements PlaceholderService {

    private final DiscordSRV discordSRV;
    private final LoadingCache<Class<?>, Set<PlaceholderProvider>> classProviders;
    private final Set<PlaceholderResultMapper> mappers = new CopyOnWriteArraySet<>();
    private final Set<Object> globalContext = new CopyOnWriteArraySet<>();

    public PlaceholderServiceImpl(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.classProviders = discordSRV.caffeineBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .build(new ClassProviderLoader());
    }

    public void addGlobalContext(@NotNull Object context) {
        globalContext.add(context);
    }

    public void removeGlobalContext(@NotNull Object context) {
        globalContext.remove(context);
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
    public PlaceholderLookupResult lookupPlaceholder(@NotNull String placeholder, @NotNull Set<Object> lookupContexts) {
        Set<Object> contexts = new HashSet<>(lookupContexts);
        contexts.addAll(globalContext);
        for (Object context : contexts) {
            if (context == null) {
                continue;
            }

            if (context instanceof PlaceholderProvider) {
                PlaceholderLookupResult result = ((PlaceholderProvider) context).lookup(placeholder, contexts);
                if (result.getType() != PlaceholderLookupResult.Type.UNKNOWN_PLACEHOLDER) {
                    return result;
                }
            }

            Set<PlaceholderProvider> providers = classProviders
                    .get(context instanceof Class
                         ? (Class<?>) context
                         : context.getClass());
            if (providers == null) {
                continue;
            }

            for (PlaceholderProvider provider : providers) {
                PlaceholderLookupResult result = provider.lookup(placeholder, contexts);
                if (result.getType() != PlaceholderLookupResult.Type.UNKNOWN_PLACEHOLDER) {
                    return result;
                }
            }
        }

        // Only go through this if a placeholder couldn't be looked up from lookup/global contexts
        // API users are here as to not interfere with DiscordSRV's own placeholders
        PlaceholderLookupEvent lookupEvent = new PlaceholderLookupEvent(placeholder, contexts);
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
    public void addResultMapper(@NotNull PlaceholderResultMapper resultMapper) {
        mappers.add(resultMapper);
    }

    @Override
    public void removeResultMapper(@NotNull PlaceholderResultMapper resultMapper) {
        mappers.remove(resultMapper);
    }

    @Override
    public String replacePlaceholders(@NotNull String input, @NotNull Set<Object> context) {
        return getReplacement(PATTERN, input, context);
    }

    private String getReplacement(Pattern pattern, String input, Set<Object> context) {
        Matcher matcher = pattern.matcher(input);

        String output = input;
        while (matcher.find()) {
            String placeholder = getPlaceholder(matcher);
            List<PlaceholderLookupResult> results = resolve(placeholder, context);
            output = updateContent(results, placeholder, matcher, output);
        }
        return output;
    }

    @Override
    public Object getResult(@NotNull Matcher matcher, @NotNull Set<Object> context) {
        if (matcher.groupCount() < 3) {
            throw new IllegalStateException("Matcher must have at least 3 groups");
        }

        String placeholder = getPlaceholder(matcher);
        List<PlaceholderLookupResult> results = resolve(placeholder, context);
        return getResultRepresentation(results, placeholder, matcher);
    }

    private String getPlaceholder(Matcher matcher) {
        String placeholder = matcher.group(2);
        Pattern pattern = matcher.pattern();
        if (PATTERN.equals(pattern)) { // Remove escapes for %
            placeholder = placeholder.replace("\\%", "%");
        } else if (RECURSIVE_PATTERN.equals(pattern)) { // Remove escapes for { and }
            placeholder = placeholder.replaceAll("\\\\([{}])", "$1");
        }
        return placeholder;
    }

    @Override
    public @NotNull CharSequence getResultAsPlain(@NotNull Matcher matcher, @NotNull Set<Object> context) {
        Object result = getResult(matcher, context);
        return getResultAsPlain(result);
    }

    @Override
    public @NotNull CharSequence getResultAsPlain(@Nullable Object result) {
        if (result == null) {
            return "";
        } else if (result instanceof CharSequence) {
            return (CharSequence) result;
        }

        Object output = null;
        for (PlaceholderResultMapper mapper : mappers) {
            output = mapper.convertResult(result);
            if (output != null) {
                break;
            }
        }

        return output instanceof CharSequence ? (CharSequence) output : String.valueOf(output != null ? output : result);
    }

    private List<PlaceholderLookupResult> resolve(String placeholder, Set<Object> context) {
        // Recursive
        placeholder = getReplacement(RECURSIVE_PATTERN, placeholder, context);

        List<PlaceholderLookupResult> results = new ArrayList<>();
        for (String part : placeholder.split("(?<!\\\\)\\|")) {
            results.add(lookupPlaceholder(part, context));
        }

        return results;
    }

    private String updateContent(List<PlaceholderLookupResult> results, String placeholder, Matcher matcher, String input) {
        Object representation = getResultRepresentation(results, placeholder, matcher);

        CharSequence output = getResultAsPlain(representation);
        return Pattern.compile(
                matcher.group(1) + placeholder + matcher.group(3),
                Pattern.LITERAL
        )
                .matcher(input)
                .replaceFirst(output instanceof String ? (String) output : output.toString());
    }

    private Object getResultRepresentation(List<PlaceholderLookupResult> results, String placeholder, Matcher matcher) {
        Object best = null;
        for (PlaceholderLookupResult result : results) {
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
                        if (replacement == null) {
                            replacement = getResultAsPlain(null);
                        }
                        if (StringUtils.isNotBlank(getResultAsPlain(replacement))) {
                            return replacement;
                        }
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
                    best = replacement;
                }
                if (!newLookup) {
                    break;
                }
            }
        }

        return best != null
               ? best
               : matcher.group(1) + placeholder + matcher.group(3);
    }

    private static class ClassProviderLoader implements CacheLoader<Class<?>, Set<PlaceholderProvider>> {

        private Set<Class<?>> getAll(Class<?> clazz) {
            Set<Class<?>> classes = new LinkedHashSet<>();
            classes.add(clazz);

            for (Class<?> anInterface : clazz.getInterfaces()) {
                classes.addAll(getAll(anInterface));
            }

            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                classes.addAll(getAll(superClass));
            }

            return classes;
        }

        @Override
        public @Nullable Set<PlaceholderProvider> load(@NonNull Class<?> key) {
            Set<PlaceholderProvider> providers = new HashSet<>();

            Set<Class<?>> classes = getAll(key);
            for (Class<?> clazz : classes) {
                for (Method method : clazz.getMethods()) {
                    Placeholder annotation = method.getAnnotation(Placeholder.class);
                    if (annotation == null) {
                        continue;
                    }

                    boolean startsWith = !annotation.relookup().isEmpty();
                    if (!startsWith) {
                        for (Parameter parameter : method.getParameters()) {
                            if (parameter.getAnnotation(PlaceholderRemainder.class) != null) {
                                startsWith = true;
                                break;
                            }
                        }
                    }

                    boolean isStatic = Modifier.isStatic(method.getModifiers());
                    providers.add(new AnnotationPlaceholderProvider(annotation, isStatic ? null : clazz, startsWith, method));
                }
                for (Field field : clazz.getFields()) {
                    Placeholder annotation = field.getAnnotation(Placeholder.class);
                    if (annotation == null) {
                        continue;
                    }

                    boolean isStatic = Modifier.isStatic(field.getModifiers());
                    providers.add(new AnnotationPlaceholderProvider(annotation, isStatic ? null : clazz, !annotation.relookup().isEmpty(), field));
                }
            }

            return providers;
        }
    }
}
