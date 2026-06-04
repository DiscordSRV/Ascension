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

package com.discordsrv.common.integration.chat;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.channel.GameChannelLookupEvent;
import com.discordsrv.api.events.message.preprocess.game.GameChatMessagePreProcessEvent;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.PluginIntegration;
import net.draycia.carbon.api.CarbonChat;
import net.draycia.carbon.api.CarbonChatProvider;
import net.draycia.carbon.api.CarbonServer;
import net.draycia.carbon.api.channels.ChatChannel;
import net.draycia.carbon.api.channels.ChannelRegistry;
import net.draycia.carbon.api.event.CarbonEventSubscription;
import net.draycia.carbon.api.event.events.CarbonChatEvent;
import net.draycia.carbon.api.users.CarbonPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CarbonChatIntegration extends PluginIntegration<DiscordSRV> {
    private CarbonEventSubscription<CarbonChatEvent> chatSubscription;

    public CarbonChatIntegration(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "CARBONCHAT"));
    }

    @Override
    public @NotNull String getIntegrationId() {
        return "CarbonChat";
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("net.draycia.carbon.api.CarbonChatProvider");
        } catch (ClassNotFoundException ignored) {
            return false;
        }

        return super.isEnabled();
    }

    @Override
    public void enable() {
        CarbonChat carbonChat = CarbonChatProvider.carbonChat();

        chatSubscription = carbonChat.eventHandler().subscribe(CarbonChatEvent.class, event -> {
            if (event.cancelled()) {
                return;
            }

            CarbonPlayer sender = event.sender();
            if (sender.muted()) {
                return;
            }

            ChatChannel chatChannel = event.chatChannel();
            if (chatChannel == null) {
                return;
            }

            IPlayer srvPlayer = discordSRV.playerProvider().player(sender.uuid());
            if (srvPlayer == null) {
                return;
            }

            MinecraftComponent component = CarbonChatKeyHelper.message(event);
            discordSRV.scheduler().run(() -> discordSRV.eventBus().publish(
                    new GameChatMessagePreProcessEvent(event, srvPlayer, component, new CarbonGameChannel(chatChannel), event.cancelled())
            ));
        });
    }

    @Override
    public void disable() {
        if (chatSubscription != null) {
            chatSubscription.dispose();
            chatSubscription = null;
        }
    }

    @Subscribe
    public void onGameChannelLookup(GameChannelLookupEvent event) {
        CarbonChat carbonChat = CarbonChatProvider.carbonChat();
        ChannelRegistry registry = carbonChat.channelRegistry();

        ChatChannel channel;
        if (event.isDefault()) {
            channel = registry.defaultChannel();
        } else {
            channel = findChannel(registry, event.getChannelName());
        }

        if (channel != null) {
            event.process(new CarbonGameChannel(channel));
        }
    }

    private @Nullable ChatChannel findChannel(ChannelRegistry registry, String channelName) {
        return CarbonChatKeyHelper.findChannel(registry, channelName);
    }

    private class CarbonGameChannel implements GameChannel {
        private final ChatChannel channel;

        public CarbonGameChannel(ChatChannel channel) {
            this.channel = channel;
        }

        @Placeholder("command_name")
        public String getCommandName() {
            return channel.commandName();
        }

        @Placeholder("quick_prefix")
        public @Nullable String getQuickPrefix() {
            return channel.quickPrefix();
        }

        @Override
        public @NotNull String getOwnerName() {
            return getIntegrationId();
        }

        @Override
        public @NotNull String getChannelName() {
            return CarbonChatKeyHelper.channelName(channel);
        }

        @Override
        public boolean isChat() {
            return true;
        }

        @Override
        public @NotNull Set<DiscordSRVPlayer> getRecipients() {
            CarbonServer server = CarbonChatProvider.carbonChat().server();
            List<? extends CarbonPlayer> carbonPlayers = server.players();
            Set<DiscordSRVPlayer> players = new HashSet<>(carbonPlayers.size());

            for (CarbonPlayer carbonPlayer : carbonPlayers) {
                if (!channel.permissions().hearingPermitted(carbonPlayer).permitted()) {
                    continue;
                }

                if (!carbonPlayer.online()) {
                    continue;
                }

                IPlayer srvPlayer = discordSRV.playerProvider().player(carbonPlayer.uuid());
                if (srvPlayer != null) {
                    players.add(srvPlayer);
                }
            }

            return players;
        }

        @Override
        public String toString() {
            return GameChannel.toString(this);
        }
    }
}
