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

package com.discordsrv.fabric.module.chat;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.message.receive.game.AwardMessageReceiveEvent;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.fabric.module.AbstractFabricModule;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Optional;

public class FabricAdvancementModule extends AbstractFabricModule {

    private static FabricAdvancementModule instance;
    private final FabricDiscordSRV discordSRV;

    public FabricAdvancementModule(FabricDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "ADVANCEMENT_MODULE"));
        this.discordSRV = discordSRV;
        instance = this;
    }

    public static void onGrant(AdvancementEntry advancementEntry, ServerPlayerEntity owner) {
        if (instance == null || !instance.enabled) return;

        FabricDiscordSRV discordSRV = instance.discordSRV;
        Advancement advancement = advancementEntry.value();

        Optional<AdvancementDisplay> displayOptional = advancement.display();
        if (displayOptional.isEmpty() || !displayOptional.get().shouldAnnounceToChat()) {
            instance.logger().trace("Skipping advancement display of \"" + (advancement.name().isPresent() ? advancement.name().get() : advancement) + "\" for "
                    + owner + ": advancement display == null or does not broadcast to chat");
            return;
        }

        AdvancementDisplay display = displayOptional.get();
        AdvancementFrame frame = display.getFrame();

        MinecraftComponent name = ComponentUtil.toAPI(discordSRV.getAdventure().asAdventure(display.getTitle()));
        MinecraftComponent title = ComponentUtil.toAPI(discordSRV.getAdventure().asAdventure(frame.getChatAnnouncementText(advancementEntry, owner)));
        MinecraftComponent description = ComponentUtil.toAPI(discordSRV.getAdventure().asAdventure(display.getDescription()));

        IPlayer player = discordSRV.playerProvider().player(owner);
        discordSRV.eventBus().publish(
                new AwardMessageReceiveEvent(
                        null,
                        player,
                        name,
                        title,
                        description,
                        AwardMessageReceiveEvent.AdvancementFrame.valueOf(frame.toString()),
                        null,
                        false
                )
        );
    }
}
