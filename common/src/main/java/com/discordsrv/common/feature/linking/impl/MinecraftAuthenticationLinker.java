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

package com.discordsrv.common.feature.linking.impl;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.linking.LinkStore;
import com.discordsrv.common.feature.linking.LinkingModule;
import com.discordsrv.common.feature.linking.requirelinking.RequiredLinkingModule;
import com.discordsrv.common.feature.linking.requirelinking.requirement.type.MinecraftAuthRequirementType;
import com.discordsrv.common.util.CompletableFutureUtil;
import com.discordsrv.common.util.function.CheckedSupplier;
import me.minecraftauth.lib.AuthService;
import me.minecraftauth.lib.account.AccountType;
import me.minecraftauth.lib.account.platform.discord.DiscordAccount;
import me.minecraftauth.lib.account.platform.minecraft.MinecraftAccount;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MinecraftAuthenticationLinker extends CachedLinkProvider {

    public static final String BASE_LINK_URL = DiscordSRV.WEBSITE + "/link";

    private final Logger logger;
    private final LinkStore linkStore;

    public MinecraftAuthenticationLinker(DiscordSRV discordSRV) {
        super(discordSRV);
        this.linkStore = new StorageLinker(discordSRV);
        this.logger = new NamedLogger(discordSRV, "MINECRAFTAUTH_LINKER");
    }

    @Override
    public CompletableFuture<Optional<Long>> queryUserId(@NotNull UUID playerUUID, boolean canCauseLink) {
        return query(
                canCauseLink,
                () -> AuthService.lookup(AccountType.MINECRAFT, playerUUID.toString(), AccountType.DISCORD)
                        .map(account -> (DiscordAccount) account)
                        .map(discord -> Long.parseUnsignedLong(discord.getUserId())),
                () -> linkStore.getUserId(playerUUID),
                userId -> linked(playerUUID, userId),
                userId -> unlinked(playerUUID, userId)
        ).exceptionally(t -> {
            logger.error("Lookup for uuid " + playerUUID + " failed", t);
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<Optional<UUID>> queryPlayerUUID(long userId, boolean canCauseLink) {
        return query(
                canCauseLink,
                () -> AuthService.lookup(AccountType.DISCORD, Long.toUnsignedString(userId), AccountType.MINECRAFT)
                        .map(account -> (MinecraftAccount) account)
                        .map(MinecraftAccount::getUUID),
                () -> linkStore.getPlayerUUID(userId),
                playerUUID -> linked(playerUUID, userId),
                playerUUID -> unlinked(playerUUID, userId)
        ).exceptionally(t -> {
            logger.error("Lookup for user id " + Long.toUnsignedString(userId) + " failed", t);
            return Optional.empty();
        });
    }

    @Override
    public CompletableFuture<MinecraftComponent> getLinkingInstructions(@NotNull IPlayer player, @Nullable String requestReason) {
        return getInstructions(player, requestReason);
    }

    @Override
    public CompletableFuture<MinecraftComponent> getLinkingInstructions(String username, UUID playerUUID, Locale locale, @Nullable String requestReason) {
        return getInstructions(null, requestReason);
    }

    private CompletableFuture<MinecraftComponent> getInstructions(@Nullable IPlayer player, @Nullable String requestReason) {
        String method = null;
        if (requestReason != null) {
            requestReason = requestReason.toLowerCase(Locale.ROOT);
            if (requestReason.startsWith("discord")) {
                method = "discord";
            } else if (requestReason.startsWith("link")) {
                method = "link";
            } else if (requestReason.startsWith("freeze")) {
                method = "freeze";
            }
        }

        StringBuilder additionalParam = new StringBuilder();
        RequiredLinkingModule<?> requiredLinkingModule = discordSRV.getModule(RequiredLinkingModule.class);
        if (requiredLinkingModule != null && requiredLinkingModule.isEnabled()) {
            for (MinecraftAuthRequirementType.Provider requirementProvider : requiredLinkingModule.getActiveMinecraftAuthProviders()) {
                additionalParam.append(requirementProvider.character());
            }
        }

        String url = BASE_LINK_URL + (additionalParam.length() > 0 ? "/" + additionalParam : "");
        String simple = url.substring(url.indexOf("://") + 3); // Remove protocol & don't include method query parameter
        MinecraftComponent component = discordSRV.messagesConfig(player).minecraftAuthLinking.textBuilder()
                .addContext(player)
                .addPlaceholder("minecraftauth_link", url + (method != null ? "?command=" + method : null))
                .addPlaceholder("minecraftauth_link_simple", simple)
                .applyPlaceholderService()
                .build();
        return CompletableFuture.completedFuture(component);
    }

    private void linked(UUID playerUUID, long userId) {
        logger.debug("New link: " + playerUUID + " & " + Long.toUnsignedString(userId));
        linkStore.createLink(playerUUID, userId).whenComplete((v, t) -> {
            if (t != null) {
                logger.error("Failed to link player persistently", t);
                return;
            }

            module().linked(playerUUID, userId);
        });

    }

    private void unlinked(UUID playerUUID, long userId) {
        logger.debug("Unlink: " + playerUUID + " & " + Long.toUnsignedString(userId));
        linkStore.removeLink(playerUUID, userId).whenComplete((v, t) -> {
            if (t != null) {
                logger.error("Failed to unlink player in persistent storage", t);
                return;
            }

            module().unlinked(playerUUID, userId);
        });
    }

    private LinkingModule module() {
        LinkingModule module = discordSRV.getModule(LinkingModule.class);
        if (module == null) {
            throw new IllegalStateException("LinkingModule not available");
        }
        return module;
    }

    private <T> CompletableFuture<Optional<T>> query(
            boolean canCauseLink,
            CheckedSupplier<Optional<T>> authSupplier,
            Supplier<CompletableFuture<Optional<T>>> storageSupplier,
            Consumer<T> linked,
            Consumer<T> unlinked
    ) {
        CompletableFuture<Optional<T>> authService = discordSRV.scheduler().supply(authSupplier);
        if (!canCauseLink) {
            return authService;
        }

        CompletableFuture<Optional<T>> storageFuture = storageSupplier.get();
        return CompletableFutureUtil.combine(authService, storageFuture).thenApply(results -> {
            Optional<T> auth = authService.join();
            Optional<T> storage = storageFuture.join();

            if (auth.isPresent() && !storage.isPresent()) {
                // new link
                linked.accept(auth.get());
            }
            if (!auth.isPresent() && storage.isPresent()) {
                // unlink
                unlinked.accept(storage.get());
            }
            if (auth.isPresent() && storage.isPresent() && !auth.get().equals(storage.get())) {
                // linked account changed
                unlinked.accept(storage.get());
                linked.accept(auth.get());
            }

            return auth;
        });
    }
}
