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

package com.discordsrv.common.feature.messageforwarding.game;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.message.post.game.AbstractGameMessagePostEvent;
import com.discordsrv.api.events.message.post.game.JoinMessagePostEvent;
import com.discordsrv.api.events.message.postprocess.game.JoinMessagePostProcessEvent;
import com.discordsrv.api.events.message.preprocess.game.JoinMessagePreProcessEvent;
import com.discordsrv.api.events.vanish.PlayerVanishStatusChangeEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.generic.IMessageConfig;
import com.discordsrv.common.events.player.PlayerDisconnectedEvent;
import com.discordsrv.common.permission.game.Permissions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class JoinMessageModule extends AbstractGameMessageModule<IMessageConfig, JoinMessagePreProcessEvent, JoinMessagePostProcessEvent> {

    private final Map<UUID, Future<?>> delayedTasks = new HashMap<>();
    private final ThreadLocal<Boolean> silentJoinPermission = new ThreadLocal<>();

    public JoinMessageModule(DiscordSRV discordSRV) {
        super(discordSRV, "JOIN_MESSAGES");
    }

    @Subscribe(priority = EventPriorities.LAST, ignoreCancelled = false, ignoreProcessed = false)
    public void onJoinMessageReceive(JoinMessagePreProcessEvent event) {
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        DiscordSRVPlayer player = event.getPlayer();
        boolean silentJoin = player instanceof IPlayer && ((IPlayer) player).hasPermission(Permissions.SILENT_JOIN);
        discordSRV.scheduler().run(() -> {
            silentJoinPermission.set(silentJoin);
            process(event, event.getPlayer(), event.getGameChannel());
        });
        event.markAsProcessed();
    }

    @Subscribe(priority = EventPriorities.LAST)
    public void onPlayerVanishStatusChange(PlayerVanishStatusChangeEvent event) {
        if (event.isNewStatus() || !event.isSendFakeMessage()) {
            return;
        }

        // Player unvanished
        discordSRV.eventBus().publish(new JoinMessagePreProcessEvent(
                event,
                event.getPlayer(),
                event.getFakeMessage(),
                null,
                false,
                true,
                false,
                false
        ));
    }

    @Override
    protected Task<Void> forwardToChannel(
            @Nullable JoinMessagePreProcessEvent event,
            @Nullable IPlayer player,
            @NotNull BaseChannelConfig config,
            @Nullable GameChannel channel
    ) {
        if (event != null && event.isMessageCancelled() && !config.joinMessages().sendEvenIfCancelled) {
            return Task.completed(null);
        }
        if (player != null && config.joinMessages().enableSilentPermission && silentJoinPermission.get()) {
            logger().info(player.username() + " is joining silently, join message will not be sent");
            return Task.completed(null);
        }
        if (player != null && event != null && event.isFakeJoin()) {
            if (!config.joinMessages().sendFakeJoinMessages) {
                logger().debug("Not sending fake join message for " + player.username() + ", disabled in config");
                return Task.completed(null);
            } else {
                logger().info(player.username() + " unvanished, sending fake join message");
            }
        }

        Supplier<Boolean> vanishCheck = () -> {
            if (!config.joinMessages().sendMessageForVanishedPlayers
                    && player != null && player.isVanished()
                    && (event == null || !event.isFakeJoin())) {
                logger().info(player.username() + " is vanished while joining, join message will not be sent");
                return true;
            }

            return false;
        };

        long delay = config.joinMessages().ignoreIfLeftWithinMS;
        if (player != null && delay > 0) {
            UUID playerUUID = player.uniqueId();

            synchronized (delayedTasks) {
                Task<Void> future = discordSRV.scheduler()
                        .supplyLater(() -> {
                            if (vanishCheck.get()) {
                                return Task.completed((Void) null);
                            }

                            return super.forwardToChannel(event, player, config, channel);
                        }, Duration.ofMillis(delay))
                        .then(r -> r)
                        .whenComplete((v, t) -> delayedTasks.remove(playerUUID));

                delayedTasks.put(playerUUID, future);
                return future;
            }
        }

        if (vanishCheck.get()) {
            return Task.completed(null);
        }
        return super.forwardToChannel(event, player, config, channel);
    }

    @Subscribe
    public void onPlayerDisconnected(PlayerDisconnectedEvent event) {
        IPlayer player = event.player();
        Future<?> future = delayedTasks.remove(player.uniqueId());
        if (future != null) {
            future.cancel(true);
            logger().info(player.username() + " left within timeout period, join message will not be sent");
        }
    }

    @Override
    public IMessageConfig mapConfig(JoinMessagePreProcessEvent event, BaseChannelConfig channelConfig) {
        return channelConfig.joinMessages().getForEvent(event);
    }

    @Override
    public IMessageConfig mapConfig(BaseChannelConfig channelConfig) {
        return channelConfig.joinMessages();
    }

    @Override
    protected JoinMessagePostProcessEvent createPostProcessEvent(
            JoinMessagePreProcessEvent preEvent,
            IPlayer player,
            List<DiscordGuildMessageChannel> channels,
            SendableDiscordMessage discordMessage
    ) {
        return new JoinMessagePostProcessEvent(preEvent, player, channels, discordMessage);
    }

    @Override
    protected AbstractGameMessagePostEvent<JoinMessagePostProcessEvent> createPostEvent(
            JoinMessagePostProcessEvent preEvent,
            ReceivedDiscordMessageCluster cluster
    ) {
        return new JoinMessagePostEvent(preEvent, cluster);
    }

    @Override
    public void setPlaceholders(
            IMessageConfig config,
            JoinMessagePreProcessEvent event,
            SendableDiscordMessage.Formatter formatter
    ) {
        formatter.addPlaceholder("message", event.getMessage());
    }
}
