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

package com.discordsrv.common.linking.impl;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.function.CheckedSupplier;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.linking.LinkProvider;
import com.discordsrv.common.linking.LinkStore;
import com.discordsrv.common.linking.LinkingModule;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;
import me.minecraftauth.lib.AuthService;
import me.minecraftauth.lib.account.AccountType;
import me.minecraftauth.lib.account.platform.discord.DiscordAccount;
import me.minecraftauth.lib.account.platform.minecraft.MinecraftAccount;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MinecraftAuthenticationLinker extends CachedLinkProvider implements LinkProvider {

    private final Logger logger;
    private final LinkStore linkStore;

    public MinecraftAuthenticationLinker(DiscordSRV discordSRV) {
        super(discordSRV);
        this.linkStore = new StorageLinker(discordSRV);
        this.logger = new NamedLogger(discordSRV, "MINECRAFTAUTH_LINKER");
    }

    @Override
    public CompletableFuture<Optional<Long>> queryUserId(@NotNull UUID playerUUID) {
        return query(
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
    public CompletableFuture<Optional<UUID>> queryPlayerUUID(long userId) {
        return query(
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
    public MinecraftComponent getLinkingInstructions(String username, UUID playerUUID, Locale locale) {
        return ComponentUtil.toAPI(Component.text("<linking instructions>"));
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
        linkStore.createLink(playerUUID, userId).whenComplete((v, t) -> {
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
            CheckedSupplier<Optional<T>> authSupplier,
            Supplier<CompletableFuture<Optional<T>>> storageSupplier,
            Consumer<T> linked,
            Consumer<T> unlinked
    ) {
        CompletableFuture<Optional<T>> authService = new CompletableFuture<>();

        discordSRV.scheduler().run(() -> {
            try {
                authService.complete(authSupplier.get());
            } catch (Throwable t) {
                authService.completeExceptionally(t);
            }
        });

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
