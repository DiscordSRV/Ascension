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

package com.discordsrv.common.config.main;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.core.logging.Logger;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@ConfigSerializable
public class PresenceUpdaterConfig {

    @Comment("The number of seconds between presence updates\n"
            + "Minimum value: %1")
    @Constants.Comment("30")
    public int updaterRateInSeconds = 90;

    public List<Presence> presences = new ArrayList<>(Collections.singleton(new Presence()));

    @ConfigSerializable
    public static class Server extends PresenceUpdaterConfig {

        @Comment("The presence to use while the server is starting")
        public boolean useStartingPresence = true;
        public Presence startingPresence = new Presence(OnlineStatus.DO_NOT_DISTURB, "Starting...");

        @Comment("The presence to use while the server is stopping")
        public boolean useStoppingPresence = true;
        public Presence stoppingPresence = new Presence(OnlineStatus.IDLE, "Stopping...");

    }

    @ConfigSerializable
    public static class Presence {

        public Presence() {}

        public Presence(OnlineStatus status, String activity) {
            this.status = status;
            this.activity = activity;
        }

        @Comment("Valid options: %1")
        @Constants.Comment({"online, idle, do_not_disturb, invisible"})
        public OnlineStatus status = OnlineStatus.ONLINE;

        @Comment("This may be prefixed by one of the following and a space to specify the activity type: %1\n"
                + "You can use streaming by setting the value to: %2, a YouTube or Twitch link and the text all separated by a space")
        @Constants.Comment({
                "\"playing\", \"listening\", \"watching\", \"competing in\"",
                "\"streaming\""
        })
        public String activity = "playing Minecraft";

        public Activity activity(Logger logger, DiscordSRV discordSRV) {
            Activity.ActivityType activityType = Activity.ActivityType.CUSTOM_STATUS;
            String activity = this.activity;
            String url = null;

            for (Activity.ActivityType type : Activity.ActivityType.values()) {
                String name = type.name().toLowerCase(Locale.ROOT);
                if (type == Activity.ActivityType.COMPETING) {
                    name = "competing in";
                }
                name += " ";

                if (!activity.toLowerCase(Locale.ROOT).startsWith(name)) {
                    continue;
                }

                String namePart = activity.substring(name.length());
                if (namePart.trim().isEmpty()) {
                    continue;
                }
                if (type == Activity.ActivityType.STREAMING) {
                    String[] parts = namePart.split(" ", 2);
                    if (parts.length == 2) {
                        String link = parts[0];
                        if (!Activity.isValidStreamingUrl(link)) {
                            if (logger != null) {
                                logger.warning("Invalid streaming presence URL: " + link);
                            }
                        } else {
                            url = parts[0];
                            namePart = parts[1];
                        }
                    }
                }

                activityType = type;
                activity = namePart;
                break;
            }

            return Activity.of(
                    activityType,
                    discordSRV.placeholderService().replacePlaceholders(activity),
                    url
            );
        }
    }
}
