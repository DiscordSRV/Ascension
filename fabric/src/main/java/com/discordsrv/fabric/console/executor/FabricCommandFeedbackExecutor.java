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

package com.discordsrv.fabric.console.executor;

import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;
import java.util.function.Consumer;

public class FabricCommandFeedbackExecutor implements CommandOutput, Consumer<Component> {

    private final MinecraftServer server;
    private final Consumer<Component> componentConsumer;

    public FabricCommandFeedbackExecutor(MinecraftServer server, Consumer<Component> componentConsumer) {
        this.server = server;
        this.componentConsumer = componentConsumer;
    }

    public ServerCommandSource getCommandSource() {
        ServerWorld serverWorld = server.getOverworld();
        //? if minecraft: <1.19 {
        /*Text text = Text.of("DiscordSRV");
        *///?} else {
        Text text = Text.literal("DiscordSRV");
         //?}
        return new ServerCommandSource(
                this, serverWorld == null ? Vec3d.ZERO : Vec3d.of(serverWorld.getSpawnPos()), Vec2f.ZERO, serverWorld, 4, "DiscordSRV", text, server, null
        );
    }
    @Override
    //? if minecraft: <1.19 {
    /*public void sendSystemMessage(Text message, UUID sender) {
    *///?} else {
    public void sendMessage(Text message) {
     //?}
            accept(Component.text(Formatting.strip(message.getString())));
    }

    @Override
    public void accept(Component component) {
        componentConsumer.accept(component);
    }

    @Override
    public boolean shouldReceiveFeedback() {
        return true;
    }

    @Override
    public boolean shouldTrackOutput() {
        return true;
    }

    @Override
    public boolean shouldBroadcastConsoleToOps() {
        return true;
    }
}
