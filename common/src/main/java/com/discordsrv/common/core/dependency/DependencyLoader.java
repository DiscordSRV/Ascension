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

package com.discordsrv.common.core.dependency;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.logging.Logger;
import dev.vankka.dependencydownload.DependencyManager;
import dev.vankka.dependencydownload.classloader.IsolatedClassLoader;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import dev.vankka.dependencydownload.path.DependencyPathProvider;
import dev.vankka.dependencydownload.repository.MavenRepository;
import dev.vankka.dependencydownload.repository.Repository;
import dev.vankka.dependencydownload.resource.DependencyDownloadResource;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class DependencyLoader {

    private static final List<Repository> REPOSITORIES = Arrays.asList(
            // TODO
            new MavenRepository("https://repo1.maven.org/maven2"),
            new MavenRepository("https://oss.sonatype.org/content/repositories/snapshots"),
            new MavenRepository("https://s01.oss.sonatype.org/content/repositories/snapshots"),
            new MavenRepository("https://nexus.scarsz.me/content/groups/public")
    );

    public static Path resolvePath(Path dataDirectory) {
        return dataDirectory.resolve("cache");
    }

    public static DependencyDownloadResource loadResource(String resourceName) throws IOException {
        URL resource = DependencyLoader.class.getClassLoader().getResource(resourceName);
        if (resource == null) {
            throw new IllegalArgumentException("Could not find resource with: " + resourceName);
        }

        return DependencyDownloadResource.parse(resource);
    }

    private static DependencyManager makeManager(Logger logger, Path dataDirectory, String[] resources) throws IOException {
        DependencyManager dependencyManager = new DependencyManager(
                DependencyPathProvider.directory(resolvePath(dataDirectory)),
                new DependencyDownloadLogger(logger)
        );

        for (String dependencyResource : resources) {
            dependencyManager.loadResource(loadResource(dependencyResource));
        }

        return dependencyManager;
    }

    private final DependencyManager dependencyManager;
    private final Executor executor;
    private final ClasspathAppender classpathAppender;

    public DependencyLoader(Logger logger, Path dataDirectory, Executor executor, ClasspathAppender classpathAppender, String[] paths) throws IOException {
        this(executor, classpathAppender, makeManager(logger, dataDirectory, paths));
    }

    public DependencyLoader(DiscordSRV discordSRV, DependencyManager dependencyManager) {
        this(discordSRV.scheduler().executor(), discordSRV.bootstrap().classpathAppender(), dependencyManager);
    }

    public DependencyLoader(Executor executor, ClasspathAppender classpathAppender, DependencyManager dependencyManager) {
        this.executor = executor;
        this.classpathAppender = classpathAppender;
        this.dependencyManager = dependencyManager;
    }

    public DependencyManager getDependencyManager() {
        return dependencyManager;
    }

    public IsolatedClassLoader intoIsolated() throws IOException {
        IsolatedClassLoader classLoader = new IsolatedClassLoader();
        downloadRelocateAndLoad(classLoader).join();
        return classLoader;
    }

    public CompletableFuture<Void> downloadRelocateAndLoad() {
        return downloadRelocateAndLoad(classpathAppender);
    }

    private CompletableFuture<Void> downloadRelocateAndLoad(ClasspathAppender appender) {
        return dependencyManager.downloadAll(executor, REPOSITORIES)
                .thenCompose(v -> dependencyManager.relocateAll(executor))
                .thenCompose(v -> dependencyManager.loadAll(executor, appender));
    }
}
