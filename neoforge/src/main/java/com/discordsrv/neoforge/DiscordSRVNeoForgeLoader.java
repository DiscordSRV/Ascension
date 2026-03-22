/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.neoforge;

import dev.vankka.dependencydownload.jarinjar.classloader.JarInJarClassLoader;
import dev.vankka.dependencydownload.jarinjar.loader.ILoader;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.net.URL;

@Mod("discordsrv")
public class DiscordSRVNeoForgeLoader implements ILoader {

    protected final JarInJarClassLoader classLoader;
    private final ModContainer modContainer;
    private final IEventBus eventBus;

    public DiscordSRVNeoForgeLoader(IEventBus eventBus, ModContainer modContainer) {
        super();
        this.modContainer = modContainer;
        this.eventBus = eventBus;
        this.classLoader = initialize();
    }

    @Override
    public final void initiateBootstrap(Class<?> bootstrapClass, @NotNull JarInJarClassLoader classLoader) throws ReflectiveOperationException {
        Constructor<?> constructor = bootstrapClass.getConstructor(JarInJarClassLoader.class, ModContainer.class, IEventBus.class);
        constructor.newInstance(classLoader, modContainer, eventBus);
    }

    @Override
    public @NotNull String getBootstrapClassName() {
        return "com.discordsrv.neoforge.DiscordSRVNeoForgeBootstrap";
    }

    @Override
    public @NotNull String getName() {
        return "DiscordSRV";
    }

    @Override
    public @NotNull URL getJarInJarResource() {
        URL resource = getClass().getClassLoader().getResource("neoforge.jarinjar");
        if (resource == null) {
            throw new IllegalStateException("Jar does not contain jarinjar");
        }
        return resource;
    }
}
