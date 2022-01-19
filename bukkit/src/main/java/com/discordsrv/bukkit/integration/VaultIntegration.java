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

package com.discordsrv.bukkit.integration;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.function.CheckedSupplier;
import com.discordsrv.common.module.type.PermissionDataProvider;
import com.discordsrv.common.module.type.PluginIntegration;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VaultIntegration extends PluginIntegration<BukkitDiscordSRV>
        implements PermissionDataProvider.Permissions,
                   PermissionDataProvider.Groups,
                   PermissionDataProvider.PrefixAndSuffix {

    private Permission permission;
    private Chat chat;
    private boolean permissionAsync;
    private boolean chatAsync;

    public VaultIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public int priority(Class<?> type) {
        // Lower priority than default
        return -1;
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("net.milkbowl.vault.permission.Permission");
        } catch (ClassNotFoundException e) {
            return false;
        }

        return super.isEnabled();
    }

    @Override
    public void enable() {
        ServicesManager servicesManager = discordSRV.plugin().getServer().getServicesManager();

        RegisteredServiceProvider<Permission> permissionRSP = servicesManager.getRegistration(Permission.class);
        if (permissionRSP != null) {
            permission = permissionRSP.getProvider();
            permissionAsync = isAsync(permission);
        }

        RegisteredServiceProvider<Chat> chatRSP = servicesManager.getRegistration(Chat.class);
        if (chatRSP != null) {
            chat = chatRSP.getProvider();
            chatAsync = isAsync(chat);
        }
    }

    private boolean isAsync(Object vault) {
        if (vault == null) {
            return false;
        }
        return vault.getClass().getSimpleName().startsWith("LuckPerms");
    }

    @Override
    public void disable() {
        permission = null;
        chat = null;
    }

    @Override
    public boolean supportsOffline() {
        // Maybe
        return true;
    }

    private <T> CompletableFuture<T> unsupported() {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Vault backend unavailable"));
        return future;
    }

    private <T> CompletableFuture<T> supply(CheckedSupplier<T> supplier, boolean async) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Runnable runnable = () -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        };
        if (async) {
            discordSRV.scheduler().runFork(runnable);
        } else {
            discordSRV.scheduler().runOnMainThread(runnable);
        }
        return future;
    }

    private OfflinePlayer offlinePlayer(UUID player) {
        return discordSRV.plugin().getServer().getOfflinePlayer(player);
    }

    @Override
    public CompletableFuture<Boolean> hasGroup(UUID player, String groupName) {
        if (permission == null) {
            return unsupported();
        }

        return supply(() -> {
            OfflinePlayer offlinePlayer = offlinePlayer(player);
            return permission.playerInGroup(null, offlinePlayer, groupName);
        }, permissionAsync);
    }

    @Override
    public CompletableFuture<Void> addGroup(UUID player, String groupName) {
        if (permission == null) {
            return unsupported();
        }

        return supply(() -> {
            OfflinePlayer offlinePlayer = offlinePlayer(player);
            permission.playerAddGroup(null, offlinePlayer, groupName);
            return null;
        }, permissionAsync);
    }

    @Override
    public CompletableFuture<Void> removeGroup(UUID player, String groupName) {
        if (permission == null) {
            return unsupported();
        }

        return supply(() -> {
            OfflinePlayer offlinePlayer = offlinePlayer(player);
            permission.playerRemoveGroup(null, offlinePlayer, groupName);
            return null;
        }, permissionAsync);
    }

    @Override
    public CompletableFuture<Boolean> hasPermission(UUID player, String permissionNode) {
        if (permission == null) {
            return unsupported();
        }

        return supply(() -> {
            OfflinePlayer offlinePlayer = offlinePlayer(player);
            return permission.playerHas(null, offlinePlayer, permissionNode);
        }, permissionAsync);
    }

    @Override
    public CompletableFuture<String> getPrefix(UUID player) {
        if (chat == null) {
            return unsupported();
        }

        return supply(() -> {
            OfflinePlayer offlinePlayer = offlinePlayer(player);
            return chat.getPlayerPrefix(null, offlinePlayer);
        }, chatAsync);
    }

    @Override
    public CompletableFuture<String> getSuffix(UUID player) {
        if (chat == null) {
            return unsupported();
        }

        return supply(() -> {
            OfflinePlayer offlinePlayer = offlinePlayer(player);
            return chat.getPlayerSuffix(null, offlinePlayer);
        }, chatAsync);
    }
}
