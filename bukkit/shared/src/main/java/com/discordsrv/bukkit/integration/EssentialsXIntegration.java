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

package com.discordsrv.bukkit.integration;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.channel.GameChannelLookupEvent;
import com.discordsrv.api.events.message.preprocess.game.GameChatMessagePreProcessEvent;
import com.discordsrv.api.events.vanish.PlayerVanishStatusChangeEvent;
import com.discordsrv.api.module.type.NicknameModule;
import com.discordsrv.api.module.type.PunishmentModule;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.api.punishment.Punishment;
import com.discordsrv.api.task.Task;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.player.BukkitPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.PluginIntegration;
import com.discordsrv.common.feature.nicknamesync.NicknameSyncModule;
import com.discordsrv.common.util.ComponentUtil;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.UserData;
import net.ess3.api.IUser;
import net.ess3.api.events.MuteStatusChangeEvent;
import net.ess3.api.events.NickChangeEvent;
import net.ess3.api.events.VanishStatusChangeEvent;
import net.essentialsx.api.v2.events.chat.GlobalChatEvent;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public class EssentialsXIntegration
        extends PluginIntegration<BukkitDiscordSRV>
        implements Listener, NicknameModule, PunishmentModule.Mutes {

    public EssentialsXIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "ESSENTIALSX_INTEGRATION"));
    }

    @Override
    public @NotNull String getIntegrationId() {
        return "Essentials";
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("net.essentialsx.api.v2.events.chat.GlobalChatEvent");
            Class.forName("net.ess3.api.events.MuteStatusChangeEvent");
            Class.forName("net.ess3.api.events.NickChangeEvent");
            Class.forName("net.ess3.api.events.VanishStatusChangeEvent");
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

    protected Essentials get() {
        return (Essentials) discordSRV.server().getPluginManager().getPlugin("Essentials");
    }

    protected Task<User> getUser(UUID playerUUID) {
        return discordSRV.scheduler().supply(() -> get().getUsers().loadUncachedUser(playerUUID));
    }

    // Chat

    private final GlobalChannel channel = new GlobalChannel();

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onGlobalChat(GlobalChatEvent event) {
        Player player = event.getPlayer();
        MinecraftComponent component = ComponentUtil.toAPI(
                BukkitComponentSerializer.legacy().deserialize(event.getMessage())
        );

        BukkitPlayer srvPlayer = discordSRV.playerProvider().player(player);
        boolean cancelled = event.isCancelled();
        discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(
                new GameChatMessagePreProcessEvent(event, srvPlayer, component, channel, cancelled)
        ));
    }

    @Subscribe(priority = EventPriorities.LAST)
    public void onGameChannelLookup(GameChannelLookupEvent event) {
        if (!discordSRV.server().getPluginManager().isPluginEnabled("EssentialsChat")) {
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

        @Override
        public String toString() {
            return GameChannel.toString(this);
        }
    }

    // Mute

    @EventHandler(ignoreCancelled = true)
    public void onMuteStatusChange(MuteStatusChangeEvent event) {

    }

    @Override
    public Task<Punishment> getMute(@NotNull UUID playerUUID) {
        return getUser(playerUUID).thenApply(user -> new Punishment(
                Instant.ofEpochMilli(user.getMuteTimeout()),
                ComponentUtil.toAPI(BukkitComponentSerializer.legacy().deserialize(user.getMuteReason())),
                null
        ));
    }

    @Override
    public Task<Void> addMute(@NotNull UUID playerUUID, @Nullable Instant until, @Nullable MinecraftComponent reason, @NotNull MinecraftComponent punisher) {
        String reasonLegacy = reason != null ? BukkitComponentSerializer.legacy().serialize(ComponentUtil.fromAPI(reason)) : null;
        return getUser(playerUUID).thenApply(user -> {
            user.setMuted(true);
            user.setMuteTimeout(until != null ? until.toEpochMilli() : 0);
            user.setMuteReason(reasonLegacy);
            return null;
        });
    }

    @Override
    public Task<Void> removeMute(@NotNull UUID playerUUID) {
        return getUser(playerUUID).thenApply(user -> {
            user.setMuted(false);
            user.setMuteTimeout(0);
            user.setMuteReason(null);
            return null;
        });
    }

    // Nickname

    @EventHandler(ignoreCancelled = true)
    public void onNickChange(NickChangeEvent event) {
        NicknameSyncModule module = discordSRV.getModule(NicknameSyncModule.class);
        if (module != null) {
            module.newGameNickname(event.getAffected().getUUID(), event.getValue());
        }
    }

    @Override
    public Task<String> getNickname(UUID playerUUID) {
        return getUser(playerUUID).thenApply(UserData::getNickname);
    }

    @Override
    public Task<Void> setNickname(UUID playerUUID, String nickname) {
        return getUser(playerUUID).thenApply(user -> {
            user.setNickname(nickname);
            return null;
        });
    }

    // Vanish

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVanishStatusChange(VanishStatusChangeEvent event) {
        IUser user = event.getAffected();
        DiscordSRVPlayer player = discordSRV.playerProvider().player(user.getBase());

        discordSRV.eventBus().publish(new PlayerVanishStatusChangeEvent(player, event.getValue(), false, null));
    }
}
