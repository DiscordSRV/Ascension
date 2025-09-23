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
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.minecraft.resource.ResourceManager;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
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

public class FabricComponentFactory extends ComponentFactory implements IdentifiableResourceReloadListener {

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
    public Component fromNative(Text text) {
        return adventure.toAdventure(text);
    *///?} else {
    public Component fromNative(Text text) {
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

    @Override
    //? if minecraft: >=1.21.9 {
    public CompletableFuture<Void> reload(Store store, Executor prepareExecutor, Synchronizer synchronizer, Executor applyExecutor) {
        ResourceManager manager = store.getResourceManager();
    //?} else if minecraft: >1.21.1 {
    //public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager, Executor prepareExecutor, Executor applyExecutor) {
    //?} else {
     /*public CompletableFuture<Void> reload(Synchronizer synchronizer, ResourceManager manager, Profiler prepareProfiler, Profiler applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
    *///?}
        return discordSRV
                .translationLoader()
                .reload(manager)
                .thenCompose(synchronizer::whenPrepared);
    }

    @Override
    public Identifier getFabricId() {
        return FabricDiscordSRV.id("discordsrv", "component_factory");
    }
}
