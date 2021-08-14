/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.component;

import com.discordsrv.api.component.EnhancedTextBuilder;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.component.MinecraftComponentFactory;
import com.discordsrv.common.DiscordSRV;
import org.jetbrains.annotations.NotNull;

public class ComponentFactory implements MinecraftComponentFactory {

    private final DiscordSRV discordSRV;

    public ComponentFactory(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public @NotNull MinecraftComponent empty() {
        return MinecraftComponentImpl.empty();
    }

    @Override
    public EnhancedTextBuilder enhancedBuilder(String content) {
        return new EnhancedTextBuilderImpl(discordSRV, content);
    }
}
