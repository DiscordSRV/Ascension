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

package com.discordsrv.common.integration;

import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import com.discordsrv.common.core.module.type.PluginIntegration;
import com.discordsrv.common.events.player.PlayerCollectSkinEvent;
import net.skinsrestorer.api.PropertyUtils;
import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.property.SkinVariant;
import net.skinsrestorer.api.storage.PlayerStorage;
import org.jetbrains.annotations.NotNull;

public class SkinsRestorerIntegration extends PluginIntegration<DiscordSRV> {

    private final PlayerStorage playerStorage;

    public SkinsRestorerIntegration(DiscordSRV discordSRV) {
        super(discordSRV);
        this.playerStorage = SkinsRestorerProvider.get().getPlayerStorage();
    }

    @Override
    public @NotNull String getIntegrationId() {
        return "SkinsRestorer";
    }

    @Subscribe(priority = EventPriorities.LATE)
    public void getSkinForPlayer(PlayerCollectSkinEvent event) {
        playerStorage.getSkinOfPlayer(event.player().uniqueId()).map(skinProperty -> {
            SkinVariant variant = PropertyUtils.getSkinVariant(skinProperty);
            return new SkinInfo(PropertyUtils.getSkinTextureHash(skinProperty), variant.name(), null);
        }).ifPresent(event::update);
    }
}
