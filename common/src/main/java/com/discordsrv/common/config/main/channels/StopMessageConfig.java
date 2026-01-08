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

package com.discordsrv.common.config.main.channels;

import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.config.configurate.annotation.Untranslated;
import com.discordsrv.common.config.configurate.manager.abstraction.ConfigurateConfigManager;
import com.discordsrv.common.config.helper.SendableDiscordMessageTemplate;
import com.discordsrv.common.config.main.generic.IMessageConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class StopMessageConfig implements IMessageConfig {

    public StopMessageConfig() {
        ConfigurateConfigManager.nullAllFields(this);
    }

    public Boolean enabled = true;

    @Untranslated(Untranslated.Type.VALUE)
    public SendableDiscordMessageTemplate format = new SendableDiscordMessageTemplate(
            SendableDiscordMessage.builder().setContent(":stop_button: **The server has stopped**")
    );

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public SendableDiscordMessageTemplate format() {
        return format;
    }
}
