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

package com.discordsrv.common.future.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class CompletableFutureUtil {

    private CompletableFutureUtil() {}

    /**
     * Same as {@link CompletableFuture#completedFuture(Object)} but for failing.
     */
    public static <T> CompletableFuture<T> failed(Throwable throwable) {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(throwable);
        return future;
    }

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<List<T>> combine(Collection<CompletableFuture<T>> futures) {
        return combine(futures.toArray(new CompletableFuture[0]));
    }

    @SafeVarargs
    public static <T> CompletableFuture<List<T>> combine(CompletableFuture<T>... futures) {
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        CompletableFuture.allOf(futures).whenComplete((v, t) -> {
            if (t != null) {
                future.completeExceptionally(t);
                return;
            }

            List<T> results = new ArrayList<>();
            for (CompletableFuture<T> aFuture : futures) {
                results.add(aFuture.join());
            }
            future.complete(results);
        });
        return future;
    }
}
