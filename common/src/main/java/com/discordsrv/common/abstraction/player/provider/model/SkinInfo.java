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

package com.discordsrv.common.abstraction.player.provider.model;

import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;

import java.net.URL;

@PlaceholderPrefix("skin_")
public class SkinInfo {

    private final String textureId;
    private final String model;

    public SkinInfo(URL textureUrl, String model) {
        String textureUrlPlain = textureUrl.toString();
        this.textureId = textureUrlPlain.substring(textureUrlPlain.lastIndexOf('/') + 1);
        this.model = model;
    }

    public SkinInfo(String textureId, String model) {
        this.textureId = textureId;
        this.model = model;
    }

    @Placeholder("texture_id")
    public String textureId() {
        return textureId;
    }

    @Placeholder("model")
    public String model() {
        return model;
    }
}
