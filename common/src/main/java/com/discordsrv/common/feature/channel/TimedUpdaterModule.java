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

package com.discordsrv.common.feature.channel;

import com.discordsrv.api.discord.connection.jda.errorresponse.ErrorCallbackContext;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.TimedUpdaterConfig;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.managers.channel.ChannelManager;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TimedUpdaterModule extends AbstractModule<DiscordSRV> {

    private final Map<TimedUpdaterConfig.UpdaterConfig, ScheduledFuture<?>> activeUpdaters = new LinkedHashMap<>();
    private boolean firstReload = true;

    public TimedUpdaterModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "CHANNEL_UPDATER"));
    }

    @Override
    public boolean isEnabled() {
        boolean any = false;

        TimedUpdaterConfig config = discordSRV.config().timedUpdater;
        for (TimedUpdaterConfig.UpdaterConfig updaterConfig : config.getConfigs()) {
            if (updaterConfig.any()) {
                any = true;
                break;
            }
        }
        if (!any) {
            return false;
        }

        return super.isEnabled() && discordSRV.isReady() && discordSRV.isServerStarted();
    }

    private Set<TimedUpdaterConfig.UpdaterConfig> cancelFutures() {
        Set<TimedUpdaterConfig.UpdaterConfig> activeConfigs = new LinkedHashSet<>(activeUpdaters.size());
        for (Map.Entry<TimedUpdaterConfig.UpdaterConfig, ScheduledFuture<?>> entry : activeUpdaters.entrySet()) {
            activeConfigs.add(entry.getKey());

            entry.getValue().cancel(false);
        }

        return activeConfigs;
    }

    @Override
    public void serverShuttingDown() {
        Set<TimedUpdaterConfig.UpdaterConfig> configs = cancelFutures();
        for (TimedUpdaterConfig.UpdaterConfig updater : configs) {
            update(updater, true);
        }
    }

    @Override
    public void reload(Consumer<ReloadResult> resultConsumer) {
        cancelFutures();

        TimedUpdaterConfig config = discordSRV.config().timedUpdater;
        for (TimedUpdaterConfig.UpdaterConfig updaterConfig : config.getConfigs()) {
            long time = Math.max(updaterConfig.timeSeconds(), updaterConfig.minimumSeconds());
            activeUpdaters.put(
                    updaterConfig,
                    discordSRV.scheduler().runAtFixedRate(
                            () -> update(updaterConfig, false),
                            firstReload ? Duration.ZERO : Duration.ofSeconds(time),
                            Duration.ofSeconds(time)
                    )
            );
        }
        firstReload = false;
    }

    public void update(TimedUpdaterConfig.UpdaterConfig config, boolean shutdown) {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            return;
        }

        if (config instanceof TimedUpdaterConfig.VoiceChannelConfig) {
            TimedUpdaterConfig.VoiceChannelConfig voiceConfig = (TimedUpdaterConfig.VoiceChannelConfig) config;
            updateChannel(
                    jda,
                    voiceConfig.channelIds,
                    shutdown ? voiceConfig.shutdownNameFormat : voiceConfig.nameFormat,
                    null
            );
        } else if (config instanceof TimedUpdaterConfig.TextChannelConfig) {
            TimedUpdaterConfig.TextChannelConfig textConfig = (TimedUpdaterConfig.TextChannelConfig) config;
            updateChannel(
                    jda,
                    textConfig.channelIds,
                    shutdown ? textConfig.shutdownNameFormat : textConfig.nameFormat,
                    shutdown ? textConfig.shutdownTopicFormat : textConfig.topicFormat
            );
        }
    }

    private void updateChannel(JDA jda, List<Long> channelIds, String nameFormat, String topicFormat) {
        if (topicFormat != null) {
            topicFormat = discordSRV.placeholderService().replacePlaceholders(topicFormat);
        }
        if (nameFormat != null) {
            nameFormat = discordSRV.placeholderService().replacePlaceholders(nameFormat);
        }

        for (Long channelId : channelIds) {
            GuildChannel channel = jda.getGuildChannelById(channelId);
            if (channel == null) {
                continue;
            }

            try {
                ChannelManager<?, ?> manager = channel.getManager();
                boolean anythingChanged = false;
                if (manager instanceof TextChannelManager && channel instanceof TextChannel
                        && StringUtils.isNotEmpty(topicFormat)
                        && !topicFormat.equals(((TextChannel) channel).getTopic())) {
                    anythingChanged = true;
                    manager = ((TextChannelManager) manager).setTopic(topicFormat);
                }
                if (StringUtils.isNotEmpty(nameFormat) && !nameFormat.equals(channel.getName())) {
                    anythingChanged = true;
                    manager = manager.setName(nameFormat);
                }

                if (!anythingChanged) {
                    logger().debug("Skipping updating channel " + channel + ": nothing changed");
                    continue;
                }

                manager.timeout(30, TimeUnit.SECONDS).queue(
                        null,
                        ErrorCallbackContext.context("Failed to update channel " + channel)
                );
            } catch (Throwable t) {
                discordSRV.logger().error("Failed to update channel " + channel, t);
            }
        }
    }

}
