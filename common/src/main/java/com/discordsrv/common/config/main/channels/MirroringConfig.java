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

package com.discordsrv.common.config.main.channels;

import com.discordsrv.common.config.main.DiscordIgnores;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class MirroringConfig {

    public boolean enabled = true;

    @Comment("Users, bots and webhooks to ignore when mirroring")
    public DiscordIgnores ignores = new DiscordIgnores();

    @Comment("The format of the username of mirrored messages\n"
            + "It's recommended to include some special character if in-game messages use webhooks,\n"
            + "in order to prevent Discord users and in-game players being grouped together")
    public String usernameFormat = "%user_effective_name% \uD83D\uDD03";
}
