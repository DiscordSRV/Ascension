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

    public static final PermissionDynamicContextKey<String> STRING_ID = new PermissionDynamicContextKey<>(String.class, "ID", String::toString);
    public static final HashMap<Permission, PermissionNode<Boolean>> permissionNodes = new HashMap<>();

    public DiscordSRVNeoForgePermissionAPI() {
        collectPermissions();
    }

    private void collectPermissions() {
        // Trigger the static initializer of Permission and Permissions to ensure all permissions are registered before we collect them.
        Permission ignored = Permissions.COMMAND_BROADCAST;

        for (Permission permission : Permissions.allPermissions) {
            PermissionNode<Boolean> node = new PermissionNode<>(
                    "discordsrv",
                    permission.node() == null ? permission.strippedPermission() : permission.strippedPermission() + "." + permission.node(),
                    PermissionTypes.BOOLEAN,
                    //? if minecraft: >=1.21.11 {
                    (player, uuid, permissionDynamicContexts) -> !permission.requiresOpByDefault() || (player != null && player.hasPermission(new net.minecraft.server.permissions.Permission.HasCommandLevel(net.minecraft.server.permissions.PermissionLevel.byId(4)))),
                    //?} else {
                    /*(player, uuid, permissionDynamicContexts) -> !permission.requiresOpByDefault() || (player != null && player.server.getProfilePermissions(player.getGameProfile()) >= 4),
                    *///?}
                    STRING_ID
            );

            permissionNodes.put(permission, node);
        }
    }

    @SubscribeEvent
    public void onPermissionGather(PermissionGatherEvent.Nodes event) {
        event.addNodes(new ArrayList<>(permissionNodes.values()));
    }
}
