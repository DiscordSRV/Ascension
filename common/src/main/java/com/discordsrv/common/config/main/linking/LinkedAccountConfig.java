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

package com.discordsrv.common.config.main.linking;

import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.feature.linking.impl.MinecraftAuthenticationLinker;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class LinkedAccountConfig {

    @Comment("Should linked accounts be enabled")
    public boolean enabled = true;

    @Comment("Should users who have not linked their accounts be sent a message encouraging them to link their accounts every time they join the server")
    public LinkPesteringConfig pesteringConfig = new LinkPesteringConfig();

    @Comment("The linked account provider\n"
            + "\n"
            + " - auto: Uses \"minecraftauth\" if the %1 permits it and the server is in online mode or using ip forwarding, otherwise \"%3\"\n"
            + " - minecraftauth: Uses %2 as the linked account provider (offline and (non-linked) bedrock players cannot link using this method)\n"
            + " - storage: Use the configured database for linked accounts")
    @Constants.Comment({ConnectionConfig.FILE_NAME, MinecraftAuthenticationLinker.DOMAIN, "storage"})
    public Provider provider = Provider.AUTO;

    public enum Provider {
        AUTO,
        MINECRAFTAUTH,
        STORAGE
    }

    public static class LinkPesteringConfig {
        @Comment("Should link pestering be enabled")
        public boolean enabled = true;

        @Comment("How should we pester users to link their accounts\n"
                + "\n"
                + " \"%1\" to send a message when they connect to the server\n"
                + " \"%2\" to send a timed message \n"
                + " \"%3\" to do both of the above")
        @Constants.Comment({"join", "timer", "both"})
        public PesteringMode mode = PesteringMode.JOIN;

        @Comment("How often (in minutes) should we send the timed pestering message to users who have not linked their accounts. Minimum of 1 minute")
        public int timer = 1;

        public enum PesteringMode {
            JOIN,
            TIMER,
            BOTH
        }
    }
}
