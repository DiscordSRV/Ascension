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

package com.discordsrv.common.dependency;

import com.discordsrv.common.scheduler.threadfactory.CountingForkJoinWorkerThreadFactory;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

/**
 * TODO: revamp
 * - run DiscordSRV#load() after DiscordSRV is initialized
 * - catch exceptions, so they don't go missing
 * - make the whenComplete stuff less janky
 */
public class InitialDependencyLoader {

    private CompletableFuture<?> completableFuture;
    protected ForkJoinPool taskPool;

    public InitialDependencyLoader(
            Path dataDirectory,
            String[] dependencyResources,
            ClasspathAppender classpathAppender
    ) throws IOException {
        this.taskPool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(), new CountingForkJoinWorkerThreadFactory("DiscordSRV initial dependency download #%s"), null, false);

        List<String> resourcePaths = new ArrayList<>(Arrays.asList(
                "dependencies/runtimeDownloadOnly-common.txt",
                "dependencies/runtimeDownloadApi-common.txt"
        ));
        resourcePaths.addAll(Arrays.asList(dependencyResources));

        DependencyLoader dependencyLoader = new DependencyLoader(
                dataDirectory,
                taskPool,
                resourcePaths.toArray(new String[0])
        );
        this.completableFuture = dependencyLoader.process(classpathAppender);
        whenComplete(() -> taskPool.shutdown());
    }

    public CompletableFuture<?> whenComplete(Runnable runnable) {
        return this.completableFuture = completableFuture.whenComplete((v, t) -> runnable.run());
    }
}
