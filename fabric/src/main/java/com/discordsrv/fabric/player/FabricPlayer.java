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

package com.discordsrv.fabric.player;

import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.fabric.accessor.ServerPlayerEntityAccessor;
import com.discordsrv.fabric.command.game.sender.FabricCommandSender;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public class FabricPlayer extends FabricCommandSender implements IPlayer {

    private final ServerPlayer player;

    public FabricPlayer(FabricDiscordSRV discordSRV, ServerPlayer player) {
        super(discordSRV, player.createCommandSourceStack());
        this.player = player;
    }

    @Override
    public DiscordSRV discordSRV() {
        return discordSRV;
    }

    public @NotNull ServerPlayer getPlayer() {
        return player;
    }

    @Override
    public @NotNull String username() {
        return player.getName().getString();
    }

    @Override
    public @Nullable Locale locale() {
        return Locale.forLanguageTag(((ServerPlayerEntityAccessor) player).discordsrv$getLocale());
    }

    @Override
    public @NotNull String worldName() {
        //? if minecraft: >1.19.4 {
        return player.level().dimension().identifier().getPath();
        //?} else {
        /*return player.getLevel().dimension().identifier().getPath();
         *///?}
    }

    @Override
    public @NotNull String worldNamespace() {
        //? if minecraft: >1.19.4 {
        return player.level().dimension().identifier().getNamespace();
        //?} else {
        /*return player.getLevel().dimension().identifier().getNamespace();
         *///?}
    }

    @Override
    public Task<Void> kick(Component component) {
        player.connection.disconnect(discordSRV.componentFactory().toNative(component));
        return Task.completed(null);
    }

    @Override
    public void addChatSuggestions(Collection<String> suggestions) {
        //? if minecraft: >1.19 {
        net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket packet = new net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket(
                net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket.Action.ADD,
                new ArrayList<>(suggestions)
        );
        player.connection.send(packet);
        //?}
    }

    @Override
    public void removeChatSuggestions(Collection<String> suggestions) {
        //? if minecraft: >1.19 {
        net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket packet = new net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket(
                net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket.Action.REMOVE,
                new ArrayList<>(suggestions)
        );
        player.connection.send(packet);
        //?}
    }

    @Override
    public @Nullable SkinInfo skinInfo() {
        int playerModelParts = ((ServerPlayerEntityAccessor) player).discordsrv$getPlayerModelParts();

        //? if minecraft: >1.20.2 {
        //? if minecraft: >=1.21.9 {
        com.mojang.authlib.minecraft.MinecraftProfileTextures textures = discordSRV.getServer().services().sessionService().getTextures(player.getGameProfile());
        //?} else {
        /*com.mojang.authlib.minecraft.MinecraftProfileTextures textures = discordSRV.getServer().getSessionService().getTextures(player.getGameProfile());
        *///?}
        if (!textures.equals(com.mojang.authlib.minecraft.MinecraftProfileTextures.EMPTY) && textures.skin() != null) {
            String model = textures.skin().getMetadata("model");
            if (model == null) model = "classic";

            return new SkinInfo(textures.skin().getHash(), model, new SkinInfo.Parts(playerModelParts));
        }
        //?} else {
        /*java.util.Map<com.mojang.authlib.minecraft.MinecraftProfileTexture.Type, com.mojang.authlib.minecraft.MinecraftProfileTexture> texturesMap = discordSRV.getServer().getSessionService().getTextures(player.getGameProfile(), false);
        com.mojang.authlib.minecraft.MinecraftProfileTexture skinTexture = texturesMap.get(com.mojang.authlib.minecraft.MinecraftProfileTexture.Type.SKIN);
        String model;
        if (skinTexture != null) {
            model = skinTexture.getMetadata("model");
            if (model == null) model = "classic";

            return new SkinInfo(skinTexture.getHash(), model, new SkinInfo.Parts(playerModelParts));
        }
        *///?}
        return null;
    }

    @Override
    public @NotNull Identity identity() {
        //? if adventure: >=5.11.0 {
        return player.identity();
        //?} else {
        /*return Identity.identity(player.getUUID());
        *///?}
    }

    @Override
    public @NotNull Component displayName() {
        //? if adventure: >=5.3.0 {
        return player.getOrDefaultFrom(
                Identity.DISPLAY_NAME,
                () -> discordSRV.componentFactory().fromNative(player.getName())
        );
        //?} else {
        /*return Component.text(player.getName().getString());
        *///?}
    }

    @Override
    public @NotNull Component teamDisplayName() {
        PlayerTeam team = (PlayerTeam) player.getTeam();
        if (team == null) {
            return IPlayer.super.teamDisplayName();
        }

        return discordSRV.componentFactory().fromNative(team.getFormattedName(player.getName()));
    }

    @Override
    public boolean isChatVisible() {
        //? if minecraft: >1.20.1 {
        net.minecraft.world.entity.player.ChatVisiblity chatVisibility = player.clientInformation().chatVisibility();
        return chatVisibility != net.minecraft.world.entity.player.ChatVisiblity.SYSTEM
                && chatVisibility != net.minecraft.world.entity.player.ChatVisiblity.HIDDEN;
        //?} else {
        /*net.minecraft.world.entity.player.ChatVisiblity chatVisibility = player.getChatVisibility();
        return chatVisibility != net.minecraft.world.entity.player.ChatVisiblity.SYSTEM
                && chatVisibility != net.minecraft.world.entity.player.ChatVisiblity.HIDDEN;
        *///?}
    }

    @Override
    public String toString() {
        return "FabricPlayer{" + username() + "}";
    }
}
