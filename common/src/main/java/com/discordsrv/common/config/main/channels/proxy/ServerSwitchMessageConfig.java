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

package com.discordsrv.common.config.main.channels.proxy;

import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.config.annotation.Untranslated;
import com.discordsrv.common.config.main.generic.IMessageConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class ServerSwitchMessageConfig implements IMessageConfig {

    public boolean enabled = false;

    @Untranslated(Untranslated.Type.VALUE)
    public SendableDiscordMessage.Builder format = SendableDiscordMessage.builder()
            .addEmbed(
                    DiscordMessageEmbed.builder()
                            .setAuthor(
                                    "%player_display_name% switched from %from_server% to %to_server%",
                                    null,
                                    "%player_avatar_url%"
                            )
                            .setColor(0x5555FF)
                            .build()
            );

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public SendableDiscordMessage.Builder format() {
        return format;
    }
}
