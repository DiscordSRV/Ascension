/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.channel;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.ChannelUpdaterConfig;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.AbstractModule;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.managers.channel.ChannelManager;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ChannelUpdaterModule extends AbstractModule<DiscordSRV> {

    private final Set<ScheduledFuture<?>> futures = new LinkedHashSet<>();
    private boolean firstReload = true;

    public ChannelUpdaterModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "CHANNEL_UPDATER"));
    }

    @Override
    public void reload() {
        futures.forEach(future -> future.cancel(false));
        futures.clear();

        for (ChannelUpdaterConfig config : discordSRV.config().channelUpdaters) {
            futures.add(discordSRV.scheduler().runAtFixedRate(
                    () -> update(config),
                    firstReload ? 0 : config.timeMinutes,
                    config.timeMinutes,
                    TimeUnit.MINUTES
            ));
        }
        firstReload = false;
    }

    public void update(ChannelUpdaterConfig config) {
        try {
            // Wait a moment in case we're (re)connecting at the time
            discordSRV.waitForStatus(DiscordSRV.Status.CONNECTED, 15, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}

        JDA jda = discordSRV.jda().orElse(null);
        if (jda == null) {
            return;
        }

        String topicFormat = config.topicFormat;
        String nameFormat = config.nameFormat;

        if (topicFormat != null) {
            topicFormat = discordSRV.placeholderService().replacePlaceholders(topicFormat);
        }
        if (nameFormat != null) {
            nameFormat = discordSRV.placeholderService().replacePlaceholders(nameFormat);
        }

        for (Long channelId : config.channelIds) {
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
                        t -> discordSRV.logger().error("Failed to update channel " + channel, t)
                );
            } catch (Throwable t) {
                discordSRV.logger().error("Failed to update channel " + channel, t);
            }
        }
    }


}
