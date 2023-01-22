/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.command.game.sender;

import com.discordsrv.common.command.game.executor.CommandExecutor;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public interface ICommandSender extends ForwardingAudience.Single, CommandExecutor {

    /**
     * Sends a message to this {@link ICommandSender} with {@link Identity#nil()}.
     * @param message the message to send
     */
    default void sendMessage(@NotNull Component message) {
        sendMessage(Identity.nil(), message, MessageType.CHAT);
    }

    /**
     * Sends a message to this {@link ICommandSender} with {@link Identity#nil()}.
     * @param message the message to send
     * @param messageType the {@link MessageType}
     */
    default void sendMessage(@NotNull Component message, @NotNull MessageType messageType) {
        sendMessage(Identity.nil(), message, messageType);
    }

    boolean hasPermission(String permission);

}
