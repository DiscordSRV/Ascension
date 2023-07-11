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

package com.discordsrv.bukkit.requiredlinking;

import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.config.main.BukkitRequiredLinkingConfig;
import com.discordsrv.common.config.main.linking.RequirementsConfig;
import com.discordsrv.common.linking.requirelinking.ServerRequireLinkingModule;
import org.bukkit.event.Listener;

public class BukkitRequiredLinkingModule extends ServerRequireLinkingModule<BukkitDiscordSRV> implements Listener {

    public BukkitRequiredLinkingModule(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public BukkitRequiredLinkingConfig config() {
        return discordSRV.config().requiredLinking;
    }
}
