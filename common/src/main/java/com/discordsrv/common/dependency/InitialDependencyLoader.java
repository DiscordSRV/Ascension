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

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.scheduler.threadfactory.CountingForkJoinWorkerThreadFactory;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class InitialDependencyLoader {

    private final Logger logger;
    private final ForkJoinPool taskPool;
    private final CompletableFuture<?> completableFuture;

    public InitialDependencyLoader(
            Logger logger,
            Path dataDirectory,
            String[] dependencyResources,
            ClasspathAppender classpathAppender
    ) throws IOException {
        this.logger = logger;
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
        completableFuture.whenComplete((v, t) -> taskPool.shutdown());
    }

    public void loadAndEnable(Supplier<DiscordSRV> discordSRVSupplier) {
        load();
        enable(discordSRVSupplier);
    }

    public void load() {
        try {
            completableFuture.get();
        } catch (ExecutionException | InterruptedException e) {
            logger.error("Failed to download dependencies", e);
        }
    }

    public void enable(Supplier<DiscordSRV> discordSRVSupplier) {
        if (!completableFuture.isDone()) {
            return;
        }
        discordSRVSupplier.get().invokeEnable();
    }

    public void reload(DiscordSRV discordSRV) {
        if (discordSRV == null) {
            return;
        }
        discordSRV.invokeReload();
    }

    public void disable(DiscordSRV discordSRV) {
        if (!completableFuture.isDone()) {
            completableFuture.cancel(true);
            return;
        }

        if (discordSRV == null) {
            return;
        }

        try {
            discordSRV.invokeDisable().get(15, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            logger.warning("Timed out/interrupted shutting down DiscordSRV");
        } catch (ExecutionException ignored) {}
    }

}
