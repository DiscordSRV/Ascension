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

package com.discordsrv.neoforge;

import com.discordsrv.common.permission.game.Permission;
import com.discordsrv.common.permission.game.Permissions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContextKey;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.ArrayList;
import java.util.HashMap;

public class DiscordSRVNeoForgePermissionAPI {

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final DiscordSRVNeoForgeBootstrap bootstrap;
    public static final PermissionDynamicContextKey<String> STRING_ID = new PermissionDynamicContextKey<>(String.class, "id", String::toString);
    public static final HashMap<String, PermissionNode<Boolean>> permissionNodes = new HashMap<>();

    public DiscordSRVNeoForgePermissionAPI(DiscordSRVNeoForgeBootstrap bootstrap) {
        this.bootstrap = bootstrap;
        collectPermissions();
    }

    private void collectPermissions() {
        // Trigger the static initializer of Permission and Permissions to ensure all permissions are registered before we collect them.
        Permission ignored = Permissions.COMMAND_BROADCAST;

        for (Permission permission : Permissions.allPermissions) {
            PermissionNode<Boolean> node = new PermissionNode<>(
                    "discordsrv",
                    permission.strippedPermission(),
                    PermissionTypes.BOOLEAN,
                    //? if minecraft: >=1.21.11 {
                    (player, uuid, permissionDynamicContexts) -> !permission.requiresOpByDefault() || (player != null && player.permissions().hasPermission(new net.minecraft.server.permissions.Permission.HasCommandLevel(net.minecraft.server.permissions.PermissionLevel.byId(4)))),
                    //?} else if minecraft: >=1.21.9 {
                    /*(player, uuid, permissionDynamicContexts) -> !permission.requiresOpByDefault() || (player != null && bootstrap.getServer().getProfilePermissions(new net.minecraft.server.players.NameAndId(player.getGameProfile())) >= 4),
                    *///?} else {
                    /*(player, uuid, permissionDynamicContexts) -> !permission.requiresOpByDefault() || (player != null && bootstrap.getServer().getProfilePermissions(player.getGameProfile()) >= 4),
                    *///?}
                    STRING_ID
            );

            permissionNodes.put(permission.strippedPermission(), node);
        }
    }

    @SubscribeEvent
    public void onPermissionGather(PermissionGatherEvent.Nodes event) {
        event.addNodes(new ArrayList<>(permissionNodes.values()));
    }
}
