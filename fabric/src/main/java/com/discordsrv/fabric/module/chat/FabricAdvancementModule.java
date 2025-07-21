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
import com.discordsrv.api.events.message.preprocess.game.AwardMessagePreProcessEvent;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.fabric.module.AbstractFabricModule;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.text.Text;
import net.minecraft.server.network.ServerPlayerEntity;

public class FabricAdvancementModule extends AbstractFabricModule {

    private static FabricAdvancementModule instance;
    private final FabricDiscordSRV discordSRV;

    public FabricAdvancementModule(FabricDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "ADVANCEMENT_MODULE"));
        this.discordSRV = discordSRV;
        instance = this;
    }

    //? if minecraft: <1.20.2 {
    /*public static void onGrant(Advancement advancement, ServerPlayerEntity owner) {
    *///?} else {
    public static void onGrant(net.minecraft.advancement.AdvancementEntry advancementEntry, ServerPlayerEntity owner) {
    //?}
        if (instance == null || !instance.enabled) return;

        FabricDiscordSRV discordSRV = instance.discordSRV;
        //? if minecraft: <1.20.2 {
        /*AdvancementDisplay display = advancement.getDisplay();
         *///?} else {
        Advancement advancement = advancementEntry.value();
        AdvancementDisplay display = advancement.display().orElse(null);
        //?}

        if (display == null || !display.shouldAnnounceToChat()) {
            instance.logger().trace("Skipping advancement display of \"" + (advancement) + "\" for "
                    + owner + ": advancement display == null or does not broadcast to chat");
            return;
        }

        AdvancementFrame frame = display.getFrame();

        //? if minecraft: <1.20.3 {
        /*Text rawChat = Text.translatable("chat.type.advancement." + frame.getId(), owner.getDisplayName(), display.getTitle());
        *///?} else {
        Text rawChat = frame.getChatAnnouncementText(advancementEntry, owner);
        //?}
        Text rawTitle = display.getTitle();
        Text rawDesc  = display.getDescription();

        //? if adventure: <6 {
        /*@SuppressWarnings("removal")
        MinecraftComponent message = ComponentUtil.toAPI(discordSRV.getAdventure().toAdventure(rawChat));
        @SuppressWarnings("removal")
        MinecraftComponent title = ComponentUtil.toAPI(discordSRV.getAdventure().toAdventure(rawTitle));
        @SuppressWarnings("removal")
        MinecraftComponent description = ComponentUtil.toAPI(discordSRV.getAdventure().toAdventure(rawDesc));
        *///?} else {
        MinecraftComponent message = ComponentUtil.toAPI(discordSRV.getAdventure().asAdventure(rawChat));
        MinecraftComponent title = ComponentUtil.toAPI(discordSRV.getAdventure().asAdventure(rawTitle));
        MinecraftComponent description = ComponentUtil.toAPI(discordSRV.getAdventure().asAdventure(rawDesc));
        //?}

        IPlayer player = discordSRV.playerProvider().player(owner);
        discordSRV.eventBus().publish(
                new AwardMessagePreProcessEvent(
                        null,
                        player,
                        message,
                        title,
                        description,
                        AwardMessagePreProcessEvent.AdvancementFrame.valueOf(frame.toString()),
                        null,
                        false
                )
        );
    }
}
