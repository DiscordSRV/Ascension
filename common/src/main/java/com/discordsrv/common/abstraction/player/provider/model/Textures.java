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

import com.discordsrv.common.DiscordSRV;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.Map;

public class Textures {

    public static String KEY = "textures";

    public String profileId;
    public String profileName;
    public boolean signatureRequired;
    public Map<String, Texture> textures;

    public static class Texture {
        public String url;
        public Map<String, Object> metadata;
    }
    public SkinInfo getSkinInfo(SkinInfo.Parts parts) {
        Textures.Texture texture = textures.get("SKIN");
        if (texture == null) {
            return null;
        }

        String url = texture.url;
        Map<String, Object> metadata = texture.metadata;

        String textureId = url.substring(url.lastIndexOf("/") + 1);
        return new SkinInfo(textureId, metadata != null ? (String) metadata.get("model") : null, parts);
    }

    public static Textures getFromBase64(DiscordSRV discordSRV, String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        Textures textures;
        try {
            textures = discordSRV.json().readValue(bytes, Textures.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return textures;
    }
}
