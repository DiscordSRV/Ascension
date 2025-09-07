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

package com.discordsrv.common.abstraction.player;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.vanish.PlayerVanishStatusCheckEvent;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

@PlaceholderPrefix("player_")
public interface IPlayer extends DiscordSRVPlayer, IOfflinePlayer, ICommandSender {

    @Override
    default void sendMessageFromDiscord(@NotNull MinecraftComponent component) {
        sendMessage(ComponentUtil.fromAPI(component));
    }

    @Override
    @NotNull
    String username();

    @Override
    default @NotNull UUID uniqueId() {
        return identity().uuid();
    }

    Task<Void> kick(Component component);

    void addChatSuggestions(Collection<String> suggestions);
    void removeChatSuggestions(Collection<String> suggestions);

    @NotNull
    @Placeholder("display_name")
    Component displayName();

    @NotNull
    @Placeholder("team_display_name")
    default Component teamDisplayName() {
        return Component.text(username());
    }

    @Nullable
    @Placeholder("world_name")
    default String world() {
        return null;
    }

    @Override
    default boolean isVanished() {
        PlayerVanishStatusCheckEvent event = new PlayerVanishStatusCheckEvent(this, false);
        discordSRV().eventBus().publish(event);
        return event.getStatus();
    }

    @Override
    default boolean isChatVisible() {
        return true;
    }

    @Override
    @Nullable Locale locale();
}
