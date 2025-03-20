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

package com.discordsrv.common.core.placeholder.context;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.SelfUser;

@PlaceholderPrefix("bot_")
public class DiscordBotContext {

    private final DiscordSRV discordSRV;

    public DiscordBotContext(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    private SelfUser getSelfUser() {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            return null;
        }

        return jda.getSelfUser();
    }

    @Placeholder("user")
    public DiscordUser getUser() {
        return discordSRV.discordAPI().getUser(getSelfUser());
    }
}
