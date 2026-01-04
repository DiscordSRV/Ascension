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

package com.discordsrv.bukkit.integration.chat;

import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.message.preprocess.game.GameChatMessagePreProcessEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.PluginIntegration;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.jetbrains.annotations.NotNull;

public class GriefPreventionChatIntegration extends PluginIntegration<BukkitDiscordSRV> {

    public GriefPreventionChatIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "GRIEFPREVENTION"));
    }

    @Override
    public @NotNull String getIntegrationId() {
        return "GriefPrevention";
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
        } catch (ClassNotFoundException ignored) {
            return false;
        }

        return super.isEnabled();
    }

    @Subscribe(priority = EventPriorities.EARLY)
    public void onGameChatMessageReceive(GameChatMessagePreProcessEvent event) {
        GriefPrevention griefPrevention = (GriefPrevention) discordSRV.server().getPluginManager().getPlugin(
                getIntegrationId());
        if (griefPrevention == null) {
            return;
        }

        DiscordSRVPlayer player = event.getPlayer();
        if (griefPrevention.dataStore.isSoftMuted(player.uniqueId())) {
            logger().debug(player.username() + " is softmuted");
            event.setCancelled(true);
        }
    }
}
