package com.discordsrv.bukkit.player;

import org.bukkit.entity.Player;

import java.util.Locale;

public final class PaperPlayer {

    private PaperPlayer() {}

    private static final boolean localeMethodExists;
    private static final boolean getLocaleMethodExists;

    static {
        Class<?> playerClass = Player.class;

        boolean locale = false, getLocale = false;
        try {
            playerClass.getMethod("locale");
            locale = true;
        } catch (ReflectiveOperationException ignored) {}
        try {
            playerClass.getMethod("getLocale");
            getLocale = true;
        } catch (ReflectiveOperationException ignored) {}
        localeMethodExists = locale;
        getLocaleMethodExists = getLocale;
    }

    @SuppressWarnings("deprecation")
    public static Locale getLocale(Player player) {
        if (localeMethodExists) {
            return player.locale();
        } else if (getLocaleMethodExists) {
            return Locale.forLanguageTag(player.getLocale());
        } else {
            return null;
        }
    }
}
