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

package com.discordsrv.modded.command.game.sender;

import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.permission.game.Permission;
import com.discordsrv.modded.ModdedDiscordSRV;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.kyori.adventure.audience.Audience;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ModdedCommandSender implements ICommandSender {

    protected final ModdedDiscordSRV discordSRV;
    protected CommandSourceStack commandSource;

    public ModdedCommandSender(ModdedDiscordSRV discordSRV, CommandSourceStack commandSource) {
        this.discordSRV = discordSRV;
        this.commandSource = commandSource;

        if (commandSource == null) { // Register for when the server fully starts up
            //? if fabric
            ServerLifecycleEvents.SERVER_STARTED.register(server -> this.commandSource = getCommandSource(server, "DiscordSRV"));

            //? if neoforge
            //net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent event) -> this.commandSource = getCommandSource(event.getServer(), "DiscordSRV"));
        }
    }

    @Override
    public boolean hasPermission(Permission permission) {
        int defaultLevel = permission.requiresOpByDefault() ? 4 : 0;

        //? if fabric
        return me.lucko.fabric.api.permissions.v0.Permissions.check(commandSource, permission.permission(), defaultLevel);

        //? if neoforge {
        /*//? if minecraft: >=1.21.11 {
        return commandSource.permissions().hasPermission(new net.minecraft.server.permissions.Permission.HasCommandLevel(net.minecraft.server.permissions.PermissionLevel.byId(defaultLevel)));
         //?} else {
        /^return commandSource.hasPermission(4);
        ^///?}
        *///?}
    }

    @Override
    public void runCommand(String command) {
        //? if minecraft: <1.19 {
        /*discordSRV.getServer().getCommandManager().execute(commandSource, command);
        *///?} else {
        discordSRV.getServer().getCommands().performPrefixedCommand(commandSource, command);
        //?}
    }

    @Override
    public @NotNull Audience audience() {
        return discordSRV.componentFactory().audience(commandSource);
    }

    public static CommandSourceStack getCommandSource(MinecraftServer server, String name) {
        return getCommandSource(server, server, name);
    }

    public static CommandSourceStack getCommandSource(MinecraftServer server, CommandSource source, String name) {
        ServerLevel level = server.overworld();
        //? if minecraft: <1.19 {
        //Text text = Text.of(name);
        //?} else {
        net.minecraft.network.chat.Component text = net.minecraft.network.chat.Component.literal(name);
        //?}

        //? if minecraft: >=1.21.9 {
        Vec3 spawnPos = level == null ? Vec3.ZERO : level.getRespawnData().pos().getCenter();
        //?} else {
        /*Vec3 spawnPos = Vec3.atLowerCornerOf(level.getSharedSpawnPos());
         *///?}

        //? if minecraft: >=1.21.11 {
        net.minecraft.server.permissions.PermissionSet permissionSet = net.minecraft.server.permissions.PermissionSet.ALL_PERMISSIONS;
        //?} else {
        /*int permissionSet = 4;
         *///?}

        return new CommandSourceStack(
                source, spawnPos, Vec2.ZERO, level, permissionSet, name, text, server, null
        );
    }
}
