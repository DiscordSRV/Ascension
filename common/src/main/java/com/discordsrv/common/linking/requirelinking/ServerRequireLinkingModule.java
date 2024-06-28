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

package com.discordsrv.common.linking.requirelinking;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.linking.ServerRequiredLinkingConfig;
import com.discordsrv.common.linking.requirelinking.requirement.parser.ParsedRequirements;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class ServerRequireLinkingModule<T extends DiscordSRV> extends RequiredLinkingModule<T> {

    private final List<ParsedRequirements> additionalRequirements = new CopyOnWriteArrayList<>();

    public ServerRequireLinkingModule(T discordSRV) {
        super(discordSRV);
    }

    @Override
    public abstract ServerRequiredLinkingConfig config();

    @Override
    public void reload() {
        synchronized (additionalRequirements) {
            additionalRequirements.clear();
            additionalRequirements.addAll(compile(config().requirements.additionalRequirements));
        }
    }

    @Override
    public List<ParsedRequirements> getAllActiveRequirements() {
        return additionalRequirements;
    }

    public CompletableFuture<Component> getBlockReason(UUID playerUUID, String playerName, boolean join) {
        List<ParsedRequirements> additionalRequirements;
        synchronized (this.additionalRequirements) {
            additionalRequirements = this.additionalRequirements;
        }

        return getBlockReason(config().requirements, additionalRequirements, playerUUID, playerName, join);
    }
}
