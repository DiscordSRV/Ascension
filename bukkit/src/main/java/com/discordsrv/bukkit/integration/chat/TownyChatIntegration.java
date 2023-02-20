/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.channel.GameChannelLookupEvent;
import com.discordsrv.api.event.events.message.receive.game.GameChatMessageReceiveEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.player.BukkitPlayer;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.PluginIntegration;
import com.palmergames.bukkit.TownyChat.Chat;
import com.palmergames.bukkit.TownyChat.channels.Channel;
import com.palmergames.bukkit.TownyChat.events.AsyncChatHookEvent;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class TownyChatIntegration extends PluginIntegration<BukkitDiscordSRV> implements Listener {

    public TownyChatIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "TOWNYCHAT"));
    }

    @Override
    public @NotNull String getIntegrationName() {
        return "TownyChat";
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("com.palmergames.bukkit.TownyChat.Chat");
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

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onAsyncChatHook(AsyncChatHookEvent event) {
        Player player = event.getPlayer();
        Channel channel = event.getChannel();
        MinecraftComponent component = ComponentUtil.toAPI(
                BukkitComponentSerializer.legacy().deserialize(event.getMessage())
        );

        discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(
                new GameChatMessageReceiveEvent(
                        discordSRV.playerProvider().player(player),
                        new TownyChatChannel(channel),
                        component,
                        event.isCancelled()
                )
        ));
    }

    @Subscribe
    public void onGameChannelLookup(GameChannelLookupEvent event) {
        if (checkProcessor(event)) {
            return;
        }

        Chat chat = (Chat) Bukkit.getPluginManager().getPlugin("TownyChat");
        if (chat == null) {
            logger().debug("TownyChat main class == null");
            return;
        }

        Channel channel;
        if (event.isDefault()) {
            channel = chat.getChannelsHandler().getDefaultChannel();
        } else {
            channel = chat.getChannelsHandler().getChannel(event.getChannelName());
        }

        if (channel != null) {
            event.process(new TownyChatChannel(channel));
        }
    }

    private class TownyChatChannel implements GameChannel {

        private final Channel channel;

        public TownyChatChannel(Channel channel) {
            this.channel = channel;
        }

        @Override
        public @NotNull String getOwnerName() {
            return getIntegrationName();
        }

        @Override
        public @NotNull String getChannelName() {
            return channel.getName();
        }

        @Override
        public boolean isChat() {
            return true;
        }

        @Override
        public void sendMessage(@NotNull MinecraftComponent component) {
            for (BukkitPlayer player : discordSRV.playerProvider().allPlayers()) {
                if (!channel.isPresent(player.username())) {
                    continue;
                }

                String permission = channel.getPermission();
                if (permission != null && !player.hasPermission(permission)) {
                    continue;
                }

                player.sendMessage(ComponentUtil.fromAPI(component));
            }
        }
    }
}
