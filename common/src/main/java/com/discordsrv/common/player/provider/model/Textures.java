package com.discordsrv.common.player.provider.model;

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
    public SkinInfo getSkinInfo() {
        Textures.Texture texture = textures.get("SKIN");
        if (texture == null) {
            return null;
        }

        String url = texture.url;
        Map<String, Object> metadata = texture.metadata;

        String textureId = url.substring(url.lastIndexOf("/") + 1);
        return new SkinInfo(textureId, metadata != null ? (String) metadata.get("model") : null);
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
