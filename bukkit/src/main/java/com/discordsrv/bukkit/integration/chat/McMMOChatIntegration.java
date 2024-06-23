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
import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.channel.GameChannelLookupEvent;
import com.discordsrv.api.event.events.message.receive.game.GameChatMessageReceiveEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.player.BukkitPlayer;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.PluginIntegration;
import com.gmail.nossr50.api.ChatAPI;
import com.gmail.nossr50.chat.author.Author;
import com.gmail.nossr50.chat.author.PlayerAuthor;
import com.gmail.nossr50.events.chat.McMMOAdminChatEvent;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.mcmmo.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

public class McMMOChatIntegration extends PluginIntegration<BukkitDiscordSRV> implements Listener {

    private final McMMOAdminChannel adminChannel = new McMMOAdminChannel();

    public McMMOChatIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "MCMMO"));
    }

    @Override
    public @NotNull String getIntegrationName() {
        return "mcMMO";
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("com.gmail.nossr50.mcMMO");
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

    @Subscribe(priority = EventPriority.EARLY)
    public void onGameChatMessageReceive(GameChatMessageReceiveEvent event) {
        Player player = discordSRV.server().getPlayer(event.getPlayer().uniqueId());
        if (!player.hasMetadata("mcMMO: Player Data")) {
            return;
        }

        if (ChatAPI.isUsingPartyChat(player)) {
            logger().debug(player.getName() + " is using party chat");
            event.setCancelled(true);
        } else if (ChatAPI.isUsingAdminChat(player)) {
            logger().debug(player.getName() + " is using admin chat");
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
    public void onMcMMOAdminChat(McMMOAdminChatEvent event) {
        Author author = event.getAuthor();
        if (!author.isPlayer()) return;

        Player player = ((PlayerAuthor) author).getPlayer();

        String json = GsonComponentSerializer.gson().serialize(event.getChatMessage().getChatMessage());
        MinecraftComponent component = ComponentUtil.toAPI(
                BukkitComponentSerializer.gson().deserialize(json)
        );

        BukkitPlayer srvPlayer = discordSRV.playerProvider().player(player);
        boolean cancelled = event.isCancelled();
        discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(
                new GameChatMessageReceiveEvent(event, srvPlayer, component, adminChannel, cancelled)
        ));
    }

    @Subscribe
    public void onGameChannelLookup(GameChannelLookupEvent event) {
        if (checkProcessor(event)) {
            return;
        }

        if (event.getChannelName().equalsIgnoreCase(adminChannel.getChannelName())) {
            event.process(adminChannel);
        }
    }

    private class McMMOAdminChannel implements GameChannel {

        @Override
        public @NotNull String getOwnerName() {
            return getIntegrationName();
        }

        @Override
        public @NotNull String getChannelName() {
            return "admin";
        }

        @Override
        public boolean isChat() {
            return true;
        }

        @Override
        public @NotNull Set<DiscordSRVPlayer> getRecipients() {
            return Collections.emptySet();
        }

        @Override
        public void sendMessage(@NotNull MinecraftComponent component) {
            mcMMO mcMMO = (mcMMO) discordSRV.server().getPluginManager().getPlugin("mcMMO");
            if (mcMMO == null) return;

            String message = BukkitComponentSerializer.legacy().serialize(ComponentUtil.fromAPI(component));
            mcMMO.getChatManager().processConsoleMessage(message);
        }
    }
}
