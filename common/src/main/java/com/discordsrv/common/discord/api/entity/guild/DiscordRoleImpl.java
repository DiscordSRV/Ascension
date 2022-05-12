/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.discord.api.entity.guild;

import com.discordsrv.api.color.Color;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DiscordRoleImpl implements DiscordRole {

    private final Role role;
    private final DiscordGuild guild;
    private final Color color;

    public DiscordRoleImpl(DiscordSRV discordSRV, Role role) {
        this.role = role;
        this.guild = new DiscordGuildImpl(discordSRV, role.getGuild());
        this.color = new Color(role.getColorRaw());
    }

    @Override
    public long getId() {
        return role.getIdLong();
    }

    @Override
    public @NotNull DiscordGuild getGuild() {
        return guild;
    }

    @Override
    public @NotNull String getName() {
        return role.getName();
    }

    @Override
    public @NotNull Color getColor() {
        return color;
    }

    @Override
    public boolean isHoisted() {
        return role.isHoisted();
    }

    @Override
    public Role getAsJDARole() {
        return role;
    }

    @Override
    public String getAsMention() {
        return role.getAsMention();
    }

    @Override
    public String toString() {
        return "ServerRole:" + getName() + "(" + Long.toUnsignedString(getId()) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscordRoleImpl that = (DiscordRoleImpl) o;
        return Objects.equals(role.getId(), that.role.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(role.getId());
    }
}
