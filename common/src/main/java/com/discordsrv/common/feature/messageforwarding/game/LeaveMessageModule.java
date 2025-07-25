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

package com.discordsrv.common.feature.messageforwarding.game;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.message.post.game.AbstractGameMessagePostEvent;
import com.discordsrv.api.events.message.post.game.LeaveMessagePostEvent;
import com.discordsrv.api.events.message.postprocess.game.LeaveMessagePostProcessEvent;
import com.discordsrv.api.events.message.preprocess.game.LeaveMessagePreProcessEvent;
import com.discordsrv.api.events.vanish.PlayerVanishStatusChangeEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.channels.LeaveMessageConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.events.player.PlayerConnectedEvent;
import com.discordsrv.common.permission.game.Permissions;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class LeaveMessageModule extends AbstractGameMessageModule<LeaveMessageConfig, LeaveMessagePreProcessEvent, LeaveMessagePostProcessEvent> {

    private final Map<UUID, Pair<Long, Future<?>>> playersJoinedRecently = new ConcurrentHashMap<>();
    private final ThreadLocal<Boolean> silentQuitPermission = new ThreadLocal<>();

    public LeaveMessageModule(DiscordSRV discordSRV) {
        super(discordSRV, "LEAVE_MESSAGES");
    }

    @Subscribe
    public void onPlayerConnected(PlayerConnectedEvent event) {
        UUID playerUUID = event.player().uniqueId();
        Pair<Long, Future<?>> pair = playersJoinedRecently.remove(playerUUID);
        if (pair != null) {
            pair.getValue().cancel(true);
        }

        long maxMS = 0;
        for (BaseChannelConfig channel : discordSRV.channelConfig().getAllChannels()) {
            long ms = channel.leaveMessages.ignoreIfJoinedWithinMS;
            if (maxMS < ms) {
                maxMS = ms;
            }
        }
        if (maxMS > 0) {
            long currentTime = System.currentTimeMillis();
            Future<?> removeFuture = discordSRV.scheduler().runLater(() -> playersJoinedRecently.remove(playerUUID), Duration.ofMillis(maxMS + 100));
            playersJoinedRecently.put(playerUUID, Pair.of(currentTime, removeFuture));
        }
    }

    @Subscribe(priority = EventPriorities.LAST, ignoreCancelled = false, ignoreProcessed = false)
    public void onLeaveMessageReceive(LeaveMessagePreProcessEvent event) {
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        DiscordSRVPlayer player = event.getPlayer();
        boolean silentQuit = player instanceof IPlayer && ((IPlayer) player).hasPermission(Permissions.SILENT_QUIT);
        discordSRV.scheduler().run(() -> {
            silentQuitPermission.set(silentQuit);
            process(event, player, event.getGameChannel());
        });
        event.markAsProcessed();
    }

    @Subscribe(priority = EventPriorities.LAST)
    public void onPlayerVanishStatusChange(PlayerVanishStatusChangeEvent event) {
        if (!event.isNewStatus() || !event.isSendFakeMessage()) {
            return;
        }

        // Player vanished
        discordSRV.eventBus().publish(new LeaveMessagePreProcessEvent(
                event,
                event.getPlayer(),
                event.getFakeMessage(),
                null,
                true,
                false,
                false
        ));
    }

    @Override
    protected Task<Void> forwardToChannel(
            @Nullable LeaveMessagePreProcessEvent event,
            @Nullable IPlayer player,
            @NotNull BaseChannelConfig config,
            @Nullable GameChannel channel
    ) {
        if (player != null) {
            Pair<Long, Future<?>> pair = playersJoinedRecently.get(player.uniqueId());
            if (pair != null) {
                long delta = System.currentTimeMillis() - pair.getKey();
                if (delta < config.leaveMessages.ignoreIfJoinedWithinMS) {
                    logger().info(player.username() + " joined within timeout period, join message will not be sent");
                    return Task.completed(null);
                }
            }
        }

        if (event != null && event.isMessageCancelled() && !config.leaveMessages.sendEvenIfCancelled) {
            return Task.completed(null);
        }
        if (player != null && config.leaveMessages.enableSilentPermission && silentQuitPermission.get()) {
            logger().info(player.username() + " is leaving silently, leave message will not be sent");
            return Task.completed(null);
        }
        if (player != null && event != null && event.isFakeLeave()) {
            if (!config.leaveMessages.sendFakeMessages) {
                logger().debug("Not sending fake leave message for " + player.username() + ", disabled in config");
                return Task.completed(null);
            } else {
                logger().info(player.username() + " vanished, sending fake leave message");
            }
        }
        if (!config.leaveMessages.sendMessageForVanishedPlayers
                && player != null && player.isVanished()
                && (event == null || !event.isFakeLeave())) {
            logger().info(player.username() + " left while vanished, leave message will not be sent");
            return Task.completed(null);
        }
        return super.forwardToChannel(event, player, config, channel);
    }

    @Override
    public LeaveMessageConfig mapConfig(BaseChannelConfig channelConfig) {
        return channelConfig.leaveMessages;
    }

    @Override
    protected LeaveMessagePostProcessEvent createPostProcessEvent(
            LeaveMessagePreProcessEvent preEvent,
            IPlayer player,
            List<DiscordGuildMessageChannel> channels,
            SendableDiscordMessage discordMessage
    ) {
        return new LeaveMessagePostProcessEvent(preEvent, player, channels, discordMessage);
    }

    @Override
    protected AbstractGameMessagePostEvent<LeaveMessagePostProcessEvent> createPostEvent(
            LeaveMessagePostProcessEvent preEvent,
            ReceivedDiscordMessageCluster cluster
    ) {
        return new LeaveMessagePostEvent(preEvent, cluster);
    }

    @Override
    public void setPlaceholders(
            LeaveMessageConfig config,
            LeaveMessagePreProcessEvent event,
            SendableDiscordMessage.Formatter formatter
    ) {
        formatter.addPlaceholder("message", event.getMessage());
    }
}
