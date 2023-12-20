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

package com.discordsrv.common.dependency;

import com.discordsrv.common.DiscordSRV;
import dev.vankka.dependencydownload.DependencyManager;
import dev.vankka.dependencydownload.classloader.IsolatedClassLoader;
import dev.vankka.dependencydownload.classpath.ClasspathAppender;
import dev.vankka.dependencydownload.repository.Repository;
import dev.vankka.dependencydownload.repository.StandardRepository;

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
            new StandardRepository("https://repo1.maven.org/maven2"),
            new StandardRepository("https://oss.sonatype.org/content/repositories/snapshots"),
            new StandardRepository("https://s01.oss.sonatype.org/content/repositories/snapshots"),
            new StandardRepository("https://nexus.scarsz.me/content/groups/public")
    );

    public static Path resolvePath(Path dataDirectory) {
        return dataDirectory.resolve("cache");
    }

    public static DependencyManager fromPaths(Path dataDirectory, String[] resources) throws IOException {
        DependencyManager dependencyManager = new DependencyManager(resolvePath(dataDirectory));
        for (String dependencyResource : resources) {
            URL resource = DependencyLoader.class.getClassLoader().getResource(dependencyResource);
            if (resource == null) {
                throw new IllegalArgumentException("Could not find resource with: " + dependencyResource);
            }
            dependencyManager.loadFromResource(resource);
        }

        return dependencyManager;
    }

    private final DependencyManager dependencyManager;
    private final Executor executor;
    private final ClasspathAppender classpathAppender;


    public DependencyLoader(DiscordSRV discordSRV, String[] paths) throws IOException {
        this(discordSRV, fromPaths(discordSRV.dataDirectory(), paths));
    }

    public DependencyLoader(Path dataDirectory, Executor executor, ClasspathAppender classpathAppender, String[] paths) throws IOException {
        this(executor, classpathAppender, fromPaths(dataDirectory, paths));
    }

    public DependencyLoader(DiscordSRV discordSRV, DependencyManager dependencyManager) {
        this(discordSRV.scheduler().executor(), discordSRV.bootstrap().classpathAppender(), dependencyManager);
    }

    public DependencyLoader(Executor executor, ClasspathAppender classpathAppender, DependencyManager dependencyManager) {
        this.dependencyManager = dependencyManager;
        this.executor = executor;
        this.classpathAppender = classpathAppender;
    }

    public DependencyManager getDependencyManager() {
        return dependencyManager;
    }

    public IsolatedClassLoader loadIntoIsolated() throws IOException {
        IsolatedClassLoader classLoader = new IsolatedClassLoader();
        download(classLoader).join();
        return classLoader;
    }

    public CompletableFuture<Void> download() {
        return download(classpathAppender);
    }

    private CompletableFuture<Void> download(ClasspathAppender appender) {
        return dependencyManager.downloadAll(executor, REPOSITORIES)
                .thenCompose(v -> dependencyManager.relocateAll(executor))
                .thenCompose(v -> dependencyManager.loadAll(executor, appender));
    }
}
