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

package com.discordsrv.common.feature.linking.requirelinking;

import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.linking.AccountUnlinkedEvent;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.linking.RequiredLinkingConfig;
import com.discordsrv.common.config.main.linking.RequirementsConfig;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.core.scheduler.Scheduler;
import com.discordsrv.common.core.scheduler.executor.DynamicCachingThreadPoolExecutor;
import com.discordsrv.common.core.scheduler.threadfactory.CountingThreadFactory;
import com.discordsrv.common.feature.linking.LinkProvider;
import com.discordsrv.common.feature.linking.impl.MinecraftAuthenticationLinker;
import com.discordsrv.common.feature.linking.requirelinking.requirement.Requirement;
import com.discordsrv.common.feature.linking.requirelinking.requirement.RequirementType;
import com.discordsrv.common.feature.linking.requirelinking.requirement.parser.ParsedRequirements;
import com.discordsrv.common.feature.linking.requirelinking.requirement.parser.RequirementParser;
import com.discordsrv.common.feature.linking.requirelinking.requirement.type.DiscordBoostingRequirementType;
import com.discordsrv.common.feature.linking.requirelinking.requirement.type.DiscordRoleRequirementType;
import com.discordsrv.common.feature.linking.requirelinking.requirement.type.DiscordServerRequirementType;
import com.discordsrv.common.feature.linking.requirelinking.requirement.type.MinecraftAuthRequirementType;
import com.discordsrv.common.helper.Someone;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.util.*;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public abstract class RequiredLinkingModule<T extends DiscordSRV> extends AbstractModule<T> {

    public static String NOT_READY_MESSAGE = "The server is still connecting to Discord, please try again in a moment";

    private final List<RequirementType<?>> availableRequirementTypes = new ArrayList<>();
    private final Set<UUID> storageBypass = new HashSet<>();
    private ThreadPoolExecutor executor;

    public RequiredLinkingModule(T discordSRV) {
        super(discordSRV);
    }

    public DiscordSRV discordSRV() {
        return discordSRV;
    }

    public abstract RequiredLinkingConfig config();

    @Override
    public boolean canEnableBeforeReady() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return discordSRV.config() == null || config().enabled;
    }

    @MustBeInvokedByOverriders
    @Override
    public void enable() {
        executor = new DynamicCachingThreadPoolExecutor(
                1,
                Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                10,
                TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new CountingThreadFactory(Scheduler.THREAD_NAME_PREFIX + "RequiredLinking #%s")
        );
    }

    @MustBeInvokedByOverriders
    @Override
    public void disable() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    @Override
    public final void reload(Consumer<ReloadResult> resultConsumer) {
        List<RequirementType<?>> requirementTypes = new ArrayList<>();

        requirementTypes.add(new DiscordRoleRequirementType(this));
        requirementTypes.add(new DiscordServerRequirementType(this));
        requirementTypes.add(new DiscordBoostingRequirementType(this));

        if (discordSRV.linkProvider() instanceof MinecraftAuthenticationLinker) {
            requirementTypes.addAll(MinecraftAuthRequirementType.createRequirements(this));
        }

        synchronized (availableRequirementTypes) {
            for (RequirementType<?> requirementType : availableRequirementTypes) {
                discordSRV.moduleManager().unregister(requirementType);
            }
            availableRequirementTypes.clear();

            for (RequirementType<?> requirementType : requirementTypes) {
                discordSRV.moduleManager().register(requirementType);
            }
            availableRequirementTypes.addAll(requirementTypes);
        }

        if (discordSRV.config() != null) {
            reload();
            updateBypassFromStorage();
        }
    }

    public boolean isBypassingLinkingByConfig(UUID playerUUID) {
        return false;
    }

    public boolean isBypassingLinking(UUID playerUUID) {
        return storageBypass.contains(playerUUID);
    }

    public Set<UUID> getBypassingPlayers() {
        return storageBypass;
    }

    public void addLinkingBypass(UUID playerUUID) {
        updateBypassFromStorage();
        synchronized (storageBypass) {
            if (storageBypass.contains(playerUUID)) {
                return;
            }

            discordSRV.storage().addRequiredLinkingBypass(playerUUID);
            storageBypass.add(playerUUID);
        }
    }

    public void removeLinkingBypass(UUID playerUUID) {
        updateBypassFromStorage();
        synchronized (storageBypass) {
            if (!storageBypass.contains(playerUUID)) {
                return;
            }

            discordSRV.storage().removeRequiredLinkingBypass(playerUUID);
            storageBypass.remove(playerUUID);
        }
    }

    private void updateBypassFromStorage() {
        Set<UUID> bypass = discordSRV.storage().getRequiredLinkingBypass();
        synchronized (this.storageBypass) {
            storageBypass.clear();
            storageBypass.addAll(bypass);
        }
    }

    public abstract void reload();

    public abstract List<ParsedRequirements> getAllActiveRequirements();
    public abstract void recheck(IPlayer player);

    private void recheck(Someone someone) {
        IPlayer player = someone.onlinePlayer();
        if (player != null) {
            recheck(player);
            return;
        }

        someone.resolve()
                .thenApply(resolved -> resolved != null ? resolved.onlinePlayer() : null)
                .whenComplete((onlinePlayer, t) -> {
                    if (t != null) {
                        logger().error("Failed to get linked account for " + someone, t);
                    }
                    if (onlinePlayer != null) {
                        recheck(onlinePlayer);
                    }
                });
    }

    public <RT> void stateChanged(Someone someone, RequirementType<RT> requirementType, RT value, boolean newState) {
        for (ParsedRequirements activeRequirement : getAllActiveRequirements()) {
            for (Requirement<?> requirement : activeRequirement.usedRequirements()) {
                if (requirement.type() != requirementType || !Objects.equals(requirement.value(), value)) {
                    continue;
                }

                // One of the checks now fails
                recheck(someone);
                break;
            }
        }
    }

    @Subscribe
    public void onAccountUnlinked(AccountUnlinkedEvent event) {
        recheck(Someone.of(discordSRV, event.getPlayerUUID(), event.getUserId()));
    }

    protected List<ParsedRequirements> compile(List<String> additionalRequirements) {
        List<ParsedRequirements> parsed = new ArrayList<>();
        for (String input : additionalRequirements) {
            ParsedRequirements parsedRequirement = RequirementParser.getInstance()
                    .parse(input, availableRequirementTypes);
            parsed.add(parsedRequirement);
        }

        return parsed;
    }

    public List<MinecraftAuthRequirementType.Provider> getActiveMinecraftAuthProviders() {
        List<MinecraftAuthRequirementType.Provider> providers = new ArrayList<>();
        for (ParsedRequirements parsedRequirements : getAllActiveRequirements()) {
            for (Requirement<?> requirement : parsedRequirements.usedRequirements()) {
                RequirementType<?> requirementType = requirement.type();
                if (requirementType instanceof MinecraftAuthRequirementType) {
                    providers.add(((MinecraftAuthRequirementType<?>) requirementType).getProvider());
                }
            }
        }
        return providers;
    }

    public Task<Component> getBlockReason(
            RequirementsConfig config,
            List<ParsedRequirements> additionalRequirements,
            UUID playerUUID,
            String playerName,
            boolean join
    ) {
        if (storageBypass.contains(playerUUID) || config.bypassUUIDs.contains(playerUUID.toString())) {
            // Bypasses: let them through
            logger().debug("Player " + playerName + " is bypassing required linking requirements");
            return Task.completed(null);
        }

        LinkProvider linkProvider = discordSRV.linkProvider();
        if (linkProvider == null) {
            // Link provider unavailable but required linking enabled: error message
            Component message = ComponentUtil.fromAPI(
                    discordSRV.messagesConfig().unableToCheckLinkingStatus.minecraft().textBuilder().build()
            );
            return Task.completed(message);
        }

        return linkProvider.query(playerUUID, true).then(link -> {
            if (!link.isPresent()) {
                // User is not linked
                return linkProvider.getLinkingInstructions(playerName, playerUUID, null, join ? "join" : "freeze")
                        .thenApply(ComponentUtil::fromAPI);
            }

            long userId = link.get().userId();
            if (additionalRequirements.isEmpty()) {
                // No additional requirements: let them through
                return Task.completed(null);
            }

            Task<Void> pass = new Task<>();
            List<Task<Boolean>> all = new ArrayList<>();

            for (ParsedRequirements requirement : additionalRequirements) {
                Task<Boolean> future = requirement.predicate().apply(Someone.of(discordSRV, playerUUID, userId));

                all.add(future.thenApply(val -> {
                    if (val) {
                        pass.complete(null);
                    }
                    return val;
                }).whenFailed(t -> logger().debug("Check \"" + requirement.input() + "\" failed for "
                                       + playerName + " / " + Long.toUnsignedString(userId), t)));
            }

            // Complete when at least one passes or all of them completed
            return Task.anyOf(pass, Task.allOf(all)).thenApply(v -> {
                if (pass.isDone()) {
                    // One of the futures passed: let them through
                    return null;
                }

                // None of the futures passed: additional requirements not met
                return Component.text("You did not pass requirements");
            });
        });
    }
}
