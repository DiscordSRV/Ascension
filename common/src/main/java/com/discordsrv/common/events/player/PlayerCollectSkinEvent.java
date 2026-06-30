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

package com.discordsrv.common.events.player;

import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.events.Event;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import org.jetbrains.annotations.Nullable;


/**
 * This event is used by DiscordSRV to lookup {@link SkinInfo} for players from platform methods (and optionally integrations).
 * This is also used to determine which skin provider should take priority when there are multiple providers ({@link EventPriorities}).
 */
public class PlayerCollectSkinEvent implements Event {

    private final IOfflinePlayer player;

    private String textureId;
    private String model;
    private SkinInfo.Parts parts;

    public PlayerCollectSkinEvent(IOfflinePlayer player) {
        this.player = player;
    }

    public IOfflinePlayer player() {
        return player;
    }

    public boolean hasBeenModified() {
        return textureId != null && model != null && parts != null;
    }

    /**
     * @return the skin info for the player
     * @throws IllegalStateException if {@link #hasBeenModified()} doesn't return true
     */
    @Nullable
    public SkinInfo getSkinInfo() {
        if (!hasBeenModified()) {
            throw new IllegalStateException("This event has not been successfully modified yet, no skin is available");
        }
        return new SkinInfo(textureId, model, parts);
    }

    public void update(String textureId, String model, SkinInfo.Parts parts) {
        if (textureId != null) this.textureId = textureId;
        if (model != null) this.model = model;
        if (parts != null) this.parts = parts;
    }

    public void update(SkinInfo input) {
        if (input != null) {
            update(input.textureId(), input.model(), input.getParts());
        }
    }
}
