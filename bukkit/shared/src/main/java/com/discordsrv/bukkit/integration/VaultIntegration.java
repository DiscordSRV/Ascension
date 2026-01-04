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

package com.discordsrv.bukkit.integration;

import com.discordsrv.api.module.type.PermissionModule;
import com.discordsrv.api.task.Task;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.core.module.type.PluginIntegration;
import com.discordsrv.common.exception.MessageException;
import com.discordsrv.common.util.function.CheckedSupplier;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class VaultIntegration extends PluginIntegration<BukkitDiscordSRV> implements PermissionModule.Basic {

    private Permission permission;
    private Chat chat;
    private boolean permissionAsync;
    private boolean chatAsync;

    public VaultIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public @NotNull String getIntegrationId() {
        return "Vault";
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

    private <T> Task<T> unsupported(@Nullable Object vault) {
        return Task.failed(new MessageException(
                vault != null
                ? "Vault backend " + vault.getClass().getName() + " unable to complete request"
                : "No vault backend available"
        ));
    }

    private <T> Task<T> supply(CheckedSupplier<T> supplier, boolean async) {
        if (async) {
            return discordSRV.scheduler().supply(supplier);
        } else {
            return discordSRV.scheduler().supplyOnMainThread(supplier);
        }
    }

    private OfflinePlayer offlinePlayer(UUID player) {
        return discordSRV.plugin().getServer().getOfflinePlayer(player);
    }

    @Override
    public List<String> getGroups() {
        String[] groups = permission.getGroups();
        return groups != null ? Arrays.asList(groups) : Collections.emptyList();
    }

    @Override
    public Task<Boolean> hasGroup(@NotNull UUID player, @NotNull String groupName, boolean includeInherited) {
        if (permission == null || !permission.isEnabled() || !permission.hasGroupSupport()) {
            return unsupported(permission);
        }

        return supply(() -> {
            OfflinePlayer offlinePlayer = offlinePlayer(player);
            return permission.playerInGroup(null, offlinePlayer, groupName);
        }, permissionAsync);
    }

    @Override
    public Task<Void> addGroup(@NotNull UUID player, @NotNull String groupName) {
        if (permission == null || !permission.isEnabled() || !permission.hasGroupSupport()) {
            return unsupported(permission);
        }

        return supply(() -> {
            OfflinePlayer offlinePlayer = offlinePlayer(player);
            permission.playerAddGroup(null, offlinePlayer, groupName);
            return null;
        }, permissionAsync);
    }

    @Override
    public Task<Void> removeGroup(@NotNull UUID player, @NotNull String groupName) {
        if (permission == null || !permission.isEnabled() || !permission.hasGroupSupport()) {
            return unsupported(permission);
        }

        return supply(() -> {
            OfflinePlayer offlinePlayer = offlinePlayer(player);
            permission.playerRemoveGroup(null, offlinePlayer, groupName);
            return null;
        }, permissionAsync);
    }

    @Override
    public Task<String> getPrimaryGroup(@NotNull UUID player) {
        if (permission == null || !permission.isEnabled() || !permission.hasGroupSupport()) {
            return unsupported(permission);
        }

        return supply(() -> {
            OfflinePlayer offlinePlayer = offlinePlayer(player);
            return permission.getPrimaryGroup(null, offlinePlayer);
        }, permissionAsync);
    }

    @Override
    public Task<Boolean> hasPermission(@NotNull UUID player, @NotNull String permissionNode) {
        if (permission == null || !permission.isEnabled()) {
            return unsupported(permission);
        }

        return supply(() -> {
            OfflinePlayer offlinePlayer = offlinePlayer(player);
            return permission.playerHas(null, offlinePlayer, permissionNode);
        }, permissionAsync);
    }

    @Override
    public Task<String> getPrefix(@NotNull UUID player) {
        if (chat == null || !chat.isEnabled()) {
            return unsupported(chat);
        }

        return supply(() -> {
            OfflinePlayer offlinePlayer = offlinePlayer(player);
            return chat.getPlayerPrefix(null, offlinePlayer);
        }, chatAsync);
    }

    @Override
    public Task<String> getSuffix(@NotNull UUID player) {
        if (chat == null || !chat.isEnabled()) {
            return unsupported(chat);
        }

        return supply(() -> {
            OfflinePlayer offlinePlayer = offlinePlayer(player);
            return chat.getPlayerSuffix(null, offlinePlayer);
        }, chatAsync);
    }
}
