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
import com.discordsrv.fabric.command.game.sender.FabricCommandSender;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.minecraft.network.packet.s2c.play.ChatSuggestionsS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

public class FabricPlayer extends FabricCommandSender implements IPlayer {

    private final ServerPlayerEntity player;

    public FabricPlayer(FabricDiscordSRV discordSRV, ServerPlayerEntity player) {
        super(discordSRV, player.getCommandSource());
        this.player = player;
    }

    @Override
    public DiscordSRV discordSRV() {
        return discordSRV;
    }

    @Override
    public @NotNull String username() {
        return player.getName().getString();
    }

    @Override
    public @Nullable Locale locale() {
        return Locale.of(player.getClientOptions().language());
    }

    @Override
    public boolean isVanished() {
        return false;
    }

    @Override
    public Task<Void> kick(Component component) {
        player.networkHandler.disconnect(discordSRV.getAdventure().asNative(component));
        return Task.completed(null);
    }

    @Override
    public void addChatSuggestions(Collection<String> suggestions) {
        ChatSuggestionsS2CPacket packet = new ChatSuggestionsS2CPacket(ChatSuggestionsS2CPacket.Action.ADD, new ArrayList<>(suggestions));
        player.networkHandler.sendPacket(packet);
    }

    @Override
    public void removeChatSuggestions(Collection<String> suggestions) {
        ChatSuggestionsS2CPacket packet = new ChatSuggestionsS2CPacket(ChatSuggestionsS2CPacket.Action.REMOVE, new ArrayList<>(suggestions));
        player.networkHandler.sendPacket(packet);
    }

    @Override
    public @Nullable SkinInfo skinInfo() {
        MinecraftProfileTextures textures = discordSRV.getServer().getSessionService().getTextures(player.getGameProfile());
        if (!textures.equals(MinecraftProfileTextures.EMPTY) && textures.skin() != null) {
            String model = textures.skin().getMetadata("model");
            if (model == null) model = "classic";

            int playerModelParts = player.getClientOptions().playerModelParts();
            return new SkinInfo(textures.skin().getHash(), model, new SkinInfo.Parts(playerModelParts));
        }
        return null;
    }

    @Override
    public @NotNull Identity identity() {
        return player.identity();
    }

    @Override
    public @NotNull Component displayName() {
        // Use Adventure's Pointer, otherwise username
        return player.getOrDefaultFrom(
                Identity.DISPLAY_NAME,
                () -> discordSRV.getAdventure().asAdventure(player.getName())
        );
    }

    @Override
    public @NotNull Component teamDisplayName() {
        Team team = player.getScoreboardTeam();
        if (team == null) {
            return IPlayer.super.teamDisplayName();
        }

        return discordSRV.getAdventure().asAdventure(team.decorateName(player.getName()));
    }

    @Override
    public String toString() {
        return "FabricPlayer{" + username() + "}";
    }
}
