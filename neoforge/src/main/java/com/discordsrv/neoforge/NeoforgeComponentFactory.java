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

package com.discordsrv.neoforge;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.core.component.ComponentFactory;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class NeoforgeComponentFactory extends ComponentFactory {

    private final net.kyori.adventure.platform.modcommon.MinecraftServerAudiences adventure;
    private final NeoforgeDiscordSRV discordSRV;

    public NeoforgeComponentFactory(NeoforgeDiscordSRV discordSRV) {
        super(discordSRV);

        this.adventure = net.kyori.adventure.platform.modcommon.MinecraftServerAudiences.of(discordSRV.getServer());
        this.discordSRV = discordSRV;
    }

    public net.kyori.adventure.platform.modcommon.MinecraftServerAudiences getAdventure() {
        return adventure;
    }

    public Component fromNative(net.minecraft.network.chat.Component text) {
        return adventure.asAdventure(text);
    }

    public Component toAdventure(net.minecraft.network.chat.Component text) {
        return fromNative(text);
    }

    public net.minecraft.network.chat.Component toNative(Component component) {
        return adventure.asNative(component);
    }

    public net.minecraft.network.chat.Component fromAdventure(Component component) {
        return toNative(component);
    }

    public MinecraftComponent toAPI(Component component) {
        return ComponentUtil.toAPI(component);
    }

    public MinecraftComponent toAPI(net.minecraft.network.chat.Component text) {
        return toAPI(fromNative(text));
    }


    public net.kyori.adventure.platform.modcommon.AdventureCommandSourceStack audience(@NotNull CommandSourceStack source) {
        return adventure.audience(source);
    }

    public @NotNull Audience audience(@NotNull ServerPlayer source) {
        return adventure.audience(source);
    }

    public @NotNull Audience audience(@NotNull CommandSource source) {
        return adventure.audience(source);
    }

    public @NotNull Audience audience(@NotNull Iterable<ServerPlayer> players) {
        return adventure.audience(players);
    }

}
