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

package com.discordsrv.common.core.component;

import com.discordsrv.api.discord.util.DiscordFormattingUtil;
import net.dv8tion.jda.api.entities.Message;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Possibly removable after <a href="https://github.com/KyoriPowered/adventure/pull/842">adventure #842</a>
 *
 * @see ComponentFactory#discordSerialize(Component)
 */
public class DiscordMentionComponent {

    private static final String PREFIX = "DiscordSRV:discord_mention:";

    @NotNull
    public static TextComponent of(Message.MentionType type, @NotNull String value, ComponentLike gameComponent) {
        return Component.text()
                .append(Component.text().insertion(PREFIX + type.name() + ";" + value))
                .append(gameComponent)
                .build();
    }

    public static List<Pair<Message.MentionType, String>> digValues(@NotNull Component component) {
        List<Pair<Message.MentionType, String>> values = new ArrayList<>();

        Pair<Message.MentionType, String> discordValue = getDiscordValues(component);
        if (discordValue != null) {
            values.add(discordValue);
            return values;
        }

        for (Component child : component.children()) {
            values.addAll(digValues(child));
        }
        return values;
    }

    public static Component remapToDiscord(@NotNull Component component) {
        List<Component> children = component.children();
        if (component instanceof TextComponent) {
            String discordRepresentation = convertToDiscord(component);
            if (discordRepresentation != null) {
                return Component.text(discordRepresentation);
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

    private static Pair<Message.MentionType, String> getDiscordValues(Component candidate) {
        if (!(candidate instanceof TextComponent)) {
            return null;
        }
        List<Component> children = candidate.children();
        if (children.size() != 2) {
            return null;
        }

        String insertion = children.get(0).insertion();
        if (insertion != null && insertion.startsWith(PREFIX)) {
            String[] parts = insertion.substring(PREFIX.length()).split(";", 2);
            if (parts.length == 2) {
                Message.MentionType mentionType;
                try {
                    mentionType = Message.MentionType.valueOf(parts[0]);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
                return Pair.of(mentionType, parts[1]);
            }
        }
        return null;
    }

    private static String convertToDiscord(Component candidate) {
        Pair<Message.MentionType, String> values = getDiscordValues(candidate);
        if (values == null) {
            return null;
        }
        switch (values.getKey()) {
            case USER: return "<@" + values.getValue() + ">";
            case ROLE: return "<@&" + values.getValue() + ">";
            case CHANNEL: return "<#" + values.getValue() + ">";
            case EMOJI: return values.getValue();
        }
        return null;
    }
}
