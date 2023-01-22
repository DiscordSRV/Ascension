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

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.linking.LinkProvider;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;
import me.minecraftauth.lib.AuthService;
import me.minecraftauth.lib.account.AccountType;
import me.minecraftauth.lib.account.Identity;
import me.minecraftauth.lib.account.MinecraftAccount;
import me.minecraftauth.lib.exception.LookupException;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MinecraftAuthenticationLinker extends CachedLinkProvider implements LinkProvider {

    private final Logger logger;

    public MinecraftAuthenticationLinker(DiscordSRV discordSRV) {
        super(discordSRV);
        this.logger = new NamedLogger(discordSRV, "MINECRAFTAUTH_LINKER");
    }

    @Override
    public CompletableFuture<Optional<Long>> queryUserId(@NotNull UUID playerUUID) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return AuthService.lookup(AccountType.MINECRAFT, playerUUID.toString())
                                .map(Identity::getDiscordAccount)
                                .map(discord -> Long.parseUnsignedLong(discord.getUserId()));
                    } catch (LookupException e) {
                        logger.error("Lookup for uuid " + playerUUID + " failed", e);
                        return Optional.empty();
                    }
                },
                discordSRV.scheduler().executor()
        );
    }

    @Override
    public CompletableFuture<Optional<UUID>> queryPlayerUUID(long userId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        return AuthService.lookup(AccountType.DISCORD, Long.toUnsignedString(userId))
                                .map(Identity::getMinecraftAccount)
                                .map(MinecraftAccount::getUUID);
                    } catch (LookupException e) {
                        logger.error("Lookup for user id " + Long.toUnsignedString(userId) + " failed", e);
                        return Optional.empty();
                    }
                },
                discordSRV.scheduler().executor()
        );
    }
}
