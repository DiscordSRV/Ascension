package com.discordsrv.bukkit.player;

import com.discordsrv.common.player.provider.model.SkinInfo;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerTextures;

import java.net.URL;
import java.util.Locale;

public final class SpigotPlayer {

    private SpigotPlayer() {}

    private static final boolean playerProfileExists;

    static {
        Class<?> playerClass = Player.class;

        boolean playerProfile = false;
        try {
            playerClass.getMethod("getPlayerProfile");
            playerProfile = true;
        } catch (ReflectiveOperationException ignored) {}
        playerProfileExists = playerProfile;
    }

    public static SkinInfo getSkinInfo(Player player) {
        if (!playerProfileExists) {
            return null;
        }

        PlayerTextures textures = player.getPlayerProfile().getTextures();

        URL skinUrl = textures.getSkin();
        if (skinUrl == null) {
            return null;
        }

        String skinUrlPlain = skinUrl.toString();
        return new SkinInfo(
                skinUrlPlain.substring(skinUrlPlain.lastIndexOf('/') + 1),
                textures.getSkinModel().toString().toLowerCase(Locale.ROOT)
        );
    }
}
