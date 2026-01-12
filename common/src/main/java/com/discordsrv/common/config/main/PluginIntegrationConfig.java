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

package com.discordsrv.common.config.main;

import com.discordsrv.common.config.configurate.annotation.Constants;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class PluginIntegrationConfig {

    @Comment("Plugin/mod integrations that should be disabled. Specify the names or ids of plugins/mods to disable integrations for")
    public List<String> disabledIntegrations = new ArrayList<>();

    @Comment("Specify how vanish should be tracked for players.\n"
            + "\"%1\" to automatically determine if timed tracking should be used\n"
            + "\"%2\" to always use timed checking for vanish status\n"
            + "\"%3\" to only use events for vanish status tracking (only works with supported vanish plugins)")
    @Constants.Comment({"auto", "timer", "event_only"})
    public VanishTracking vanishTracking = VanishTracking.AUTO;
    public int vanishTrackingTimerSeconds = 15;

    public enum VanishTracking {
        AUTO,
        TIMER,
        EVENT_ONLY
    }
}
