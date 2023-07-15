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
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.PluginIntegration;
import com.github.ucchyocean.lc3.LunaChatAPI;
import com.github.ucchyocean.lc3.LunaChatBukkit;
import com.github.ucchyocean.lc3.bukkit.event.LunaChatBukkitChannelChatEvent;
import com.github.ucchyocean.lc3.channel.Channel;
import com.github.ucchyocean.lc3.member.ChannelMember;
import com.github.ucchyocean.lc3.member.ChannelMemberPlayer;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class LunaChatIntegration extends PluginIntegration<BukkitDiscordSRV> implements Listener {

    public LunaChatIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "LUNACHAT"));
    }

    @Override
    public @NotNull String getIntegrationName() {
        return "LunaChat";
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("com.github.ucchyocean.lc3.LunaChatAPI");
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
    public void onLunaChatBukkitChannelChat(LunaChatBukkitChannelChatEvent event) {
        ChannelMember member = event.getMember();
        if (!(member instanceof ChannelMemberPlayer)) {
            return;
        }

        Player player = ((ChannelMemberPlayer) member).getPlayer();
        Channel channel = event.getChannel();
        MinecraftComponent component = ComponentUtil.toAPI(
                BukkitComponentSerializer.legacy().deserialize(event.getNgMaskedMessage())
        );

        discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(
                new GameChatMessageReceiveEvent(
                        event,
                        discordSRV.playerProvider().player(player),
                        component,
                        new LunaChatChannel(channel),
                        event.isCancelled()
                )
        ));
    }

    @Subscribe
    public void onGameChannelLookup(GameChannelLookupEvent event) {
        if (checkProcessor(event)) {
            return;
        }

        LunaChatBukkit lunaChat = LunaChatBukkit.getInstance();
        if (lunaChat == null) {
            logger().debug("LunaChatBukkit == null");
            return;
        }

        LunaChatAPI api = lunaChat.getLunaChatAPI();
        if (api == null) {
            logger().debug("LunaChatAPI == null");
            return;
        }

        Channel channel = api.getChannel(event.getChannelName());
        if (channel != null) {
            event.process(new LunaChatChannel(channel));
        }
    }

    private class LunaChatChannel implements GameChannel {

        private final Channel channel;

        public LunaChatChannel(Channel channel) {
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
            BaseComponent[] baseComponent = BungeeComponentSerializer.get().serialize(ComponentUtil.fromAPI(component));
            for (ChannelMember member : channel.getMembers()) {
                member.sendMessage(baseComponent);
            }
        }
    }

}
