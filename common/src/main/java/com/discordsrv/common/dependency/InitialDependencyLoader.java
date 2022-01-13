/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.scheduler.threadfactory.CountingForkJoinWorkerThreadFactory;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

public class InitialDependencyLoader {

    private final ForkJoinPool taskPool;
    private final CompletableFuture<?> completableFuture;
    private final List<Runnable> tasks = new CopyOnWriteArrayList<>();

    public InitialDependencyLoader(
            Logger logger,
            Path dataDirectory,
            String[] dependencyResources,
            ClasspathAppender classpathAppender
    ) throws IOException {
        this.taskPool = new ForkJoinPool(
                Runtime.getRuntime().availableProcessors(),
                new CountingForkJoinWorkerThreadFactory("DiscordSRV Initialization #%s"),
                null,
                false
        );

        List<String> resourcePaths = new ArrayList<>(Collections.singletonList(
                "dependencies/runtimeDownload-common.txt"
        ));
        resourcePaths.addAll(Arrays.asList(dependencyResources));

        DependencyLoader dependencyLoader = new DependencyLoader(
                dataDirectory,
                taskPool,
                resourcePaths.toArray(new String[0])
        );

        this.completableFuture = dependencyLoader.process(classpathAppender);
        completableFuture.whenComplete((v, t) -> {
            if (t != null) {
                logger.error("Error loading dependencies", t);
                return;
            }

            for (Runnable task : tasks) {
                try {
                    task.run();
                } catch (Throwable throwable) {
                    logger.error("Callback failed", throwable);
                }
            }

            taskPool.shutdown();
        });
    }

    /**
     * Joins the dependency download.
     */
    public void join() {
        completableFuture.join();
    }

    /**
     * This will run on the current thread if dependencies are already downloaded, otherwise will be added to a list.
     */
    public void runWhenComplete(Runnable runnable) {
        if (completableFuture.isDone()) {
            runnable.run();
            return;
        }

        tasks.add(runnable);
    }
}
