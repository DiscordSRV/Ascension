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

package com.discordsrv.fabric;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

//? if adventure: <6 {
/*import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.platform.fabric.AdventureCommandSourceStack;
 *///?} else {
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.platform.modcommon.AdventureCommandSourceStack;
//?}

public class FabricAdventureUtil {

    //? if adventure: <6 {
    /*private final FabricServerAudiences adventure;
     *///?} else {
    private final MinecraftServerAudiences adventure;
    //?}

    public FabricAdventureUtil(FabricDiscordSRV discordSRV) {
        //? if adventure: <6 {
        /*this.adventure = FabricServerAudiences.of(discordSRV.getServer());
         *///?} else {
        this.adventure = MinecraftServerAudiences.of(discordSRV.getServer());
        //?}
    }

    //? if adventure: <6 {
    /*public FabricServerAudiences getAdventure() {
        return adventure;
    }
    *///?} else {
    public MinecraftServerAudiences getAdventure() {
        return adventure;
    }//?}

    public Component fromNative(Text text) {
        //? if adventure: <6 {
        /*@SuppressWarnings("removal")
        return adventure.toAdventure(text);
        *///?} else {
        return adventure.asAdventure(text);
        //?}
    }

    public Component toAdventure(Text text) {
        return fromNative(text);
    }

    public Text toNative(Component component) {
        //? if adventure: <6 {
        // return adventure.toNative(component);
        //?} else {
        return adventure.asNative(component);
        //?}
    }

    public Text fromAdventure(Component component) {
        return toNative(component);
    }

    public MinecraftComponent toAPI(Component component) {
        return ComponentUtil.toAPI(component);
    }

    public MinecraftComponent toAPI(Text text) {
        return toAPI(fromNative(text));
    }

    public AdventureCommandSourceStack audience(@NotNull ServerCommandSource source) {
        return adventure.audience(source);
    }

    public @NotNull Audience audience(@NotNull ServerPlayerEntity source) {
        return adventure.audience(source);
    }

    public @NotNull Audience audience(@NotNull CommandOutput source) {
        return adventure.audience(source);
    }

    public @NotNull Audience audience(@NotNull Iterable<ServerPlayerEntity> players) {
        return adventure.audience(players);
    }
}
