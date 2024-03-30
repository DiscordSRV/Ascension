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

package com.discordsrv.common.module;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.api.discord.connection.details.DiscordCacheFlag;
import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.discord.connection.details.DiscordMemberCachePolicy;
import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVReadyEvent;
import com.discordsrv.api.event.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.api.module.Module;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.commands.subcommand.reload.ReloadResults;
import com.discordsrv.common.debug.DebugGenerateEvent;
import com.discordsrv.common.debug.file.TextDebugFile;
import com.discordsrv.common.discord.connection.jda.JDAConnectionManager;
import com.discordsrv.common.function.CheckedFunction;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.AbstractModule;
import com.discordsrv.common.module.type.ModuleDelegate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiConsumer;
import java.util.function.Function;

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

    private <T> Set<T> getModuleDetails(Function<Module, Collection<T>> detailFunction, BiConsumer<AbstractModule<?>, Collection<T>> setRequested) {
        Set<T> details = new HashSet<>();
        for (Module module : modules) {
            try {
                if (!module.isEnabled()) {
                    continue;
                }

                Collection<T> values = detailFunction.apply(module);
                details.addAll(values);

                setRequested.accept(getAbstract(module), values);
            } catch (Throwable t) {
                logger.debug("Failed to get details from " + module.getClass(), t);
            }
        }
        return details;
    }

    public Set<DiscordGatewayIntent> requiredIntents() {
        return getModuleDetails(Module::requiredIntents, AbstractModule::setRequestedIntents);
    }

    public Set<DiscordCacheFlag> requiredCacheFlags() {
        return getModuleDetails(Module::requiredCacheFlags, AbstractModule::setRequestedCacheFlags);
    }

    public Set<DiscordMemberCachePolicy> requiredMemberCachePolicies() {
        return getModuleDetails(Module::requiredMemberCachingPolicies, (mod, result) -> mod.setRequestedMemberCachePolicies(result.size()));
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(Class<T> moduleType) {
        return (T) moduleLookupTable.computeIfAbsent(moduleType.getName(), key -> {
            Module bestCandidate = null;
            int bestCandidatePriority = Integer.MIN_VALUE;
            for (Module module : modules) {
                if (!module.isEnabled()) {
                    continue;
                }
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

        this.modules.add(module);
        this.moduleLookupTable.put(module.getClass().getName(), module);

        logger.debug(module + " registered");

        if (discordSRV.isReady()) {
            // Check if Discord connection is ready, if it is already we'll enable the module
            enable(getAbstract(module));
        }
    }

    public void unregister(Module module) {
        if (module instanceof ModuleDelegate) {
            throw new IllegalArgumentException("Cannot unregister a delegate");
        }

        // Disable if needed
        disable(getAbstract(module));

        this.modules.remove(module);
        this.moduleLookupTable.values().removeIf(mod -> mod == module);
        this.delegates.remove(module);

        logger.debug(module + " unregistered");
    }

    private void enable(AbstractModule<?> module) {
        try {
            if (module.enableModule()) {
                logger.debug(module + " enabled");
            }
        } catch (Throwable t) {
            discordSRV.logger().error("Failed to enable " + module.getClass().getSimpleName(), t);
        }
    }

    private void disable(AbstractModule<?> module) {
        try {
            if (module.disableModule()) {
                logger.debug(module + " disabled");
            }
        } catch (Throwable t) {
            discordSRV.logger().error("Failed to disable " + module.getClass().getSimpleName(), t);
        }
    }

    @Subscribe(priority = EventPriority.EARLY)
    public void onShuttingDown(DiscordSRVShuttingDownEvent event) {
        modules.stream()
                .sorted((m1, m2) -> Integer.compare(m2.shutdownOrder(), m1.shutdownOrder()))
                .forEachOrdered(module -> disable(getAbstract(module)));
    }

    @Subscribe
    public void onDiscordSRVReady(DiscordSRVReadyEvent event) {
        reload();
    }

    public List<DiscordSRV.ReloadResult> reload() {
        JDAConnectionManager connectionManager = discordSRV.discordConnectionManager();

        Set<DiscordSRVApi.ReloadResult> reloadResults = new HashSet<>();
        for (Module module : modules) {
            AbstractModule<?> abstractModule = getAbstract(module);

            boolean fail = false;
            if (abstractModule.isEnabled()) {
                for (DiscordGatewayIntent requiredIntent : abstractModule.getRequestedIntents()) {
                    if (!connectionManager.getIntents().contains(requiredIntent)) {
                        fail = true;
                        logger().warning("Missing gateway intent " + requiredIntent.name() + " for module " + module.getClass().getSimpleName());
                    }
                }
                for (DiscordCacheFlag requiredCacheFlag : abstractModule.getRequestedCacheFlags()) {
                    if (!connectionManager.getCacheFlags().contains(requiredCacheFlag)) {
                        fail = true;
                        logger().warning("Missing cache flag " + requiredCacheFlag.name() + " for module " + module.getClass().getSimpleName());
                    }
                }
            }

            if (fail) {
                reloadResults.add(ReloadResults.DISCORD_CONNECTION_RELOAD_REQUIRED);
            }

            // Check if the module needs to be enabled or disabled
            if (!fail) {
                enable(abstractModule);
            }
            if (!abstractModule.isEnabled()) {
                disable(abstractModule);
                continue;
            }

            try {
                abstractModule.reload(result -> {
                    if (result == null) {
                        throw new NullPointerException("null result supplied to resultConsumer");
                    }
                    reloadResults.add(result);
                });
            } catch (Throwable t) {
                discordSRV.logger().error("Failed to reload " + module.getClass().getSimpleName(), t);
            }
        }

        List<DiscordSRVApi.ReloadResult> results = new ArrayList<>();

        List<DiscordSRV.ReloadResult> validResults = Arrays.asList(DiscordSRVApi.ReloadResult.DefaultConstants.values());
        for (DiscordSRVApi.ReloadResult reloadResult : reloadResults) {
            if (validResults.contains(reloadResult)) {
                results.add(reloadResult);
            }
        }

        return results;
    }

    @Subscribe
    public void onDebugGenerate(DebugGenerateEvent event) {
        StringBuilder builder = new StringBuilder();

        builder.append("Enabled modules:");
        List<Module> disabled = new ArrayList<>();
        for (Module module : modules) {
            if (!getAbstract(module).isEnabled()) {
                disabled.add(module);
                continue;
            }

            appendModule(builder, module, true);
        }

        builder.append("\n\nDisabled modules:");
        for (Module module : disabled) {
            appendModule(builder, module, false);
        }

        event.addFile(new TextDebugFile("modules.txt", builder));
    }

    private void appendModule(StringBuilder builder, Module module, boolean extra) {
        builder.append('\n').append(module.getClass().getName());

        if (!extra) {
            return;
        }

        AbstractModule<?> mod = getAbstract(module);

        List<DiscordGatewayIntent> intents = mod.getRequestedIntents();
        if (!intents.isEmpty()) {
            builder.append("\n Intents: ").append(intents);
        }

        List<DiscordCacheFlag> cacheFlags = mod.getRequestedCacheFlags();
        if (!cacheFlags.isEmpty()) {
            builder.append("\n Cache Flags: ").append(cacheFlags);
        }

        int memberCachePolicies = mod.getRequestedMemberCachePolicies();
        if (memberCachePolicies != 0) {
            builder.append("\n Member Cache Policies: ").append(memberCachePolicies);
        }
    }
}
