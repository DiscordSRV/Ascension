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

package com.discordsrv.common.core.placeholder;

import com.discordsrv.api.events.placeholder.PlaceholderContextMappingEvent;
import com.discordsrv.api.events.placeholder.PlaceholderLookupEvent;
import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.api.placeholder.annotation.PlaceholderRemainder;
import com.discordsrv.api.placeholder.mapper.PlaceholderResultMapper;
import com.discordsrv.api.placeholder.provider.PlaceholderProvider;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.placeholder.provider.AnnotationPlaceholderProvider;
import com.discordsrv.common.helper.Timeout;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.time.Duration;
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
    private final List<Pair<Class<?>, String>> reLookups = new ArrayList<>();
    private final Set<Object> globalContext = new CopyOnWriteArraySet<>();
    private final Timeout errorLogTimeout = new Timeout(Duration.ofSeconds(20));

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

    public void addReLookup(Class<?> type, String reLookupAs) {
        this.reLookups.add(Pair.of(type, reLookupAs));
    }

    public void removeReLookup(Class<?> type) {
        this.reLookups.removeIf(pair -> pair.getKey() == type);
    }

    private static Set<Object> getArrayAsSet(Object[] array) {
        return array.length == 0
               ? Collections.emptySet()
               : new LinkedHashSet<>(Arrays.asList(array));
    }

    @Override
    public PlaceholderLookupResult lookupPlaceholder(@NotNull String placeholder, Object... context) {
        return lookupPlaceholder(placeholder, getArrayAsSet(context));
    }

    @Override
    public PlaceholderLookupResult lookupPlaceholder(@NotNull String placeholder, @NotNull Set<Object> lookupContexts) {
        Set<Object> contexts = new LinkedHashSet<>(lookupContexts);
        contexts.addAll(globalContext);
        contexts.removeIf(Objects::isNull);

        PlaceholderContextMappingEvent contextMappingEvent = new PlaceholderContextMappingEvent(contexts);
        discordSRV.eventBus().publish(contextMappingEvent);
        contexts = new LinkedHashSet<>(contextMappingEvent.getContexts());

        for (Object context : contexts) {
            if (context instanceof PlaceholderProvider) {
                PlaceholderLookupResult result = ((PlaceholderProvider) context).lookup(placeholder, contexts);
                result = mapResult(result, contexts);
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
                result = mapResult(result, contexts);
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

    private PlaceholderLookupResult mapResult(PlaceholderLookupResult result, Set<Object> contexts) {
        if (result.getType() == PlaceholderLookupResult.Type.RE_LOOKUP) {
            Object reLookupResult = result.getResult();
            boolean foundReLookup = false;
            for (Pair<Class<?>, String> reLookup : reLookups) {
                if (reLookup.getKey().isAssignableFrom(reLookupResult.getClass())) {
                    Set<Object> newContext = new LinkedHashSet<>();
                    newContext.add(reLookupResult);
                    newContext.addAll(contexts);

                    String newPlaceholder = reLookup.getValue() + result.getPlaceholder();
                    result = PlaceholderLookupResult.newLookup(newPlaceholder, newContext);
                    foundReLookup = true;
                    break;
                }
            }
            if (!foundReLookup) {
                result = PlaceholderLookupResult.UNKNOWN_PLACEHOLDER;
            }
        }
        return result;
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

            Object result = getResult(placeholder, context, matcher);
            output = updateContent(result, placeholder, matcher, output);
        }
        return output;
    }

    @Override
    public Object getResult(@NotNull Matcher matcher, @NotNull Set<Object> context) {
        if (matcher.groupCount() < 3) {
            throw new IllegalStateException("Matcher must have at least 3 groups");
        }

        String placeholder = getPlaceholder(matcher);
        return getResult(placeholder, context, matcher);
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
    public @NotNull CharSequence getResultAsCharSequence(@NotNull Matcher matcher, @NotNull Set<Object> context) {
        Object result = getResult(matcher, context);
        return getResultAsCharSequence(result);
    }

    @Override
    public @NotNull CharSequence getResultAsCharSequence(@Nullable Object result) {
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

    private String updateContent(Object result, String placeholder, Matcher matcher, String input) {
        CharSequence output = getResultAsCharSequence(result);
        return Pattern.compile(
                matcher.group(1) + placeholder + matcher.group(3),
                Pattern.LITERAL
        )
                .matcher(input)
                .replaceFirst(Matcher.quoteReplacement(output.toString()));
    }

    private Object getResult(String placeholder, Set<Object> context, Matcher matcher) {
        Map<String, AtomicInteger> preventInfiniteLoop = new HashMap<>();

        Object best = null;
        for (String singlePlaceholder : placeholder.split("(?<!\\\\)\\|")) {
            singlePlaceholder = getReplacement(RECURSIVE_PATTERN, singlePlaceholder, context);

            PlaceholderLookupResult result = lookupPlaceholder(singlePlaceholder, context);
            while (result != null) {
                PlaceholderLookupResult.Type type = result.getType();
                if (type == PlaceholderLookupResult.Type.UNKNOWN_PLACEHOLDER) {
                    break;
                }

                boolean newLookup = false;
                Object replacement = null;
                switch (type) {
                    case SUCCESS:
                        replacement = result.getResult();
                        if (replacement == null) {
                            replacement = getResultAsCharSequence(null);
                        }
                        if (StringUtils.isNotBlank(getResultAsCharSequence(replacement))) {
                            return replacement;
                        }
                        break;
                    case DATA_NOT_AVAILABLE:
                        replacement = "Unavailable";
                        break;
                    case LOOKUP_FAILED:
                        if (errorLogTimeout.checkAndUpdate()) {
                            logger.debug("Failed to resolve placeholder \"" + placeholder + "\"", result.getError());
                        } else {
                            logger.trace("Failed to resolve placeholder \"" + placeholder + "\"", result.getError());
                        }
                        replacement = "Error";
                        break;
                    case NEW_LOOKUP:
                        String newPlaceholder = result.getPlaceholder();

                        AtomicInteger infiniteLoop = preventInfiniteLoop.computeIfAbsent(newPlaceholder, key -> new AtomicInteger(0));
                        if (infiniteLoop.incrementAndGet() > 10) {
                            replacement = "Infinite Loop";
                            break;
                        }

                        result = lookupPlaceholder(newPlaceholder, result.getContext());
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
        public @Nullable Set<PlaceholderProvider> load(@NotNull Class<?> key) {
            return loadProviders(key, null);
        }
    }
}
