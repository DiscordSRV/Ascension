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

package com.discordsrv.common.config.connection;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class UpdateConfig {

    @Setting(value = "notification-enabled")
    @Comment("On/off for notifications when a new version of DiscordSRV is available")
    public boolean notificationEnabled = true;

    @Setting(value = "notification-ingame")
    @Comment("If players with the discordsrv.updatenotification permission should receive\n"
            + "a update notification upon joining if there is a update available")
    public boolean notificationInGame = true;

    @Setting(value = "enable-first-party-api-for-notifications")
    @Comment("Whether the DiscordSRV download API should be used for update checks\n"
            + "Requires a connection to: download.discordsrv.com")
    public boolean firstPartyNotification = true;

    @Setting(value = "github")
    public GitHub github = new GitHub();

    @Setting(value = "security")
    public Security security = new Security();

    @ConfigSerializable
    public static class GitHub {

        @Setting(value = "enabled")
        @Comment("Whether the GitHub API should be used for update checks\n"
                + "This will be the secondary API if both first party and GitHub sources are enabled\n"
                + "Requires a connection to: api.github.com")
        public boolean enabled = true;

        @Setting(value = "api-token")
        @Comment("The GitHub API token used for authenticating to the GitHub api,\n"
                + "if this isn't specified the API will be used 'anonymously'\n"
                + "The token only requires read permission to DiscordSRV/DiscordSRV releases, workflows and commits")
        public String apiToken = "";

    }

    @ConfigSerializable
    public static class Security {

        @Setting(value = "enabled")
        @Comment("Uses the DiscordSRV download API to check if the version of DiscordSRV\n"
                + "being used is vulnerable to known vulnerabilities, disabling the plugin if it is.\n"
                + "Requires a connection to: download.discordsrv.com\n"
                + "\n"
                + "WARNING! DO NOT TURN THIS OFF UNLESS YOU KNOW WHAT YOU'RE DOING AND STAY UP-TO-DATE")
        public boolean enabled = true;

        @Setting(value = "force")
        @Comment("If the security check needs to be completed for DiscordSRV to enable,\n"
                + "if the security check cannot be performed, DiscordSRV will be disabled if this option is set to true")
        public boolean force = false;

    }
}
