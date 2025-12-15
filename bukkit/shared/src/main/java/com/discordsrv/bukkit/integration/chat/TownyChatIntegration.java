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

package com.discordsrv.bukkit.integration.chat;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.channel.GameChannelLookupEvent;
import com.discordsrv.api.events.message.preprocess.game.GameChatMessagePreProcessEvent;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.player.BukkitPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.PluginIntegration;
import com.discordsrv.common.permission.game.Permission;
import com.discordsrv.common.util.ComponentUtil;
import com.palmergames.bukkit.TownyChat.Chat;
import com.palmergames.bukkit.TownyChat.channels.Channel;
import com.palmergames.bukkit.TownyChat.events.AsyncChatHookEvent;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TownyChatIntegration extends PluginIntegration<BukkitDiscordSRV> implements Listener {

    public TownyChatIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "TOWNYCHAT"));
    }

    @Override
    public @NotNull String getIntegrationId() {
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

        BukkitPlayer srvPlayer = discordSRV.playerProvider().player(player);
        boolean cancelled = event.isCancelled();
        discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(
                new GameChatMessagePreProcessEvent(event, srvPlayer, component, new TownyChatChannel(channel), cancelled)
        ));
    }

    @Subscribe
    public void onGameChannelLookup(GameChannelLookupEvent event) {
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
        private final TextColor messageColor;

        public TownyChatChannel(Channel channel) {
            this.channel = channel;

            TextComponent component = BukkitComponentSerializer.legacy().deserialize(channel.getMessageColour() + "a");
            List<TextColor> colors = ComponentUtil.extractColors(component);
            this.messageColor = colors.isEmpty() ? null : colors.get(0);
        }

        @Placeholder("tag")
        public String getTag() {
            return channel.getChannelTag();
        }

        @Placeholder("message_color")
        public TextColor getChannelColor() {
            return messageColor;
        }

        @Override
        public @NotNull String getOwnerName() {
            return getIntegrationId();
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
        public @NotNull Set<DiscordSRVPlayer> getRecipients() {
            Collection<BukkitPlayer> players = discordSRV.playerProvider().allPlayers();
            Set<DiscordSRVPlayer> filteredPlayers = new HashSet<>(players.size());

            for (BukkitPlayer player : players) {
                if (!channel.isPresent(player.username())) {
                    continue;
                }

                String permission = channel.getPermission();
                if (permission != null && !player.hasPermission(Permission.ofGeneric(permission))) {
                    continue;
                }

                filteredPlayers.add(player);
            }
            return filteredPlayers;
        }

        @Override
        public String toString() {
            return GameChannel.toString(this);
        }
    }
}
