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

package com.discordsrv.bukkit.integration;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.integration.LuckPermsIntegration;
import net.luckperms.api.context.ContextConsumer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class BukkitLuckPermsIntegration extends LuckPermsIntegration<Player> {
    public BukkitLuckPermsIntegration(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void calculate(@NonNull Player target, @NonNull ContextConsumer consumer) {
        calculate(target.getUniqueId(), consumer);
    }
}
