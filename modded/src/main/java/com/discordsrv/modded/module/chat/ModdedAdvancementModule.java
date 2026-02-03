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

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.message.preprocess.game.AdvancementMessagePreProcessEvent;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.modded.ModdedDiscordSRV;
import com.discordsrv.modded.module.AbstractModdedModule;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ModdedAdvancementModule extends AbstractModdedModule {

    private static ModdedAdvancementModule instance;
    private final ModdedDiscordSRV discordSRV;

    public ModdedAdvancementModule(ModdedDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "ADVANCEMENT_MODULE"));
        this.discordSRV = discordSRV;
        instance = this;

        //? if neoforge {
        /*net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.AdvancementEvent event) -> onGrant(event.getAdvancement(), (ServerPlayer) event.getEntity()));
        *///?}
    }

    //? if minecraft: <1.20.2 {
    /*public static void onGrant(Advancement advancement, ServerPlayer owner) {
    *///?} else {
    public static void onGrant(net.minecraft.advancements.AdvancementHolder advancementEntry, ServerPlayer owner) {
    //?}
        if (instance == null || !instance.enabled) return;

        ModdedDiscordSRV discordSRV = instance.discordSRV;
        //? if minecraft: <1.20.2 {
        /*DisplayInfo display = advancement.getDisplay();
         *///?} else {
        Advancement advancement = advancementEntry.value();
        DisplayInfo display = advancement.display().orElse(null);
        //?}

        if (display == null || !display.shouldAnnounceChat()) {
            instance.logger().trace("Skipping advancement display of \"" + (advancement) + "\" for "
                    + owner + ": advancement display == null or does not broadcast to chat");
            return;
        }

        //? if minecraft: <=1.20.2 {
        /*String frameName = display.getFrame().toString();
        *///?} else {
        String frameName = display != null ? display.getType().getSerializedName() : "";
         //?}

        //? if minecraft: <1.20.3 {
        /*Component rawChat = Component.translatable("chat.type.advancement." + frameName, owner.getDisplayName(), display.getTitle());
        *///?} else {
        Component rawChat = display.getType().createAnnouncement(advancementEntry, owner);
        //?}

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
