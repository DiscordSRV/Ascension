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
import com.discordsrv.common.util.ComponentUtil;
import mineverse.Aust1n46.chat.api.MineverseChatAPI;
import mineverse.Aust1n46.chat.api.MineverseChatPlayer;
import mineverse.Aust1n46.chat.api.events.VentureChatEvent;
import mineverse.Aust1n46.chat.channel.ChatChannel;
import mineverse.Aust1n46.chat.utilities.Format;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class VentureChatIntegration extends PluginIntegration<BukkitDiscordSRV> implements Listener {

    public VentureChatIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "VENTURECHAT"));
    }

    @Override
    public @NotNull String getIntegrationId() {
        return "VentureChat";
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("mineverse.Aust1n46.chat.api.events.VentureChatEvent");
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
    public void onAsyncChatHook(VentureChatEvent event) {
        MineverseChatPlayer chatPlayer = event.getMineverseChatPlayer();

        Player player = chatPlayer.getPlayer();
        if (player == null) {
            logger().debug("Bukkit player == null for " + chatPlayer.getName());
            return;
        }

        ChatChannel channel = event.getChannel();
        MinecraftComponent component = ComponentUtil.toAPI(
                BukkitComponentSerializer.legacy().deserialize(event.getChat())
        );

        BukkitPlayer srvPlayer = discordSRV.playerProvider().player(player);
        discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(
                new GameChatMessagePreProcessEvent(event, srvPlayer, component, new VentureChatChannel(channel), false)
        ));
    }

    @Subscribe
    public void onGameChannelLookup(GameChannelLookupEvent event) {
        ChatChannel channel;
        if (event.isDefault()) {
            channel = ChatChannel.getDefaultChannel();
        } else {
            channel = ChatChannel.getChannel(event.getChannelName());
        }

        if (channel != null) {
            event.process(new VentureChatChannel(channel));
        }
    }

    private class VentureChatChannel implements GameChannel {

        private final ChatChannel channel;
        private final TextColor color;
        private final TextColor chatColor;

        public VentureChatChannel(ChatChannel channel) {
            this.channel = channel;

            TextComponent colorComponent = BukkitComponentSerializer.legacy().deserialize(channel.getColor() + "a");
            List<TextColor> colors = ComponentUtil.extractColors(colorComponent);
            this.color = colors.isEmpty() ? null : colors.get(0);

            TextComponent chatColorComponent = BukkitComponentSerializer.legacy().deserialize(channel.getChatColor() + "a");
            List<TextColor> chatColors = ComponentUtil.extractColors(chatColorComponent);
            this.chatColor = chatColors.isEmpty() ? null : chatColors.get(0);
        }

        @Placeholder("color")
        public TextColor getColor() {
            return color;
        }

        @Placeholder("chat_color")
        public TextColor getChatColor() {
            return chatColor;
        }

        @Placeholder("alias")
        public String getAlias() {
            return channel.getAlias();
        }

        @Placeholder("prefix")
        public String getPrefix() {
            return channel.getPrefix();
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
            Collection<MineverseChatPlayer> chatPlayers = MineverseChatAPI.getMineverseChatPlayers();
            Set<DiscordSRVPlayer> players = new HashSet<>(chatPlayers.size());

            for (MineverseChatPlayer chatPlayer : chatPlayers) {
                if (!chatPlayer.isListening(channel.getName())) {
                    continue;
                }

                Player bukkitPlayer = chatPlayer.getPlayer();
                if (bukkitPlayer == null) {
                    continue;
                }

                if (channel.hasPermission() && !bukkitPlayer.hasPermission(channel.getPermission())) {
                    continue;
                }

                players.add(discordSRV.playerProvider().player(bukkitPlayer));
            }
            return players;
        }

        @Override
        public void sendMessageToPlayer(@NotNull DiscordSRVPlayer player, @NotNull MinecraftComponent component) {
            MineverseChatPlayer chatPlayer = MineverseChatAPI.getMineverseChatPlayer(player.uniqueId());

            if (chatPlayer.hasFilter() && channel.isFiltered()) {
                component = ComponentUtil.toAPI(
                        ComponentUtil.fromAPI(component)
                                .replaceText(
                                        TextReplacementConfig.builder()
                                                .match(Pattern.compile("[\\w\\W]+"))
                                                .replacement(match -> match.content(Format.FilterChat(match.content())))
                                                .build()
                                )
                );
            }

            player.sendMessageFromDiscord(component);
        }
    }
}
