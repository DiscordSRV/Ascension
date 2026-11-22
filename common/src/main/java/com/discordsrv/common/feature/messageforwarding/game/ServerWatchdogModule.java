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

import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessageCluster;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.message.post.game.AbstractGameMessagePostEvent;
import com.discordsrv.api.events.message.post.game.WatchdogMessagePostEvent;
import com.discordsrv.api.events.message.postprocess.game.WatchdogMessagePostProcessEvent;
import com.discordsrv.api.events.message.preprocess.game.WatchdogMessagePreProcessEvent;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.channels.base.server.ServerBaseChannelConfig;
import com.discordsrv.common.config.main.channels.server.ServerWatchdogConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.core.scheduler.ServerScheduler;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class ServerWatchdogModule extends AbstractGameMessageModule<ServerWatchdogConfig, WatchdogMessagePreProcessEvent, WatchdogMessagePostProcessEvent> {

    private final AtomicLong lastTick = new AtomicLong(System.currentTimeMillis());
    private volatile boolean triggered;
    private ScheduledFuture<?> monitorFuture;
    private int timeout;

    public ServerWatchdogModule(DiscordSRV discordSRV) {
        super(discordSRV, "SERVER_WATCHDOG");
    }

    @Override
    public void serverStarted() {
        lastTick.set(System.currentTimeMillis());
        triggered = false;

        if (discordSRV.scheduler() instanceof ServerScheduler serverScheduler) {
            serverScheduler.runOnMainThreadAtFixedRateInTicks(this::tick, ServerScheduler.TICKS_PER_SECOND);
        }

        monitorFuture = discordSRV.scheduler().runAtFixedRate(this::monitor, Duration.ofSeconds(1));
    }

    @Override
    public boolean isEnabled() {
        return discordSRV.serverType() == DiscordSRV.ServerType.SERVER && super.isEnabled();
    }

    @Override
    public void reload(Consumer<ReloadResult> resultConsumer) {
        timeout = 60;
        for (BaseChannelConfig channelConfig : discordSRV.channelConfig().getAllChannels()) {
            if (channelConfig instanceof ServerBaseChannelConfig serverBaseChannelConfig && serverBaseChannelConfig.watchdog.enabled()) {
                timeout = Math.max(10, serverBaseChannelConfig.watchdog.timeoutSeconds);
                break;
            }
        }
    }

    private void tick() {
        lastTick.set(System.currentTimeMillis());
        triggered = false;
    }

    private void monitor() {
        if (!isEnabled()) {
            return;
        }

        long elapsedMillis = System.currentTimeMillis() - lastTick.get();

        if (triggered || elapsedMillis < timeout * 1000L) {
            return;
        }

        triggered = true;
        WatchdogMessagePreProcessEvent preEvent = new WatchdogMessagePreProcessEvent(this, timeout, ZonedDateTime.now());
        discordSRV.eventBus().publish(preEvent);

        if (checkCancellation(preEvent)) {
            return;
        }

        process(preEvent, null, null);
        preEvent.markAsProcessed();
    }

    @Override
    public void disable() {
        if (monitorFuture != null) {
            monitorFuture.cancel(false);
            monitorFuture = null;
        }
        triggered = false;
    }

    @Override
    public ServerWatchdogConfig mapConfig(BaseChannelConfig channelConfig) {
        return ((ServerBaseChannelConfig) channelConfig).watchdog;
    }

    @Override
    protected WatchdogMessagePostProcessEvent createPostProcessEvent(
            WatchdogMessagePreProcessEvent preEvent,
            IPlayer player,
            List<DiscordGuildMessageChannel> channels,
            SendableDiscordMessage discordMessage
    ) {
        return new WatchdogMessagePostProcessEvent(
                preEvent,
                channels,
                discordMessage
        );
    }

    @Override
    protected AbstractGameMessagePostEvent<WatchdogMessagePostProcessEvent> createPostEvent(
            WatchdogMessagePostProcessEvent preEvent,
            ReceivedDiscordMessageCluster cluster
    ) {
        return new WatchdogMessagePostEvent(preEvent, cluster);
    }

    @Override
    public void setPlaceholders(
            ServerWatchdogConfig config,
            WatchdogMessagePreProcessEvent event,
            SendableDiscordMessage.Formatter formatter
    ) {
        formatter.addPlaceholder("watchdog_timeout", event.getTimeout());
        formatter.addPlaceholder("watchdog_date", event.getTriggeredAt());
    }

    @Override
    public int shutdownOrder() {
        return Integer.MAX_VALUE;
    }
}
