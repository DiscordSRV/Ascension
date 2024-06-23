/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.api.placeholder.annotation.PlaceholderRemainder;
import com.discordsrv.api.placeholder.mapper.PlaceholderResultMapper;
import com.discordsrv.api.placeholder.provider.PlaceholderProvider;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.placeholder.provider.AnnotationPlaceholderProvider;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderServiceImpl implements PlaceholderService {

    private final DiscordSRV discordSRV;
    private final Logger logger;
    private final LoadingCache<Class<?>, Set<PlaceholderProvider>> classProviders;
    private final Set<PlaceholderResultMapper> mappers = new CopyOnWriteArraySet<>();
    private final Set<Object> globalContext = new CopyOnWriteArraySet<>();

    public PlaceholderServiceImpl(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "PLACEHOLDER_SERVICE");
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
        contexts.removeIf(Objects::isNull);
        for (Object context : contexts) {
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
                .replaceFirst(Matcher.quoteReplacement(output.toString()));
    }

    private Object getResultRepresentation(List<PlaceholderLookupResult> results, String placeholder, Matcher matcher) {
        Map<String, AtomicInteger> preventInfiniteLoop = new HashMap<>();

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
                        logger.trace("Lookup failed", result.getError());
                        replacement = "Error";
                        break;
                    case NEW_LOOKUP:
                        String placeholderKey = (String) result.getValue();

                        AtomicInteger infiniteLoop = preventInfiniteLoop.computeIfAbsent(placeholderKey, key -> new AtomicInteger(0));
                        if (infiniteLoop.incrementAndGet() > 10) {
                            replacement = "Infinite Loop";
                            break;
                        }

                        result = lookupPlaceholder(placeholderKey, result.getExtras());
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

        private Set<PlaceholderProvider> loadProviders(Class<?> clazz, PlaceholderPrefix prefix) {
            Set<PlaceholderProvider> providers = new LinkedHashSet<>();

            Class<?> currentClass = clazz;
            while (currentClass != null) {
                PlaceholderPrefix currentPrefix = currentClass.getAnnotation(PlaceholderPrefix.class);
                if (currentPrefix != null && prefix == null) {
                    prefix = currentPrefix;
                }
                PlaceholderPrefix usePrefix = (currentPrefix != null && currentPrefix.ignoreParents()) ? currentPrefix : prefix;

                for (Method method : clazz.getMethods()) {
                    if (!method.getDeclaringClass().equals(currentClass)) {
                        continue;
                    }

                    Placeholder annotation = method.getAnnotation(Placeholder.class);
                    if (annotation == null) {
                        continue;
                    }

                    PlaceholderRemainder remainder = null;
                    for (Parameter parameter : method.getParameters()) {
                        remainder = parameter.getAnnotation(PlaceholderRemainder.class);
                        if (remainder != null) {
                            break;
                        }
                    }

                    boolean isStatic = Modifier.isStatic(method.getModifiers());
                    providers.add(new AnnotationPlaceholderProvider(annotation, usePrefix, remainder, isStatic ? null : clazz, method));
                }
                for (Field field : clazz.getFields()) {
                    if (!field.getDeclaringClass().equals(currentClass)) {
                        continue;
                    }

                    Placeholder annotation = field.getAnnotation(Placeholder.class);
                    if (annotation == null) {
                        continue;
                    }

                    boolean isStatic = Modifier.isStatic(field.getModifiers());
                    providers.add(new AnnotationPlaceholderProvider(annotation, usePrefix, isStatic ? null : clazz, field));
                }

                for (Class<?> anInterface : currentClass.getInterfaces()) {
                    providers.addAll(loadProviders(anInterface, prefix));
                }

                currentClass = currentClass.getSuperclass();
            }

            return providers;
        }

        @Override
        public @Nullable Set<PlaceholderProvider> load(@NonNull Class<?> key) {
            return loadProviders(key, null);
        }
    }
}
