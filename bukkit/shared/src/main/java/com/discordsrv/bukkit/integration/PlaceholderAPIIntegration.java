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

package com.discordsrv.bukkit.integration;

import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.placeholder.PlaceholderLookupEvent;
import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import com.discordsrv.api.placeholder.format.PlainPlaceholderFormat;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.api.profile.Profile;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.core.module.type.PluginIntegration;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlaceholderAPIIntegration extends PluginIntegration<BukkitDiscordSRV> {

    private static final String OPTIONAL_PREFIX = "placeholderapi_";
    private Expansion expansion;

    public PlaceholderAPIIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public @NotNull String getIntegrationId() {
        return "PlaceholderAPI";
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

        DiscordSRVPlayer srvPlayer = event.getContext(DiscordSRVPlayer.class);
        Player player = srvPlayer != null ? discordSRV.server().getPlayer(srvPlayer.uniqueId()) : null;
        if (player != null) {
            setResult(event, placeholder, PlaceholderAPI.setPlaceholders(player, placeholder));
            return;
        }

        Profile profile = event.getContext(Profile.class);
        UUID uuid = profile != null ? profile.playerUUID() : null;
        if (uuid == null) {
            IOfflinePlayer offlinePlayer = event.getContext(IOfflinePlayer.class);
            if (offlinePlayer != null) {
                uuid = offlinePlayer.uniqueId();
            }
        }

        OfflinePlayer offlinePlayer = uuid != null ? discordSRV.server().getOfflinePlayer(uuid) : null;
        setResult(event, placeholder, PlaceholderAPI.setPlaceholders(offlinePlayer, placeholder));
    }

    private void setResult(PlaceholderLookupEvent event, String placeholder, String result) {
        if (result.equals(placeholder)) {
            // Didn't resolve
            return;
        }

        event.process(PlaceholderLookupResult.success(BukkitComponentSerializer.legacy().deserialize(result)));
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
            return discordSRV.versionInfo().version();
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
                Profile profile = discordSRV.profileManager().getProfile(player.getUniqueId());
                if (profile != null) {
                    context.add(profile);
                }
                if (player instanceof Player) {
                    context.add(discordSRV.playerProvider().player((Player) player));
                } else {
                    context.add(discordSRV.playerProvider().offlinePlayer(player));
                }
            } else {
                context = Collections.emptySet();
            }

            String placeholder = "%" + params + "%";
            String result = PlainPlaceholderFormat.supplyWith(
                    PlainPlaceholderFormat.Formatting.LEGACY,
                    () -> discordSRV.placeholderService().replacePlaceholders(placeholder, context)
            );
            return placeholder.equals(result) ? null : result;
        }
    }
}
