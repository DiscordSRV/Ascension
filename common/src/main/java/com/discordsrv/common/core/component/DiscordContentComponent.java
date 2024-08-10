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

package com.discordsrv.common.core.component;

import com.discordsrv.api.discord.util.DiscordFormattingUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Possibly removable after <a href="https://github.com/KyoriPowered/adventure/pull/842">adventure #842</a>
 *
 * @see ComponentFactory#discordSerialize(Component)
 */
public class DiscordContentComponent {

    private static final String PREFIX = "DiscordSRV:discord_content:";

    @NotNull
    public static TextComponent of(@NotNull String discordContent, ComponentLike gameComponent) {
        return Component.text()
                .append(Component.text().insertion(PREFIX + discordContent))
                .append(gameComponent)
                .build();
    }

    public static Component remapToDiscord(@NotNull Component component) {
        List<Component> children = component.children();
        if (component instanceof TextComponent) {
            if (children.size() == 2) {
                Component first = children.get(0);
                String insertion = first.insertion();
                if (insertion != null && insertion.startsWith(PREFIX)) {
                    return Component.text(insertion.substring(PREFIX.length()));
                }
            }

            String content = ((TextComponent) component).content();
            component = ((TextComponent) component).content(DiscordFormattingUtil.escapeMentions(content));
        }

        List<Component> newChildren = new ArrayList<>();
        for (Component child : children) {
            newChildren.add(remapToDiscord(child));
        }

        return component.children(newChildren);
    }
}
