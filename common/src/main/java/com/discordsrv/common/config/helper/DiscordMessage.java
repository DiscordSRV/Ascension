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

package com.discordsrv.common.config.helper;

import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import net.kyori.adventure.text.Component;

public class DiscordMessage extends ConfigMessage {

    private final SendableDiscordMessage.Builder builder;

    public DiscordMessage(SendableDiscordMessage.Builder builder) {
        this.builder = builder;
    }

    public SendableDiscordMessage get() {
        return builder.build();
    }

    public SendableDiscordMessage.Builder builder() {
        return builder.clone();
    }

    public SendableDiscordMessage.Formatter format() {
        return builder.toFormatter();
    }

    public String content() {
        return builder.getContent();
    }

    @Override
    protected void sendTo(CommandExecution execution, Object... context) {
        execution.send((Component) null, format().addContext(context).applyPlaceholderService().build());
    }
}
