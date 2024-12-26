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

package com.discordsrv.common.core.module;

import com.discordsrv.api.discord.connection.details.DiscordCacheFlag;
import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.discord.connection.details.DiscordMemberCachePolicy;
import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.lifecycle.DiscordSRVReadyEvent;
import com.discordsrv.api.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.api.module.Module;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.core.module.type.ModuleDelegate;
import com.discordsrv.common.core.module.type.PluginIntegration;
import com.discordsrv.common.discord.connection.jda.JDAConnectionManager;
import com.discordsrv.common.events.integration.IntegrationLifecycleEvent;
import com.discordsrv.common.feature.debug.DebugGenerateEvent;
import com.discordsrv.common.feature.debug.file.TextDebugFile;
import com.discordsrv.common.util.function.CheckedFunction;

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

    private String getName(AbstractModule<?> abstractModule) {
        return abstractModule instanceof ModuleDelegate
                ? ((ModuleDelegate) abstractModule).getBase().getClass().getName()
               : abstractModule.getClass().getSimpleName();
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

        logger.debug(module.getClass().getName() + " registered");

        // Enable the module if we're already ready
        enableOrDisableAsNeeded(getAbstract(module), discordSRV.isReady(), true);
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

        logger.debug(module.getClass().getName() + " unregistered");
    }

    private List<ReloadResult> enable(AbstractModule<?> module) {
        try {
            if (module.enableModule()) {
                logger.debug(module + " enabled");
                return reload(module);
            }
        } catch (Throwable t) {
            discordSRV.logger().error("Failed to enable " + getName(module), t);
            return Collections.emptyList();
        }
        return null;
    }

    private List<ReloadResult> reload(AbstractModule<?> module) {
        List<ReloadResult> reloadResults = new ArrayList<>();
        try {
            module.reload(result -> {
                if (result == null) {
                    throw new NullPointerException("null result supplied to resultConsumer");
                }
                reloadResults.add(result);
            });
        } catch (Throwable t) {
            discordSRV.logger().error("Failed to reload " + getName(module), t);
        }
        return reloadResults;
    }

    private void disable(AbstractModule<?> module) {
        try {
            if (module.disableModule()) {
                logger.debug(module + " disabled");
            }
        } catch (Throwable t) {
            discordSRV.logger().error("Failed to disable " + getName(module), t);
        }
    }

    @Subscribe(priority = EventPriorities.EARLY)
    public void onShuttingDown(DiscordSRVShuttingDownEvent event) {
        modules.stream()
                .sorted((m1, m2) -> Integer.compare(m2.shutdownOrder(), m1.shutdownOrder()))
                .forEachOrdered(module -> disable(getAbstract(module)));
    }

    @Subscribe
    public void onDiscordSRVReady(DiscordSRVReadyEvent event) {
        reload();
    }

    @Subscribe
    public void onIntegrationLifecycle(IntegrationLifecycleEvent event) {
        String integrationIdentifier = event.integrationIdentifier();
        for (Module module : modules) {
            AbstractModule<?> abstractModule = getAbstract(module);
            if (abstractModule instanceof PluginIntegration
                    && ((PluginIntegration<?>) abstractModule).getIntegrationId().equals(integrationIdentifier)) {
                enableOrDisableAsNeeded(abstractModule, discordSRV.isReady(), true);
            }
        }
    }

    public List<ReloadResult> reload() {
        return reloadAndEnableModules(true);
    }

    public void enableModules() {
        reloadAndEnableModules(false);
    }

    private synchronized List<ReloadResult> reloadAndEnableModules(boolean reload) {
        boolean isReady = discordSRV.isReady();
        logger().debug((reload ? "Reloading" : "Enabling") + " modules (DiscordSRV ready = " + isReady + ")");

        Set<ReloadResult> reloadResults = new HashSet<>();
        for (Module module : modules) {
            reloadResults.addAll(enableOrDisableAsNeeded(getAbstract(module), isReady, reload));
        }

        List<ReloadResult> results = new ArrayList<>();

        List<ReloadResult> validResults = Arrays.asList(ReloadResult.values());
        for (ReloadResult reloadResult : reloadResults) {
            if (validResults.contains(reloadResult)) {
                results.add(reloadResult);
            }
        }

        return results;
    }

    private List<ReloadResult> enableOrDisableAsNeeded(AbstractModule<?> module, boolean isReady, boolean mayReload) {
        boolean canBeEnabled = isReady || module.canEnableBeforeReady();
        if (!canBeEnabled) {
            return Collections.emptyList();
        }

        boolean enabled = module.isEnabled();
        if (!enabled) {
            disable(module);
            return Collections.emptyList();
        }

        JDAConnectionManager connectionManager = discordSRV.discordConnectionManager();

        boolean fail = false;
        for (DiscordGatewayIntent requiredIntent : module.getRequestedIntents()) {
            if (!connectionManager.getIntents().contains(requiredIntent)) {
                fail = true;
                logger().warning("Missing gateway intent " + requiredIntent.name() + " for module " + getName(module));
            }
        }
        for (DiscordCacheFlag requiredCacheFlag : module.getRequestedCacheFlags()) {
            if (!connectionManager.getCacheFlags().contains(requiredCacheFlag)) {
                fail = true;
                logger().warning("Missing cache flag " + requiredCacheFlag.name() + " for module " + getName(module));
            }
        }

        List<ReloadResult> reloadResults = new ArrayList<>();
        if (fail) {
            reloadResults.add(ReloadResult.DISCORD_CONNECTION_RELOAD_REQUIRED);
        }

        // Enable the module if reload passed
        if (!fail) {
            List<ReloadResult> results = enable(module);
            if (results != null) {
                reloadResults.addAll(results);
            } else if (mayReload) {
                reloadResults.addAll(reload(module));
            }
        }

        return reloadResults;
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
