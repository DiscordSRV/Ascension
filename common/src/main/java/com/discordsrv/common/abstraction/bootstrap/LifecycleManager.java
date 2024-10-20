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

package com.discordsrv.common.abstraction.bootstrap;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.dependency.DependencyLoader;
import com.discordsrv.common.core.logging.Logger;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * A wrapper for loading in the initial dependencies, enabling and disabling {@link DiscordSRV}.
 */
public class LifecycleManager {

    private final Logger logger;
    private final ExecutorService taskPool;
    private final DependencyLoader dependencyLoader;
    private final CompletableFuture<?> completableFuture;

    public LifecycleManager(
            Logger logger,
            Path dataDirectory,
            String[] dependencyResources,
            ClasspathAppender classpathAppender
    ) throws IOException {
        this.logger = logger;
        this.taskPool = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "DiscordSRV Initialization"));

        List<String> resourcePaths = new ArrayList<>(Collections.singletonList(
                "dependencies/runtimeDownload-common.txt"
        ));
        resourcePaths.addAll(Arrays.asList(dependencyResources));

        this.dependencyLoader = new DependencyLoader(
                dataDirectory,
                taskPool,
                classpathAppender,
                resourcePaths.toArray(new String[0])
        );
        this.completableFuture = dependencyLoader.download();
        this.completableFuture.whenComplete((v, t) -> taskPool.shutdownNow());
    }

    public void loadAndEnable(Supplier<DiscordSRV> discordSRVSupplier) {
        if (relocateAndLoad()) {
            discordSRVSupplier.get().runEnable();
        }
    }

    private boolean relocateAndLoad() {
        try {
            completableFuture.get();
            dependencyLoader.relocateAndLoad(false).get();
            return true;
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            logger.error("Failed to download, relocate or load dependencies", e.getCause());
        }
        return false;
    }

    public void reload(DiscordSRV discordSRV) {
        if (discordSRV == null) {
            return;
        }
        discordSRV.runReload(DiscordSRVApi.ReloadFlag.DEFAULT_FLAGS, false);
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
            discordSRV.invokeDisable().get(/*15, TimeUnit.SECONDS*/);
        } catch (InterruptedException/* | TimeoutException*/ e) {
            logger.warning("Timed out/interrupted shutting down DiscordSRV");
        } catch (ExecutionException e) {
            logger.error("Failed to disable", e.getCause());
        }
    }

    public DependencyLoader getDependencyLoader() {
        return dependencyLoader;
    }
}
