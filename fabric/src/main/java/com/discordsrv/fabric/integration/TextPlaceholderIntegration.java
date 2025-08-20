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

package com.discordsrv.fabric.integration;

import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.placeholder.PlaceholderLookupEvent;
import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import com.discordsrv.api.placeholder.format.PlainPlaceholderFormat;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.api.profile.Profile;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.core.module.type.PluginIntegration;
import com.discordsrv.fabric.FabricDiscordSRV;
import com.mojang.authlib.GameProfile;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderHandler;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TextPlaceholderIntegration extends PluginIntegration<FabricDiscordSRV> implements PlaceholderHandler {

    private static final Identifier IDENTIFIER = FabricDiscordSRV.id("discordsrv", "textplaceholder");
    private static final String OPTIONAL_PREFIX = "textplaceholder_";

    public TextPlaceholderIntegration(FabricDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public @NotNull String getIntegrationId() {
        return "placeholder-api";
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("eu.pb4.placeholders.api.Placeholders");
        } catch (ClassNotFoundException ignored) {
            return false;
        }

        return super.isEnabled();
    }

    @Override
    public void enable() {
        Placeholders.register(IDENTIFIER, this);
    }

    @Override
    public void disable() {
        Placeholders.remove(IDENTIFIER);
    }

    @Subscribe
    public void onPlaceholderLookup(PlaceholderLookupEvent event) {
        String placeholder = event.getPlaceholder();
        if (placeholder.startsWith(OPTIONAL_PREFIX)) {
            placeholder = placeholder.substring(OPTIONAL_PREFIX.length());
        }
        placeholder = "%" + placeholder + "%";

        DiscordSRVPlayer srvPlayer = event.getContext(DiscordSRVPlayer.class);
        ServerPlayerEntity player = srvPlayer != null ? discordSRV.getServer().getPlayerManager().getPlayer(srvPlayer.uniqueId()) : null;
        if (player != null) {
            Text parsed = Placeholders.parseText(Text.of(placeholder), PlaceholderContext.of(player));
            setResult(event, placeholder, parsed.getString());
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

        Text parsedUser = parseUserPlaceholder(placeholder, uuid);
        if (parsedUser != null) {
            setResult(event, placeholder, parsedUser.getString());
            return;
        }

        Text parsed = Placeholders.parseText(Text.of(placeholder), PlaceholderContext.of(discordSRV.getServer()));
        setResult(event, placeholder, parsed.getString());
    }

    private void setResult(PlaceholderLookupEvent event, String placeholder, String result) {
        if (result.equals(placeholder)) {
            // Didn't resolve
            return;
        }

        event.process(PlaceholderLookupResult.success(result));
    }

    @Override
    public PlaceholderResult onPlaceholderRequest(PlaceholderContext placeholderContext, @Nullable String s) {
        Set<Object> context;
        if (placeholderContext.hasPlayer()) {
            context = new HashSet<>(2);

            ServerPlayerEntity player = placeholderContext.player();
            assert player != null;

            Profile profile = discordSRV.profileManager().getCachedProfile(player.getUuid());
            if (profile != null) {
                context.add(profile);
            }

            context.add(discordSRV.playerProvider().player(player));
        } else if (placeholderContext.hasGameProfile()) {
            context = new HashSet<>(2);

            GameProfile gameProfile = placeholderContext.gameProfile();
            assert gameProfile != null;

            Profile profile = discordSRV.profileManager().getCachedProfile(gameProfile.getId());
            if (profile != null) {
                context.add(profile);
            }

            // Check if the player is online
            ServerPlayerEntity player = discordSRV.getServer().getPlayerManager().getPlayer(gameProfile.getId());
            if (player != null) {
                context.add(discordSRV.playerProvider().player(player));
            } else {
                discordSRV.playerProvider().lookupOfflinePlayer(gameProfile.getId()).thenApply(context::add);
            }
        } else {
            context = Collections.emptySet();
        }

        String placeholder = "%" + s + "%";
        String result = PlainPlaceholderFormat.supplyWith(
                PlainPlaceholderFormat.Formatting.LEGACY,
                () -> discordSRV.placeholderService().replacePlaceholders(placeholder, context)
        );
        return placeholder.equals(result) ? PlaceholderResult.invalid() : PlaceholderResult.value(result);
    }

    private Text parseUserPlaceholder(String placeholder, UUID uuid) {
        GameProfile gameProfile = null;

        //? if minecraft: <=1.20.1 {
        /*gameProfile = discordSRV.getServer().getSessionService().fillProfileProperties(new GameProfile(uuid, null), true);

        *///?} else {
        com.mojang.authlib.yggdrasil.ProfileResult profileResult = discordSRV.getServer().getSessionService().fetchProfile(uuid, true);
        if (profileResult != null) {
            gameProfile = profileResult.profile();
        }
        //?}

        if (gameProfile != null) {
            Text parsed = Placeholders.parseText(Text.of(placeholder), PlaceholderContext.of(gameProfile, discordSRV.getServer()));
            return parsed;
        }

        return null;
    }
}
