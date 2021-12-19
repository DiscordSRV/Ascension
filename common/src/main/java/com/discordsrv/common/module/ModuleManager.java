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

package com.discordsrv.common.module;

import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVReloadEvent;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.common.DiscordSRV;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class ModuleManager {

    private final Set<Module> modules = new CopyOnWriteArraySet<>();
    private final Map<String, Module> moduleLookupTable = new ConcurrentHashMap<>();
    private final DiscordSRV discordSRV;

    public ModuleManager(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> moduleType) {
        return (T) moduleLookupTable.computeIfAbsent(moduleType.getName(), key -> {
            for (Module module : modules) {
                if (moduleType.isAssignableFrom(module.getClass())) {
                    return module;
                }
            }
            return null;
        });
    }

    public void register(Module module) {
        this.modules.add(module);
        this.moduleLookupTable.put(module.getClass().getName(), module);

        enable(module);
    }

    private void enable(Module module) {
        try {
            module.enableModule();
        } catch (Throwable t) {
            discordSRV.logger().error("Failed to enable " + module.getClass().getSimpleName(), t);
        }
    }

    public void unregister(Module module) {
        this.modules.remove(module);
        this.moduleLookupTable.values().removeIf(mod -> mod == module);

        disable(module);
    }

    private void disable(Module module) {
        try {
            module.disable();
        } catch (Throwable t) {
            discordSRV.logger().error("Failed to disable " + module.getClass().getSimpleName(), t);
        }
    }

    @Subscribe(priority = EventPriority.EARLY)
    public void onShuttingDown(DiscordSRVShuttingDownEvent event) {
        for (Module module : modules) {
            unregister(module);
        }
    }

    @Subscribe(priority = EventPriority.EARLY)
    public void onReload(DiscordSRVReloadEvent event) {
        for (Module module : modules) {
            // Check if the module needs to be enabled due to reload
            enable(module);

            try {
                module.reload();
            } catch (Throwable t) {
                discordSRV.logger().error("Failed to reload " + module.getClass().getSimpleName(), t);
            }
        }
    }
}
