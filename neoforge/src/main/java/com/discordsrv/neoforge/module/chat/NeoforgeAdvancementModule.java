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

package com.discordsrv.neoforge.module.chat;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.message.preprocess.game.AdvancementMessagePreProcessEvent;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.neoforge.NeoforgeDiscordSRV;
import com.discordsrv.neoforge.module.AbstractNeoforgeModule;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class NeoforgeAdvancementModule extends AbstractNeoforgeModule {

    private static NeoforgeAdvancementModule instance;
    private final NeoforgeDiscordSRV discordSRV;

    public NeoforgeAdvancementModule(NeoforgeDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "ADVANCEMENT_MODULE"));
        this.discordSRV = discordSRV;
        instance = this;
    }


    public static void onGrant(net.minecraft.advancements.AdvancementHolder advancementEntry, ServerPlayer owner) {
        if (instance == null || !instance.enabled) return;

        NeoforgeDiscordSRV discordSRV = instance.discordSRV;
        Advancement advancement = advancementEntry.value();
        DisplayInfo display = advancement.display().orElse(null);

        if (display == null || !display.shouldAnnounceChat()) {
            instance.logger().trace("Skipping advancement display of \"" + (advancement) + "\" for "
                    + owner + ": advancement display == null or does not broadcast to chat");
            return;
        }
        String frameName = display.getType().getSerializedName();
        Component rawChat = display.getType().createAnnouncement(advancementEntry, owner);

        MinecraftComponent message = discordSRV.componentFactory().toAPI(rawChat);
        MinecraftComponent title = discordSRV.componentFactory().toAPI(display.getTitle());
        MinecraftComponent description = discordSRV.componentFactory().toAPI(display.getDescription());

        IPlayer player = discordSRV.playerProvider().player(owner);
        discordSRV.eventBus().publish(
                new AdvancementMessagePreProcessEvent(
                        null,
                        player,
                        message,
                        title,
                        description,
                        AdvancementMessagePreProcessEvent.AdvancementFrame.fromId(frameName),
                        null,
                        false
                )
        );
    }
}
