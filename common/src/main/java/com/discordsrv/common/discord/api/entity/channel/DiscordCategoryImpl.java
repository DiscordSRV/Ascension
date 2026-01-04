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

package com.discordsrv.common.discord.api.entity.channel;

import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.discord.entity.channel.DiscordCategory;
import com.discordsrv.api.discord.entity.channel.DiscordChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;

public class DiscordCategoryImpl implements DiscordCategory {

    private final Category category;

    public DiscordCategoryImpl(DiscordSRV discordSRV, Category category) {
        this.category = category;
    }

    @Override
    public String getName() {
        return category.getName();
    }

    @Override
    public DiscordChannelType getType() {
        return DiscordChannelType.CATEGORY;
    }

    @Override
    public long getId() {
        return category.getIdLong();
    }

    @Override
    public Category asJDA() {
        return category;
    }
}
