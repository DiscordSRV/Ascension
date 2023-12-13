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

package com.discordsrv.common.linking.requirelinking;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.linking.RequirementsConfig;
import com.discordsrv.common.config.main.linking.ServerRequiredLinkingConfig;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.linking.LinkProvider;
import com.discordsrv.common.linking.requirelinking.requirement.MinecraftAuthRequirement;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public abstract class ServerRequireLinkingModule<T extends DiscordSRV> extends RequiredLinkingModule<T> {

    private final List<CompiledRequirement> compiledRequirements = new CopyOnWriteArrayList<>();

    public ServerRequireLinkingModule(T discordSRV) {
        super(discordSRV);
    }

    @Override
    public abstract ServerRequiredLinkingConfig config();

    @Override
    public void reload(Consumer<DiscordSRVApi.ReloadResult> resultConsumer) {
        super.reload(resultConsumer);

        synchronized (compiledRequirements) {
            activeRequirementTypes.clear();

            compiledRequirements.clear();
            compiledRequirements.addAll(compile(config().requirements.requirements));
        }
    }

    public List<MinecraftAuthRequirement.Type> getRequirementTypes() {
        return activeRequirementTypes;
    }

    public CompletableFuture<Component> getBlockReason(UUID playerUUID, String playerName, boolean join) {
        RequirementsConfig config = config().requirements;
        if (config.bypassUUIDs.contains(playerUUID.toString())) {
            // Bypasses: let them through
            logger().debug("Player " + playerName + " is bypassing required linking requirements");
            return CompletableFuture.completedFuture(null);
        }

        LinkProvider linkProvider = discordSRV.linkProvider();
        if (linkProvider == null) {
            // Link provider unavailable but required linking enabled: error message
            Component message = ComponentUtil.fromAPI(
                    discordSRV.messagesConfig().minecraft.unableToCheckLinkingStatus.textBuilder().build()
            );
            return CompletableFuture.completedFuture(message);
        }

        return linkProvider.queryUserId(playerUUID, true)
                .thenCompose(opt -> {
                    if (!opt.isPresent()) {
                        // User is not linked
                        return linkProvider.getLinkingInstructions(playerName, playerUUID, null, join ? "join" : "freeze")
                                .thenApply(ComponentUtil::fromAPI);
                    }

                    List<CompiledRequirement> requirements;
                    synchronized (compiledRequirements) {
                        requirements = compiledRequirements;
                    }

                    if (requirements.isEmpty()) {
                        // No additional requirements: let them through
                        return CompletableFuture.completedFuture(null);
                    }

                    CompletableFuture<Void> pass = new CompletableFuture<>();
                    List<CompletableFuture<Boolean>> all = new ArrayList<>();
                    long userId = opt.get();

                    for (CompiledRequirement requirement : requirements) {
                        CompletableFuture<Boolean> future = requirement.function().apply(playerUUID, userId);
                        all.add(future);

                        future.whenComplete((val, t) -> {
                            if (val != null && val) {
                                pass.complete(null);
                            }
                        }).exceptionally(t -> {
                            logger().debug("Check \"" + requirement.input() + "\" failed for " + playerName + " / " + Long.toUnsignedString(userId), t);
                            return null;
                        });
                    }

                    // Complete when at least one passes or all of them completed
                    return CompletableFuture.anyOf(pass, CompletableFutureUtil.combine(all))
                            .thenApply(v -> {
                                if (pass.isDone()) {
                                    // One of the futures passed: let them through
                                    return null;
                                }

                                // None of the futures passed: requirements not met
                                return Component.text("You did not pass requirements");
                            });
                });
    }
}
