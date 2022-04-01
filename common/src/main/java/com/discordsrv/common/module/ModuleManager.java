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
import com.discordsrv.api.event.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.api.module.type.Module;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.debug.DebugGenerateEvent;
import com.discordsrv.common.debug.file.TextDebugFile;
import com.discordsrv.common.function.CheckedFunction;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.AbstractModule;
import com.discordsrv.common.module.type.ModuleDelegate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class ModuleManager {

    private final Set<Module> modules = new CopyOnWriteArraySet<>();
    private final Map<String, Module> moduleLookupTable = new ConcurrentHashMap<>();
    private final Map<Module, AbstractModule<?>> delegates = new ConcurrentHashMap<>();
    private final DiscordSRV discordSRV;
    private final Logger logger;

    public ModuleManager(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "MODULE_MANAGER");
        discordSRV.eventBus().subscribe(this);
    }

    public Logger logger() {
        return logger;
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

    private AbstractModule<?> getAbstract(Module module) {
        return module instanceof AbstractModule
            ? (AbstractModule<?>) module
            : delegates.computeIfAbsent(module, mod -> new ModuleDelegate(discordSRV, mod));
    }

    public <DT extends DiscordSRV> void registerModule(DT discordSRV, CheckedFunction<DT, AbstractModule<?>> function) {
        try {
            register(function.apply(discordSRV));
        } catch (Throwable t) {
            logger.debug("Module initialization failed", t);
        }
    }

    public void register(Module module) {
        if (module instanceof ModuleDelegate) {
            throw new IllegalArgumentException("Cannot register a delegate");
        }

        AbstractModule<?> abstractModule = getAbstract(module);

        logger.debug(module + " registered");
        this.modules.add(module);
        this.moduleLookupTable.put(module.getClass().getName(), module);

        if (discordSRV.config() != null) {
            // Check if config is ready, if it is already we'll enable the module
            enable(abstractModule);
        }
    }

    private void enable(AbstractModule<?> module) {
        try {
            if (module.enableModule()) {
                logger.debug(module + " enabled");
            }
        } catch (Throwable t) {
            discordSRV.logger().error("Failed to enable " + module.toString(), t);
        }
    }

    public void unregister(Module module) {
        if (module instanceof ModuleDelegate) {
            throw new IllegalArgumentException("Cannot unregister a delegate");
        }

        if (getAbstract(module).isHasBeenEnabled()) {
            disable(module);
        }

        this.modules.remove(module);
        this.moduleLookupTable.values().removeIf(mod -> mod == module);
        this.delegates.remove(module);
    }

    private void disable(Module module) {
        AbstractModule<?> abstractModule = getAbstract(module);
        try {
            logger.debug(module + " disabling");
            abstractModule.disable();
        } catch (Throwable t) {
            discordSRV.logger().error("Failed to disable " + abstractModule.toString(), t);
        }
    }

    @Subscribe(priority = EventPriority.EARLY)
    public void onShuttingDown(DiscordSRVShuttingDownEvent event) {
        for (Module module : modules) {
            unregister(module);
        }
    }

    public void reload() {
        for (Module module : modules) {
            AbstractModule<?> abstractModule = getAbstract(module);

            // Check if the module needs to be enabled due to reload
            enable(abstractModule);

            try {
                abstractModule.reload();
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
            AbstractModule<?> abstractModule = getAbstract(module);

            if (!abstractModule.isEnabled()) {
                disabled.add(abstractModule);
                continue;
            }
            appendModule(builder, abstractModule);
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
