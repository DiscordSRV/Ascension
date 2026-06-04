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

package com.discordsrv.common.feature.linking;

import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.linking.LinkedAccountConfig;
import com.discordsrv.common.events.player.PlayerConnectedEvent;
import com.discordsrv.common.abstraction.module.AbstractTimedTrackingModule;

import java.time.Duration;
import java.util.UUID;

public class LinkPesteringModule extends AbstractTimedTrackingModule {

    public LinkPesteringModule(DiscordSRV discordSRV) {
        super(discordSRV, "LINK_PESTER");
    }

    @Override
    public boolean isEnabled() {
        return discordSRV.config().linkedAccounts.pesteringConfig.enabled;
    }

    @Subscribe
    public void onPlayerConnected(PlayerConnectedEvent event) {
        if (discordSRV.config().linkedAccounts.pesteringConfig.mode == LinkedAccountConfig.LinkPesteringConfig.PesteringMode.TIMER) {
            return;
        }
        pesterIfUnlinked(event.player());
    }

    @Override
    protected boolean shouldRunTimedTask() {
        return discordSRV.config().linkedAccounts.pesteringConfig.mode != LinkedAccountConfig.LinkPesteringConfig.PesteringMode.JOIN;
    }

    @Override
    protected Duration getMinimalInterval() {
        return Duration.ofSeconds(10);
    }

    @Override
    protected Duration timedTaskInterval() {
        int intervalSeconds = Math.max(discordSRV.config().linkedAccounts.pesteringConfig.timer, 10);
        return Duration.ofSeconds(intervalSeconds);
    }

    @Override
    protected void runTimedTask() {
        pesterOnlinePlayers();
    }

    private void pesterOnlinePlayers() {
        for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
            pesterIfUnlinked(player);
        }
    }

    private void pesterIfUnlinked(IPlayer player) {
        LinkProvider linkProvider = discordSRV.linkProvider();
        if (linkProvider == null) {
            return;
        }

        UUID playerUUID = player.uniqueId();
        linkProvider.get(playerUUID)
                .whenSuccessful(link -> {
                    if (link.isPresent()) {
                        return;
                    }

                    IPlayer onlinePlayer = discordSRV.playerProvider().player(playerUUID);
                    if (onlinePlayer == null) {
                        return;
                    }

                    onlinePlayer.sendMessage(discordSRV.messagesConfig().playerLinkPestering.asComponent());
                })
                .whenFailed(t -> logger().debug("Failed to pester unlinked player " + playerUUID, t));
    }
}
