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
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.fabric.FabricDiscordSRV;
import com.discordsrv.fabric.module.AbstractFabricModule;
import net.kyori.adventure.text.Component;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.server.network.ServerPlayerEntity;
//? if minecraft: >=1.20.2
/*import net.minecraft.advancement.AdvancementEntry;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
*/

public class FabricAdvancementModule extends AbstractFabricModule {
    private static FabricAdvancementModule instance;
    private final FabricDiscordSRV discordSRV;

    public FabricAdvancementModule(FabricDiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
        instance = this;
    }

    //?if minecraft: <1.20.2 {
    public static void onGrant(Advancement advancement, ServerPlayerEntity owner) {
    //?} else {
    /*public static void onGrant(AdvancementEntry advancementEntry, ServerPlayerEntity owner) {
    *///?}
        if (instance == null || !instance.enabled) return;

        FabricDiscordSRV discordSRV = instance.discordSRV;
        //? if minecraft: <1.20.2 {
        AdvancementDisplay display = advancement.getDisplay();
        //?} else {
        /*Advancement advancement = advancementEntry.value();
        AdvancementDisplay display = advancement.display().get();
        *///?}

        if (display == null || !display.shouldAnnounceToChat()) return; // Usually a crafting recipe.
        //? if adventure: <6 {
        @SuppressWarnings("removal")
        Component component = discordSRV.getAdventure().toAdventure(display.getTitle());
        //?} else {
        /*Component component = discordSRV.getAdventure().asAdventure(display.getTitle());
         *///?}
        MinecraftComponent advancementTitle = ComponentUtil.toAPI(component);

        IPlayer player = discordSRV.playerProvider().player(owner);
        discordSRV.eventBus().publish(
                new AwardMessageReceiveEvent(
                        null,
                        player,
                        advancementTitle,
                        null,
                        null,
                        false
                )
        );
    }
}
