/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.bukkit.config.manager;

import com.discordsrv.bukkit.config.main.BukkitConfig;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.configurate.manager.abstraction.ServerConfigManager;

import java.nio.file.Path;

public class BukkitConfigManager extends ServerConfigManager<BukkitConfig> {

    public BukkitConfigManager(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    public BukkitConfigManager(Path dataDirectory) {
        super(dataDirectory);
    }

    @Override
    public BukkitConfig createConfiguration() {
        return new BukkitConfig();
    }
}
