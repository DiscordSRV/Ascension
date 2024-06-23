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

package com.discordsrv.common.config.main.linking;

import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.connection.ConnectionConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class LinkedAccountConfig {

    @Comment("Should linked accounts be enabled")
    public boolean enabled = true;

    @Comment("The linked account provider\n"
            + "\n"
            + " - auto: Uses \"minecraftauth\" if the %1 permits it and the server is in online mode or using ip forwarding, otherwise \"%4\"\n"
            + " - minecraftauth: Uses %2 as the linked account provider (offline and (non-linked) bedrock players cannot link using this method)\n"
            + " - storage: Use the configured database for linked accounts")
    @Constants.Comment({ConnectionConfig.FILE_NAME, "minecraftauth.me"})
    public Provider provider = Provider.AUTO;

    public enum Provider {
        AUTO,
        MINECRAFTAUTH,
        STORAGE
    }
}
