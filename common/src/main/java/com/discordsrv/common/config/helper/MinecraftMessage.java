/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.config.helper;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.api.component.GameTextBuilder;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.component.util.ComponentUtil;
import net.kyori.adventure.text.Component;

public class MinecraftMessage {

    private final String rawFormat;

    public MinecraftMessage(String rawFormat) {
        this.rawFormat = rawFormat;
    }

    public String rawFormat() {
        return rawFormat;
    }

    public GameTextBuilder textBuilder() {
        return DiscordSRVApi.get().componentFactory().textBuilder(rawFormat);
    }

    public MinecraftComponent make() {
        return textBuilder().build();
    }

    public Component asComponent() {
        return ComponentUtil.fromAPI(make());
    }
}
