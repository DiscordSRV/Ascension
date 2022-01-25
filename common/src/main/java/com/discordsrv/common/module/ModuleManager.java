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

package com.discordsrv.common.module;

import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVReloadEvent;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.api.module.type.Module;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.debug.DebugGenerateEvent;
import com.discordsrv.common.debug.file.TextDebugFile;
import com.discordsrv.common.module.type.AbstractModule;

import java.util.ArrayList;
import java.util.List;
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
        discordSRV.eventBus().subscribe(this);
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> moduleType) {
        return (T) moduleLookupTable.computeIfAbsent(moduleType.getName(), key -> {
            Module bestCandidate = null;
            int bestCandidatePriority = Integer.MIN_VALUE;
            for (Module module : modules) {
                int priority;
                if (moduleType.isAssignableFrom(module.getClass()) && ((priority = module.priority(moduleType)) > bestCandidatePriority)) {
                    bestCandidate = module;
                    bestCandidatePriority = priority;
                }
            }
            return bestCandidate;
        });
    }

    public void register(AbstractModule<?> module) {
        this.modules.add(module);
        this.moduleLookupTable.put(module.getClass().getName(), module);

        enable(module, true);
    }

    private void enable(Module module, boolean enableNonAbstract) {
        try {
            if (module instanceof AbstractModule) {
                ((AbstractModule<?>) module).enableModule();
            } else if (enableNonAbstract) {
                module.enable();
            }
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
            enable(module, false);

            try {
                module.reload();
            } catch (Throwable t) {
                discordSRV.logger().error("Failed to reload " + module.getClass().getSimpleName(), t);
            }
        }
    }

    @Subscribe
    public void onDebugGenerate(DebugGenerateEvent event) {
        StringBuilder builder = new StringBuilder();

        builder.append("Enabled modules:");
        List<Module> disabled = new ArrayList<>();
        for (Module module : modules) {
            if (!module.isEnabled()) {
                disabled.add(module);
                continue;
            }
            appendModule(builder, module);
        }

        builder.append("\n\nDisabled modules:");
        for (Module module : disabled) {
            appendModule(builder, module);
        }

        event.addFile(new TextDebugFile("modules.txt", builder));
    }

    private void appendModule(StringBuilder builder, Module module) {
        builder.append('\n').append(module.getClass().getName());
    }
}
