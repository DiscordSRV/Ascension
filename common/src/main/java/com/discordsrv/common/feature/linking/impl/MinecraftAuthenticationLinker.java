/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.linking.AccountLink;
import com.discordsrv.common.feature.linking.LinkStore;
import com.discordsrv.common.feature.linking.LinkingModule;
import com.discordsrv.common.feature.linking.requirelinking.RequiredLinkingModule;
import com.discordsrv.common.feature.linking.requirelinking.requirement.type.MinecraftAuthRequirementType;
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
import java.util.function.Consumer;
import java.util.function.Function;
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
    public Task<Optional<AccountLink>> query(@NotNull UUID playerUUID, boolean canCauseLink) {
        return query(
                canCauseLink,
                () -> AuthService.lookup(AccountType.MINECRAFT, playerUUID.toString(), AccountType.DISCORD)
                        .map(account -> (DiscordAccount) account)
                        .map(discord -> Long.parseUnsignedLong(discord.getUserId())),
                () -> linkStore.get(playerUUID),
                AccountLink::userId,
                userId -> module().link(playerUUID, userId),
                userId -> module().unlink(playerUUID, userId),
                playerUUID.toString()
        ).mapException(t -> {
            throw new RuntimeException("Failed to lookup user id for " + playerUUID, t);
        });
    }

    @Override
    public Task<Optional<AccountLink>> query(long userId, boolean canCauseLink) {
        return query(
                canCauseLink,
                () -> AuthService.lookup(AccountType.DISCORD, Long.toUnsignedString(userId), AccountType.MINECRAFT)
                        .map(account -> (MinecraftAccount) account)
                        .map(MinecraftAccount::getUUID),
                () -> linkStore.get(userId),
                AccountLink::playerUUID,
                playerUUID -> module().link(playerUUID, userId),
                playerUUID -> module().unlink(playerUUID, userId),
                Long.toUnsignedString(userId)
        ).mapException(t -> {
            throw new RuntimeException("Failed to lookup Player UUID for " + Long.toUnsignedString(userId), t);
        });
    }

    @Override
    public Task<MinecraftComponent> getLinkingInstructions(
            String username,
            UUID playerUUID,
            Locale locale,
            @Nullable String requestReason,
            Object... additionalContext
    ) {
        return getInstructions(username, playerUUID, locale, requestReason);
    }

    @Override
    public boolean isValidCode(@NotNull String code) {
        throw new IllegalStateException("Does not offer codes");
    }

    @Override
    public @NotNull LinkStore store() {
        return linkStore;
    }

    private Task<MinecraftComponent> getInstructions(
            String playerName,
            UUID playerUUID,
            @Nullable Locale locale,
            @Nullable String requestReason,
            Object... additionalContext
    ) {
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
        MinecraftComponent component = discordSRV.messagesConfig(locale).minecraftAuthLinking.textBuilder()
                .addContext(additionalContext)
                .addPlaceholder("minecraftauth_link", url + (method != null ? "?command=" + method : null))
                .addPlaceholder("minecraftauth_link_simple", simple)
                .addPlaceholder("player_name", playerName)
                .addPlaceholder("player_uuid", playerUUID)
                .build();
        return Task.completed(component);
    }

    private LinkingModule module() {
        LinkingModule module = discordSRV.getModule(LinkingModule.class);
        if (module == null) {
            throw new IllegalStateException("LinkingModule not available");
        }
        return module;
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalAssignedToNull"}) // Hack
    private static final Optional<?> ERROR = null;

    @SuppressWarnings("unchecked")
    private <T> Task<Optional<AccountLink>> query(
            boolean canCauseLink,
            CheckedSupplier<Optional<T>> authSupplier,
            Supplier<Task<Optional<AccountLink>>> storageSupplier,
            Function<AccountLink, T> linkMap,
            Consumer<T> linked,
            Consumer<T> unlinked,
            String identifier
    ) {
        Task<Optional<AccountLink>> storageFuture = storageSupplier.get();
        if (!canCauseLink) {
            // If we can't cause a link, use the current account in storage
            return storageFuture;
        }

        Task<Optional<T>> authService = discordSRV.scheduler().supply(authSupplier).mapException(t -> {
            logger.error("Failed to query \"" + identifier + "\" from auth service", t);
            return (Optional<T>) ERROR;
        });

        return Task.allOf(authService, storageFuture).thenApply(results -> {
            Optional<T> auth = authService.join();
            Optional<AccountLink> storage = storageFuture.join();
            if (auth == ERROR) {
                // If we can't query the auth service, we'll just use the link from storage
                return storage;
            }

            if (auth.isPresent() && !storage.isPresent()) {
                // new link
                linked.accept(auth.get());
            }
            if (!auth.isPresent() && storage.isPresent()) {
                // unlink
                unlinked.accept(linkMap.apply(storage.get()));
            }
            if (auth.isPresent() && storage.isPresent()) {
                T authValue = auth.get();
                T storageValue = linkMap.apply(storage.get());
                if (!authValue.equals(storageValue)) {
                    // linked account changed
                    unlinked.accept(linkMap.apply(storage.get()));
                    linked.accept(auth.get());
                }
            }

            return storage;
        });
    }
}
