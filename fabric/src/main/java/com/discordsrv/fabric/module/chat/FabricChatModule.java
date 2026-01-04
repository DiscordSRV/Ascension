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

package com.discordsrv.fabric.module.chat;

import com.discordsrv.api.events.message.preprocess.game.GameChatMessagePreProcessEvent;
import com.discordsrv.common.feature.channel.global.GlobalChannel;
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.fabric.module.AbstractFabricModule;
import net.kyori.adventure.text.Component;

import java.util.UUID;

public class FabricChatModule extends AbstractFabricModule {

    private static FabricChatModule instance;
    private final FabricDiscordSRV discordSRV;

    public FabricChatModule(FabricDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
        instance = this;
    }

    //? if minecraft: <=1.19.2 {
    /*public static void onChatMessage(net.minecraft.network.chat.ChatMessageContent content, UUID uuid) {
        onChatMessage(content.decorated(), uuid);
    }
    *///?} else {
    public static void onChatMessage(String message, UUID uuid) {
        onChatMessage(net.minecraft.network.chat.Component.nullToEmpty(message), uuid);
    }
    //?}

    public static void onChatMessage(net.minecraft.network.chat.Component text, UUID uuid) {
        if (instance == null || !instance.enabled) return;
        FabricDiscordSRV discordSRV = instance.discordSRV;

        Component component = discordSRV.componentFactory().fromNative(text);
        discordSRV.eventBus().publish(new GameChatMessagePreProcessEvent(
                null,
                discordSRV.playerProvider().player(uuid),
                ComponentUtil.toAPI(component),
                new GlobalChannel(discordSRV),
                false
        ));
    }
}
