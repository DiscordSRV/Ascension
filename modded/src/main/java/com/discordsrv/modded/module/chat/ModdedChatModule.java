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

package com.discordsrv.modded.module.chat;

import com.discordsrv.api.events.message.preprocess.game.GameChatMessagePreProcessEvent;
import com.discordsrv.common.feature.channel.global.GlobalChannel;
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.modded.ModdedDiscordSRV;
import com.discordsrv.modded.module.AbstractModdedModule;
import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.function.Consumer;

public class ModdedChatModule extends AbstractModdedModule {

    private final ModdedDiscordSRV discordSRV;
    private static ModdedChatModule INSTANCE;

    public static void withInstance(Consumer<ModdedChatModule> consumer) {
        if (INSTANCE != null && INSTANCE.enabled) {
            consumer.accept(INSTANCE);
        }
    }
    public ModdedChatModule(ModdedDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
        INSTANCE = this;
    }

    //? if minecraft: <=1.19.2 {
    /*public void onChatMessage(net.minecraft.network.chat.ChatMessageContent content, UUID uuid) {
        onChatMessage(content.decorated(), uuid);
    }*///?}

    public void onChatMessage(String message, UUID uuid) {
        onChatMessage(net.minecraft.network.chat.Component.nullToEmpty(message), uuid);
    }

    public void onChatMessage(net.minecraft.network.chat.Component text, UUID uuid) {
        if (!enabled) return;
        ModdedDiscordSRV discordSRV = INSTANCE.discordSRV;

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
