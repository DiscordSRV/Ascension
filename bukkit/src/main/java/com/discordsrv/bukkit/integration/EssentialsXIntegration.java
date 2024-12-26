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

package com.discordsrv.bukkit.integration;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.channel.GameChannelLookupEvent;
import com.discordsrv.api.events.message.receive.game.GameChatMessageReceiveEvent;
import com.discordsrv.api.module.type.NicknameModule;
import com.discordsrv.api.module.type.PunishmentModule;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.api.punishment.Punishment;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.player.BukkitPlayer;
import com.discordsrv.common.core.module.type.PluginIntegration;
import com.discordsrv.common.util.ComponentUtil;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.UserData;
import net.essentialsx.api.v2.events.chat.GlobalChatEvent;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EssentialsXIntegration
        extends PluginIntegration<BukkitDiscordSRV>
        implements Listener, PunishmentModule.Mutes, NicknameModule {

    private final GlobalChannel channel = new GlobalChannel();

    public EssentialsXIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public @NotNull String getIntegrationId() {
        return "Essentials";
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("net.essentialsx.api.v2.events.chat.GlobalChatEvent");
        } catch (ClassNotFoundException ignored) {
            return false;
        }
        return super.isEnabled();
    }

    @Override
    public void enable() {
        discordSRV.server().getPluginManager().registerEvents(this, discordSRV.plugin());
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this);
    }

    private Essentials get() {
        return (Essentials) discordSRV.server().getPluginManager().getPlugin("Essentials");
    }

    private CompletableFuture<User> getUser(UUID playerUUID) {
        return discordSRV.scheduler().supply(() -> get().getUsers().loadUncachedUser(playerUUID));
    }

    @Override
    public CompletableFuture<String> getNickname(UUID playerUUID) {
        return getUser(playerUUID).thenApply(UserData::getNickname);
    }

    @Override
    public CompletableFuture<Void> setNickname(UUID playerUUID, String nickname) {
        return getUser(playerUUID).thenApply(user -> {
            user.setNickname(nickname);
            return null;
        });
    }

    @Override
    public CompletableFuture<com.discordsrv.api.punishment.Punishment> getMute(@NotNull UUID playerUUID) {
        return getUser(playerUUID).thenApply(user -> new Punishment(
                Instant.ofEpochMilli(user.getMuteTimeout()),
                ComponentUtil.toAPI(BukkitComponentSerializer.legacy().deserialize(user.getMuteReason())),
                null
        ));
    }

    @Override
    public CompletableFuture<Void> addMute(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable MinecraftComponent reason, @NotNull MinecraftComponent punisher) {
        String reasonLegacy = reason != null ? BukkitComponentSerializer.legacy().serialize(ComponentUtil.fromAPI(reason)) : null;
        return getUser(playerUUID).thenApply(user -> {
            user.setMuted(true);
            user.setMuteTimeout(until != null ? until.toEpochMilli() : 0);
            user.setMuteReason(reasonLegacy);
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> removeMute(@NotNull UUID playerUUID) {
        return getUser(playerUUID).thenApply(user -> {
            user.setMuted(false);
            user.setMuteTimeout(0);
            user.setMuteReason(null);
            return null;
        });
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onGlobalChat(GlobalChatEvent event) {
        Player player = event.getPlayer();
        MinecraftComponent component = ComponentUtil.toAPI(
                BukkitComponentSerializer.legacy().deserialize(event.getMessage())
        );

        BukkitPlayer srvPlayer = discordSRV.playerProvider().player(player);
        boolean cancelled = event.isCancelled();
        discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(
                new GameChatMessageReceiveEvent(event, srvPlayer, component, channel, cancelled)
        ));
    }

    @Subscribe(priority = EventPriorities.LAST)
    public void onGameChannelLookup(GameChannelLookupEvent event) {
        if (checkProcessor(event) || !discordSRV.server().getPluginManager().isPluginEnabled("EssentialsChat")) {
            return;
        }

        if (event.isDefault()) {
            event.process(channel);
        }
    }

    private class GlobalChannel implements GameChannel {

        @Override
        public @NotNull String getOwnerName() {
            return getIntegrationId();
        }

        @Override
        public @NotNull String getChannelName() {
            return GameChannel.DEFAULT_NAME;
        }

        @Override
        public boolean isChat() {
            return true;
        }

        @Override
        public @NotNull Collection<? extends DiscordSRVPlayer> getRecipients() {
            return discordSRV.playerProvider().allPlayers();
        }
    }
}
