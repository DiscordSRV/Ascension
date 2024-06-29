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

package com.discordsrv.bukkit.player;

import com.discordsrv.common.component.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Locale;

@SuppressWarnings("JavaLangInvokeHandleSignature") // Unrelocate
public final class PaperPlayer {

    private PaperPlayer() {}

    private static final boolean LOCALE_METHOD_EXISTS;
    private static final boolean GETLOCALE_METHOD_EXISTS;
    private static final MethodHandle KICK_COMPONENT_HANDLE;

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
        LOCALE_METHOD_EXISTS = locale;
        GETLOCALE_METHOD_EXISTS = getLocale;

        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodHandle handle = null;
        try {
            handle = lookup.findVirtual(Player.class, "kick", MethodType.methodType(
                    void.class,
                    com.discordsrv.unrelocate.net.kyori.adventure.text.Component.class
            ));
        } catch (ReflectiveOperationException ignored) {}
        KICK_COMPONENT_HANDLE = handle;
    }

    @SuppressWarnings("deprecation")
    public static Locale getLocale(Player player) {
        if (LOCALE_METHOD_EXISTS) {
            return player.locale();
        } else if (GETLOCALE_METHOD_EXISTS) {
            return Locale.forLanguageTag(player.getLocale());
        } else {
            return null;
        }
    }

    public static boolean isKickAvailable() {
        return KICK_COMPONENT_HANDLE != null;
    }

    public static void kick(Player player, Component reason) {
        try {
            KICK_COMPONENT_HANDLE.invokeExact(player, ComponentUtil.toUnrelocated(ComponentUtil.toAPI(reason)));
        } catch (Throwable e) {
            throw new RuntimeException("Failed to kick player", e);
        }
    }
}
