/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.discordsrv.api.task;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@link CompletableFuture} wrapper, main difference is improved exception handling (no {@link CompletionException}, better exception handling methods).
 * @param <T> the type of the successful result
 */
public class Task<T> implements Future<T> {

    public static <T> Task<T> of(@NotNull CompletableFuture<T> future) {
        return new Task<>(future);
    }

    public static <T> Task<T> failed(@NotNull Throwable throwable) {
        return of(failedFuture(throwable));
    }

    public static <T> Task<T> completed(@Nullable T result) {
        return of(CompletableFuture.completedFuture(result));
    }

    @SuppressWarnings("unchecked")
    public static <T, C extends Collection<? extends Task<? extends T>>> Task<T> anyOf(@NotNull C tasks) {
        return anyOf(tasks.toArray(new Task[0]));
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T> Task<T> anyOf(@NotNull Task<? extends T>... tasks) {
        CompletableFuture<T>[] futures = new CompletableFuture[tasks.length];
        for (int i = 0; i < tasks.length; i++) {
            futures[i] = (CompletableFuture<T>) tasks[i].getFuture();
        }
        return of(CompletableFuture.anyOf(futures).thenApply(obj -> (T) obj));
    }

    @SuppressWarnings("unchecked")
    public static <T, C extends Collection<? extends Task<? extends T>>> Task<List<T>> allOf(@NotNull C tasks) {
        return allOf(tasks.toArray(new Task[0]));
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T> Task<List<T>> allOf(@NotNull Task<? extends T>... tasks) {
        CompletableFuture<T>[] futures = new CompletableFuture[tasks.length];
        for (int i = 0; i < tasks.length; i++) {
            futures[i] = (CompletableFuture<T>) tasks[i].getFuture();
        }
        return of(CompletableFuture.allOf(futures).thenApply(v -> {
            List<T> results = new ArrayList<>(futures.length);
            for (CompletableFuture<T> aFuture : futures) {
                results.add(aFuture.join());
            }
            return results;
        }));
    }

    private static <T> CompletableFuture<T> failedFuture(@NotNull Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    private final CompletableFuture<T> future;

    public Task() {
        this(new CompletableFuture<>());
    }

    private Task(@NotNull CompletableFuture<T> future) {
        this.future = future;
    }

    public CompletableFuture<T> getFuture() {
        return future;
    }

    public boolean isFailed() {
        return future.isCompletedExceptionally();
    }

    public T join() {
        return future.join();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean cancel(boolean interruptIfRunning) {
        return future.cancel(interruptIfRunning);
    }

    /**
     * {@link CompletableFuture#complete(Object)}.
     */
    public void complete(T result) {
        future.complete(result);
    }

    /**
     * {@link CompletableFuture#completeExceptionally(Throwable)}.
     */
    public void completeExceptionally(Throwable throwable) {
        future.completeExceptionally(throwable);
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public T get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }

    /**
     * {@link CompletableFuture#whenComplete(BiConsumer)} but only when successful.
     */
    public Task<T> whenSuccessful(@NotNull Consumer<T> successConsumer) {
        return whenComplete((result, throwable) -> {
            if (throwable == null) {
                successConsumer.accept(result);
            }
        });
    }

    /**
     * {@link CompletableFuture#whenComplete(BiConsumer)} but only when failed.
     */
    public Task<T> whenFailed(@NotNull Consumer<Throwable> failureConsumer) {
        return whenComplete((result, throwable) -> {
            if (throwable != null) {
                failureConsumer.accept(throwable);
            }
        });
    }

    /**
     * {@link CompletableFuture#whenComplete(BiConsumer)}.
     */
    public Task<T> whenComplete(@NotNull BiConsumer<T, Throwable> consumer) {
        return of(future.whenComplete((result, throwable) -> {
            while (throwable instanceof CompletionException) {
                throwable = throwable.getCause();
            }
            consumer.accept(result, throwable);
        }));
    }

    /**
     * {@link CompletableFuture#thenApply(Function)}.
     */
    public <U> Task<U> thenApply(@NotNull Function<T, U> mappingFunction) {
        return of(future.thenCompose(result -> map(mappingFunction, result)));
    }

    /**
     * {@link CompletableFuture#thenCompose(Function)} but for {@link Task}.
     * @see #thenCompose(Function)
     */
    public <U> Task<U> then(@NotNull Function<T, Task<U>> mappingFunction) {
        return of(future.thenCompose(result -> {
            try {
                return mappingFunction.apply(result).future;
            } catch (Throwable throwable) {
                return failedFuture(throwable);
            }
        }));
    }

    /**
     * {@link CompletableFuture#thenCompose(Function)}.
     * @see #then(Function)
     */
    public <U> Task<U> thenCompose(@NotNull Function<T, CompletionStage<U>> mappingFunction) {
        return of(future.thenCompose(result -> {
            try {
                return mappingFunction.apply(result);
            } catch (Throwable throwable) {
                return failedFuture(throwable);
            }
        }));
    }

    /**
     * {@link CompletableFuture#exceptionally(Function)}.
     */
    public Task<T> mapException(@NotNull Function<Throwable, T> mappingFunction) {
        return mapException(Throwable.class, mappingFunction);
    }

    /**
     * {@link CompletableFuture#exceptionally(Function)} but has a built-in exception type filter.
     */
    @SuppressWarnings("unchecked")
    public <E extends Throwable> Task<T> mapException(@NotNull Class<E> type, @NotNull Function<E, T> mappingFunction) {
        Throwable[] error = new Throwable[1];
        return of(future.exceptionally(throwable -> {
            while (throwable instanceof CompletionException) {
                throwable = throwable.getCause();
            }
            error[0] = throwable;
            return null;
        }).thenCompose(result -> {
            E throwable = (E) error[0];
            if (throwable == null) {
                return CompletableFuture.completedFuture(result);
            } else if (type.isAssignableFrom(throwable.getClass())) {
                return map(mappingFunction, throwable);
            }

            throw (RuntimeException) throwable;
        }));
    }

    private <I, O> CompletableFuture<O> map(Function<I, O> function, I result) {
        try {
            O mappedResult = function.apply(result);
            return CompletableFuture.completedFuture(mappedResult);
        } catch (Throwable throwable) {
            return failedFuture(throwable);
        }
    }
}
