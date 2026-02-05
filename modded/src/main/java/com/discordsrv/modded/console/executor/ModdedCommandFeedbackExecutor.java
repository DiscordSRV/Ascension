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

package com.discordsrv.modded.console.executor;

import net.kyori.adventure.text.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.function.Consumer;

public class ModdedCommandFeedbackExecutor implements CommandSource, Consumer<Component> {

    private final MinecraftServer server;
    private final Consumer<Component> componentConsumer;

    public ModdedCommandFeedbackExecutor(MinecraftServer server, Consumer<Component> componentConsumer) {
        this.server = server;
        this.componentConsumer = componentConsumer;
    }

    @Override
    //? if minecraft: <1.19 {
    /*public void sendSystemMessage(Text message, UUID sender) {
    *///?} else {
    public void sendSystemMessage(net.minecraft.network.chat.Component message) {
     //?}
            accept(Component.text(ChatFormatting.stripFormatting(message.getString())));
    }

    @Override
    public void accept(Component component) {
        componentConsumer.accept(component);
    }

    @Override
    public boolean acceptsSuccess() {
        return true;
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public boolean shouldInformAdmins() {
        return true;
    }
}
