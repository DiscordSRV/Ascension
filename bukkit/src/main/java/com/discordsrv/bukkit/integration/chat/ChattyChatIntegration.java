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

package com.discordsrv.bukkit.integration.chat;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.channel.GameChannelLookupEvent;
import com.discordsrv.api.events.message.receive.game.GameChatMessageReceiveEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.player.BukkitPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.PluginIntegration;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import ru.mrbrikster.chatty.api.ChattyApi;
import ru.mrbrikster.chatty.api.chats.Chat;
import ru.mrbrikster.chatty.api.events.ChattyMessageEvent;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class ChattyChatIntegration extends PluginIntegration<BukkitDiscordSRV> implements Listener {

    public ChattyChatIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "CHATTY"));
    }

    @Override
    public @NotNull String getIntegrationName() {
        return "Chatty";
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("ru.mrbrikster.chatty.api.ChattyApi");
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
    public void onChattyMessage(ChattyMessageEvent event) {
        Player player = event.getPlayer();
        Chat chat = event.getChat();
        MinecraftComponent component = ComponentUtil.toAPI(
                BukkitComponentSerializer.legacy().deserialize(event.getMessage())
        );

        BukkitPlayer srvPlayer = discordSRV.playerProvider().player(player);
        discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(
                new GameChatMessageReceiveEvent(event, srvPlayer, component, new ChattyChannel(chat), false)
        ));
    }

    @Subscribe
    public void onGameChannelLookup(GameChannelLookupEvent event) {
        if (checkProcessor(event)) {
            return;
        }

        ChattyApi api = ChattyApi.get();
        if (api == null) {
            logger().error("ChattyApi == null");
            return;
        }

        api.getChat(event.getChannelName())
                .ifPresent(value -> event.process(new ChattyChannel(value)));
    }

    private class ChattyChannel implements GameChannel {

        private final Chat chat;

        public ChattyChannel(Chat chat) {
            this.chat = chat;
        }

        @Override
        public @NotNull String getOwnerName() {
            return getIntegrationName();
        }

        @Override
        public @NotNull String getChannelName() {
            return chat.getName();
        }

        @Override
        public boolean isChat() {
            return true;
        }

        @Override
        public @NotNull Set<DiscordSRVPlayer> getRecipients() {
            Collection<? extends Player> players = chat.getRecipients(null);
            Set<DiscordSRVPlayer> srvPlayers = new HashSet<>(players.size());
            for (Player player : players) {
                srvPlayers.add(discordSRV.playerProvider().player(player));
            }
            return srvPlayers;
        }
    }
}
