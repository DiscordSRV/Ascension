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

package com.discordsrv.bukkit.integration;

import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.placeholder.PlaceholderLookupEvent;
import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.api.profile.IProfile;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.module.type.PluginIntegration;
import com.discordsrv.common.player.IOfflinePlayer;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PlaceholderAPIIntegration extends PluginIntegration<BukkitDiscordSRV> {

    private static final String OPTIONAL_PREFIX = "placeholderapi_";
    private Expansion expansion;

    public PlaceholderAPIIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
        } catch (ClassNotFoundException ignored) {
            return false;
        }

        return super.isEnabled();
    }

    @Override
    public void enable() {
        expansion = new Expansion();
        discordSRV.scheduler().runOnMainThread(() -> expansion.register());
    }

    @Override
    public void disable() {
        if (expansion != null) {
            discordSRV.scheduler().runOnMainThread(() -> expansion.unregister());
        }
    }

    @Subscribe
    public void onPlaceholderLookup(PlaceholderLookupEvent event) {
        String placeholder = event.getPlaceholder();
        if (placeholder.startsWith(OPTIONAL_PREFIX)) {
            placeholder = placeholder.substring(OPTIONAL_PREFIX.length());
        }
        placeholder = "%" + placeholder + "%";

        Player player = event.getContext(DiscordSRVPlayer.class)
                .map(p -> discordSRV.server().getPlayer(p.uniqueId()))
                .orElse(null);
        if (player != null) {
            setResult(event, placeholder, PlaceholderAPI.setPlaceholders(player, placeholder));
            return;
        }

        UUID uuid = event.getContext(IProfile.class)
                .flatMap(IProfile::playerUUID)
                .orElseGet(() -> event.getContext(IOfflinePlayer.class).map(IOfflinePlayer::uniqueId).orElse(null));

        OfflinePlayer offlinePlayer = uuid != null ? discordSRV.server().getOfflinePlayer(uuid) : null;
        setResult(event, placeholder, PlaceholderAPI.setPlaceholders(offlinePlayer, placeholder));
    }

    private void setResult(PlaceholderLookupEvent event, String placeholder, String result) {
        if (result.equals(placeholder)) {
            // Didn't resolve
            return;
        }

        event.process(PlaceholderLookupResult.success(result));
    }

    public class Expansion extends PlaceholderExpansion {

        @Override
        public @NotNull String getIdentifier() {
            return "discordsrv";
        }

        @Override
        public @NotNull String getAuthor() {
            return "DiscordSRV";
        }

        @Override
        public @NotNull String getVersion() {
            return discordSRV.version();
        }

        @Override
        public @NotNull String getName() {
            return "DiscordSRV";
        }

        @Override
        public boolean persist() {
            return true;
        }

        @Override
        public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
            return onRequest(player, params);
        }

        @Override
        public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
            Set<Object> context;
            if (player != null) {
                context = new HashSet<>(2);
                discordSRV.profileManager().getProfile(player.getUniqueId()).ifPresent(context::add);
                if (player instanceof Player) {
                    context.add(discordSRV.playerProvider().player((Player) player));
                } else {
                    context.add(discordSRV.playerProvider().offlinePlayer(player));
                }
            } else {
                context = Collections.emptySet();
            }

            String placeholder = "%" + params + "%";
            String result = discordSRV.placeholderService().replacePlaceholders(placeholder, context);
            return placeholder.equals(result) ? null : result;
        }
    }
}
