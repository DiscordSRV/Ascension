/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.bukkit.component;

import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.unrelocate.net.kyori.adventure.text.flattener.ComponentFlattener;
import com.discordsrv.unrelocate.net.kyori.adventure.text.flattener.FlattenerListener;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Locale;

public final class PaperComponentFlattener {

    private static final ComponentFlattener FLATTENER = get();
    public static boolean IS_AVAILABLE = FLATTENER != null;

    private static ComponentFlattener get() {
        try {
            Class<?> paperComponentsClass = Class.forName("io.papermc.paper.text.PaperComponents");
            Method method = paperComponentsClass.getDeclaredMethod("flattener");
            return (ComponentFlattener) method.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private PaperComponentFlattener() {}

    @SuppressWarnings("Convert2Lambda") // Needs to be explicit
    public static String flatten(Component component) {
        if (FLATTENER == null) {
            return null;
        }

        com.discordsrv.unrelocate.net.kyori.adventure.text.Component unrelocated = ComponentUtil.toAPI(component).asAdventure();
        if (unrelocated == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        FLATTENER.flatten(unrelocated, new FlattenerListener() {
            @Override
            public void component(@NotNull String text) {
                builder.append(text);
            }
        });
        return builder.toString();
    }

    public static class Translator implements net.kyori.adventure.translation.Translator {

        @Override
        public @NotNull Key name() {
            return Key.key("discordsrv", "paper-flattener");
        }

        @Override
        public @Nullable Component translate(
                @NotNull TranslatableComponent component,
                @NotNull Locale locale
        ) {
            String translation = flatten(component);
            if (translation == null) {
                translation = component.fallback();
            }
            if (translation == null) {
                translation = component.key();
            }

            return Component.text()
                    .content(translation)
                    .style(component.style())
                    .append(component.children())
                    .build();
        }

        @Override
        public @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
            return null;
        }
    }
}
