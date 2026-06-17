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

import com.discordsrv.common.permission.game.PermissionTemplate;
import com.discordsrv.common.permission.game.Permissions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContextKey;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;

public class DiscordSRVNeoForgePermissionAPI {

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    private final DiscordSRVNeoForgeBootstrap bootstrap;
    public static final HashMap<PermissionTemplate, NeoForgePermission> permissionNodes = new LinkedHashMap<>();

    public DiscordSRVNeoForgePermissionAPI(DiscordSRVNeoForgeBootstrap bootstrap) {
        this.bootstrap = bootstrap;
        collectPermissions();
    }

    private void collectPermissions() {
        for (PermissionTemplate permissionTemplate : Permissions.getAllTemplates()) {
            PermissionDynamicContextKey<String> dynamicContextKey = null;
            if (permissionTemplate instanceof PermissionTemplate.Parameterized parameterizedTemplate) {
                dynamicContextKey = new PermissionDynamicContextKey<>(String.class, parameterizedTemplate.parameterName(), String::toString);
            }

            PermissionNode<Boolean> node = new PermissionNode<>(
                    "discordsrv",
                    permissionTemplate.permissionNode(),
                    PermissionTypes.BOOLEAN,
                    //? if minecraft: >=1.21.11 {
                    (player, uuid, permissionDynamicContexts) -> !permissionTemplate.requiresOpByDefault() || (player != null && player.permissions().hasPermission(new net.minecraft.server.permissions.Permission.HasCommandLevel(net.minecraft.server.permissions.PermissionLevel.byId(4)))),
                    //?} else if minecraft: >=1.21.9 {
                    /*(player, uuid, permissionDynamicContexts) -> !permissionTemplate.requiresOpByDefault() || (player != null && bootstrap.getServer().getProfilePermissions(new net.minecraft.server.players.NameAndId(player.getGameProfile())) >= 4),
                    *///?} else {
                    /*(player, uuid, permissionDynamicContexts) -> !permissionTemplate.requiresOpByDefault() || (player != null && bootstrap.getServer().getProfilePermissions(player.getGameProfile()) >= 4),
                    *///?}
                    dynamicContextKey != null ? new PermissionDynamicContextKey[]{ dynamicContextKey } : new PermissionDynamicContextKey[0]
            );

            permissionNodes.put(permissionTemplate, new NeoForgePermission(node, dynamicContextKey));
        }
    }

    @SubscribeEvent
    public void onPermissionGather(PermissionGatherEvent.Nodes event) {
        List<PermissionNode<?>> allPermissions = new ArrayList<>(permissionNodes.size());
        for (NeoForgePermission neoForgePermission : permissionNodes.values()) {
            allPermissions.add(neoForgePermission.node());
        }
        event.addNodes(allPermissions);
    }

    public record NeoForgePermission(PermissionNode<Boolean> node, PermissionDynamicContextKey<String> dynamicContextKey) {}
}
