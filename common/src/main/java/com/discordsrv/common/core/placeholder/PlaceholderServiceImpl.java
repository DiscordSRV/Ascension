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
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.placeholder.provider.AnnotationPlaceholderProvider;
import com.discordsrv.common.helper.Timeout;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.commons.collections4.list.SetUniqueList;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlaceholderServiceImpl implements PlaceholderService {

    private static final Pattern ADDITIONAL_CONTEXT_PATTERN = Pattern.compile("^\\[([^]]+)]");

    private final DiscordSRV discordSRV;
    private final Logger logger;
    private final LoadingCache<Class<?>, List<AnnotationPlaceholderProvider>> classProviders;
    private final List<PlaceholderResultMapper> mappers = SetUniqueList.setUniqueList(new ArrayList<>());
    private final List<Pair<Class<?>, String>> reLookups = new ArrayList<>();
    private final List<Object> globalContext = contextList(0);
    private final Timeout errorLogTimeout = new Timeout(Duration.ofSeconds(20));

    public PlaceholderServiceImpl(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "PLACEHOLDER_SERVICE");
        this.classProviders = discordSRV.caffeineBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .build(new ClassProviderLoader());
    }

    public static SetUniqueList<Object> contextList(Collection<Object> contexts) {
        return SetUniqueList.setUniqueList(new ArrayList<>(contexts));
    }

    public static SetUniqueList<Object> contextList(int capacity) {
        return SetUniqueList.setUniqueList(new ArrayList<>(capacity));
    }

    public void addGlobalContext(@NotNull Object context) {
        synchronized (globalContext) {
            globalContext.add(context);
        }
    }

    public void removeGlobalContext(@NotNull Object context) {
        synchronized (globalContext) {
            globalContext.remove(context);
        }
    }

    public void addReLookup(Class<?> type, String reLookupAs) {
        this.reLookups.add(Pair.of(type, reLookupAs));
    }

    public void removeReLookup(Class<?> type) {
        this.reLookups.removeIf(pair -> pair.getKey() == type);
    }

    private static List<Object> getArrayAsList(Object[] array) {
        return array.length == 0
               ? Collections.emptyList()
               : new ArrayList<>(Arrays.asList(array));
    }

    @Override
    public PlaceholderLookupResult lookupPlaceholder(@NotNull String placeholder, Object... context) {
        return lookupPlaceholder(placeholder, getArrayAsList(context));
    }

    @Override
    public PlaceholderLookupResult lookupPlaceholder(@NotNull String placeholder, @NotNull Collection<Object> lookupContexts) {
        List<Object> contexts = contextList(lookupContexts.size() + globalContext.size());
        contexts.addAll(lookupContexts);
        contexts.removeIf(Objects::isNull);
        synchronized (globalContext) {
            contexts.addAll(globalContext);
        }

        Matcher additionaContextMatcher = ADDITIONAL_CONTEXT_PATTERN.matcher(placeholder);
        while (additionaContextMatcher.find()) {
            String contextPlaceholder = additionaContextMatcher.group(1);
            PlaceholderLookupResult result = lookupPlaceholder(contextPlaceholder, contexts);
            if (result.getResult() != null) {
                contexts.add(result.getResult());
            }

            placeholder = placeholder.substring(additionaContextMatcher.end());
        }

        PlaceholderContextMappingEvent contextMappingEvent = new PlaceholderContextMappingEvent(contexts);
        discordSRV.eventBus().publish(contextMappingEvent);
        contexts = contextList(contextMappingEvent.getContexts());

        for (Object context : contexts) {
            if (context instanceof PlaceholderProvider) {
                PlaceholderLookupResult result = getResultFromProvider((PlaceholderProvider) context, placeholder, contexts);
                if (result.getType() != PlaceholderLookupResult.Type.UNKNOWN_PLACEHOLDER) {
                    return result;
                }
            }

            List<AnnotationPlaceholderProvider> providers = classProviders
                    .get(context instanceof Class
                         ? (Class<?>) context
                         : context.getClass());
            if (providers == null) {
                continue;
            }

            for (PlaceholderProvider provider : providers) {
                PlaceholderLookupResult result = getResultFromProvider(provider, placeholder, contexts);
                if (result.getType() != PlaceholderLookupResult.Type.UNKNOWN_PLACEHOLDER) {
                    return result;
                }
            }
        }

        // Only go through this if a placeholder couldn't be looked up from lookup/global contexts
        // API users are here as to not interfere with DiscordSRV's own placeholders
        logger.trace("Requesting " + placeholder + " (" + contexts + ") from API");
        PlaceholderLookupEvent lookupEvent = new PlaceholderLookupEvent(placeholder, contexts);
        discordSRV.eventBus().publish(lookupEvent);

        return lookupEvent.isProcessed()
                ? lookupEvent.getResultFromProcessing()
                : PlaceholderLookupResult.UNKNOWN_PLACEHOLDER;
    }

    private PlaceholderLookupResult getResultFromProvider(PlaceholderProvider provider, String placeholder, List<Object> contexts) {
        PlaceholderLookupResult result = provider.lookup(placeholder, Collections.unmodifiableList(contexts));

        Object lookupResult = result.getResult();
        if (lookupResult instanceof Task) {
            Task<?> task = (Task<?>) lookupResult;
            if (task.isDone() || !discordSRV.scheduler().isServerThread()) {
                try {
                    lookupResult = task.get(5, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    return PlaceholderLookupResult.lookupFailed(e.getCause());
                } catch (TimeoutException e) {
                    return PlaceholderLookupResult.DATA_NOT_AVAILABLE;
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            } else {
                return PlaceholderLookupResult.DATA_NOT_AVAILABLE;
            }

            if (result.getType() == PlaceholderLookupResult.Type.SUCCESS) {
                return PlaceholderLookupResult.success(lookupResult);
            }
        }

        if (result.getType() == PlaceholderLookupResult.Type.RE_LOOKUP) {
            if (lookupResult == null) {
                // Re-lookup but there's no data to base the re-lookup off from: null success
                return PlaceholderLookupResult.success(null);
            }

            Class<?> reLookupType = lookupResult.getClass();

            String charSequenceReLookup = null;
            for (Pair<Class<?>, String> reLookup : reLookups) {
                if (CharSequence.class.isAssignableFrom(reLookup.getKey())) {
                    charSequenceReLookup = reLookup.getValue();
                }
                if (!reLookup.getKey().isAssignableFrom(reLookupType)) {
                    continue;
                }

                return reLookupResult(reLookup.getValue(), lookupResult, result, contexts);
            }
            if (result.getPlaceholder().startsWith(":") && !CharSequence.class.isAssignableFrom(reLookupType) && charSequenceReLookup != null) {
                return reLookupResult(charSequenceReLookup, convertReplacementToCharSequence(lookupResult), result, contexts);
            }
            return PlaceholderLookupResult.UNKNOWN_PLACEHOLDER;
        }
        return result;
    }

    private PlaceholderLookupResult reLookupResult(String reLookup, Object lookupResult, PlaceholderLookupResult result, List<Object> contexts) {
        List<Object> newContext = contextList(contexts.size() + 1);
        newContext.add(lookupResult);
        newContext.addAll(contexts);

        String newPlaceholder = reLookup + result.getPlaceholder();
        return PlaceholderLookupResult.newLookup(newPlaceholder, newContext);
    }

    @Override
    public String replacePlaceholders(@NotNull String input, Object... context) {
        return replacePlaceholders(input, getArrayAsList(context));
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
    public String replacePlaceholders(@NotNull String input, @NotNull Collection<Object> context) {
        return replacePlaceholders(PATTERN, input, context);
    }

    private String replacePlaceholders(Pattern pattern, String input, Collection<Object> context) {
        if (input.isEmpty()) {
            return input;
        }
        Matcher matcher = pattern.matcher(input);

        StringBuffer output = new StringBuffer(input.length() * 2);
        int lastEnd = -1;
        while (matcher.find()) {
            String placeholder = getPlaceholder(matcher);
            Object result = getReplacement(placeholder, context, matcher);
            String resultAsString = convertReplacementToCharSequence(result).toString();

            matcher.appendReplacement(output, Matcher.quoteReplacement(resultAsString));
            lastEnd = matcher.end();
        }
        if (lastEnd == -1) {
            return input;
        }

        output.append(input.substring(lastEnd));
        return output.toString();
    }

    @Override
    public Object getReplacement(@NotNull Matcher matcher, @NotNull Collection<Object> context) {
        if (matcher.groupCount() < 3) {
            throw new IllegalStateException("Matcher must have at least 3 groups");
        }

        String placeholder = getPlaceholder(matcher);
        return getReplacement(placeholder, context, matcher);
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
    public @NotNull CharSequence convertReplacementToCharSequence(@NotNull Matcher matcher, @NotNull List<Object> context) {
        Object result = getReplacement(matcher, context);
        return convertReplacementToCharSequence(result);
    }

    @Override
    public @NotNull CharSequence convertReplacementToCharSequence(@Nullable Object result) {
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

    private Object getReplacement(String placeholder, Collection<Object> context, Matcher matcher) {
        Map<String, AtomicInteger> preventInfiniteLoop = new HashMap<>();

        Object best = null;
        for (String singlePlaceholder : placeholder.split("(?<!\\\\)\\|")) {
            singlePlaceholder = replacePlaceholders(RECURSIVE_PATTERN, singlePlaceholder, context);

            PlaceholderLookupResult result = lookupPlaceholder(singlePlaceholder, context);
            while (result != null) {
                logger.trace(singlePlaceholder + ": " + result);
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
                            replacement = convertReplacementToCharSequence(null);
                        }
                        if (StringUtils.isNotBlank(convertReplacementToCharSequence(replacement))) {
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

    private static class ClassProviderLoader implements CacheLoader<Class<?>, List<AnnotationPlaceholderProvider>> {

        private List<AnnotationPlaceholderProvider> loadProviders(Class<?> clazz, PlaceholderPrefix prefix) {
            List<AnnotationPlaceholderProvider> providers = new ArrayList<>();

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
        public @Nullable List<AnnotationPlaceholderProvider> load(@NotNull Class<?> key) {
            List<AnnotationPlaceholderProvider> providers = loadProviders(key, null);
            providers.sort(Comparator.comparingInt(AnnotationPlaceholderProvider::priority));
            return providers;
        }
    }
}
