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

import com.discordsrv.common.DiscordSRV;
import dev.vankka.dependencydownload.DependencyManager;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import dev.vankka.dependencydownload.repository.StandardRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public class DependencyLoader {

    public static DependencyLoader h2(DiscordSRV discordSRV) {
        return new DependencyLoader(discordSRV, new String[] {"dependencies/h2Driver.txt"});
    }

    public static DependencyLoader mysql(DiscordSRV discordSRV) {
        return new DependencyLoader(discordSRV, new String[] {"dependencies/mysqlDriver.txt"});
    }

    private final Path cacheDirectory;
    private final Executor executor;
    private final String[] dependencyResources;

    public DependencyLoader(DiscordSRV discordSRV, String[] dependencyResources) {
        this(
                discordSRV.dataDirectory(),
                discordSRV.scheduler().forkJoinPool(),
                dependencyResources
        );
    }

    public DependencyLoader(
            Path dataDirectory,
            Executor executor,
            String[] dependencyResources
    ) {
        this.cacheDirectory = dataDirectory.resolve("cache");
        this.executor = executor;
        this.dependencyResources = dependencyResources;
    }

    public CompletableFuture<Void> process(ClasspathAppender classpathAppender) throws IOException {
        DependencyManager dependencyManager = new DependencyManager(cacheDirectory);
        for (String dependencyResource : dependencyResources) {
            dependencyManager.loadFromResource(getClass().getClassLoader().getResource(dependencyResource));
        }
        return download(dependencyManager, classpathAppender);
    }

    private CompletableFuture<Void> download(DependencyManager manager,
                                             ClasspathAppender classpathAppender) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                manager.downloadAll(executor, Arrays.asList(
                        // TODO
                        new StandardRepository("https://repo1.maven.org/maven2"),
                        new StandardRepository("https://m2.dv8tion.net/releases"),
                        new StandardRepository("https://oss.sonatype.org/content/repositories/snapshots"),
                        new StandardRepository("https://s01.oss.sonatype.org/content/repositories/snapshots")
                )).join();
                manager.relocateAll(executor).join();
                manager.loadAll(executor, classpathAppender).join();

                future.complete(null);
            } catch (CompletionException e) {
                future.completeExceptionally(e.getCause());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
