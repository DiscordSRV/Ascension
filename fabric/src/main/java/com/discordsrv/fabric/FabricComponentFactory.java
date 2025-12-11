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
import com.discordsrv.common.core.component.ComponentFactory;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

//? if adventure: <6 {
/*import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.platform.fabric.AdventureCommandSourceStack;
 *///?} else {
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.platform.modcommon.AdventureCommandSourceStack;
//?}

//? if minecraft: >=1.21.9 {
public class FabricComponentFactory extends ComponentFactory implements net.minecraft.server.packs.resources.PreparableReloadListener {
//?} else {
/*public class FabricComponentFactory extends ComponentFactory implements net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener {
    @Override
    public net.minecraft.resources.ResourceLocation getFabricId() {
        return IDENTIFIER;
    }
*///?}

    public static final net.minecraft.resources.ResourceLocation IDENTIFIER = FabricDiscordSRV.id("discordsrv", "component_factory");

    //? if adventure: <6 {
    /*private final FabricServerAudiences adventure;
     *///?} else {
    private final MinecraftServerAudiences adventure;
    //?}
    private final FabricDiscordSRV discordSRV;

    public FabricComponentFactory(FabricDiscordSRV discordSRV) {
        super(discordSRV);
        //? if adventure: <6 {
        /*this.adventure = FabricServerAudiences.of(discordSRV.getServer());
         *///?} else {
        this.adventure = MinecraftServerAudiences.of(discordSRV.getServer());
        //?}
        this.discordSRV = discordSRV;
    }

    //? if adventure: <6 {
    /*public FabricServerAudiences getAdventure() {
        return adventure;
    }
    *///?} else {
    public MinecraftServerAudiences getAdventure() {
        return adventure;
    }
    //?}

    //? if adventure: <6 {
    /*@SuppressWarnings("removal")
    public Component fromNative(net.minecraft.network.chat.Component text) {
        return adventure.toAdventure(text);
    *///?} else {
    public Component fromNative(net.minecraft.network.chat.Component text) {
        return adventure.asAdventure(text);
    //?}
    }

    public Component toAdventure(net.minecraft.network.chat.Component text) {
        return fromNative(text);
    }

    public net.minecraft.network.chat.Component toNative(Component component) {
        //? if adventure: <6 {
         /*return adventure.toNative(component);
        *///?} else {
        return adventure.asNative(component);
        //?}
    }

    public net.minecraft.network.chat.Component fromAdventure(Component component) {
        return toNative(component);
    }

    public MinecraftComponent toAPI(Component component) {
        return ComponentUtil.toAPI(component);
    }

    public MinecraftComponent toAPI(net.minecraft.network.chat.Component text) {
        return toAPI(fromNative(text));
    }

    public AdventureCommandSourceStack audience(@NotNull CommandSourceStack source) {
        return adventure.audience(source);
    }

    public @NotNull Audience audience(@NotNull ServerPlayer source) {
        return adventure.audience(source);
    }

    public @NotNull Audience audience(@NotNull CommandSource source) {
        return adventure.audience(source);
    }

    public @NotNull Audience audience(@NotNull Iterable<ServerPlayer> players) {
        return adventure.audience(players);
    }

    @Override
    //? if minecraft: >=1.21.9 {
    public CompletableFuture<Void> reload(SharedState store, Executor prepareExecutor, PreparationBarrier preparationBarrier, Executor applyExecutor) {
        ResourceManager manager = store.resourceManager();
    //?} else if minecraft: >1.21.1 {
    /*public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager manager, Executor prepareExecutor, Executor applyExecutor) {

            *///?} else {
     /*public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager manager, ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
    *///?}
        return discordSRV
                .translationLoader()
                .reload(manager)
                .thenCompose(preparationBarrier::wait);
    }
}
