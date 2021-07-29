/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.listener;

import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.message.receive.game.ChatMessageReceiveEvent;
import com.discordsrv.api.event.events.message.send.game.ChatMessageSendEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.channels.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.ChannelConfig;
import com.discordsrv.common.function.OrDefault;
import com.discordsrv.common.player.util.PlayerUtil;
import com.discordsrv.common.string.util.Placeholders;
import dev.vankka.enhancedlegacytext.EnhancedLegacyText;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Collections;
import java.util.List;

public class DefaultChatListener extends AbstractListener {

    public DefaultChatListener(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Subscribe(priority = EventPriority.LAST)
    public void onChatReceive(ChatMessageReceiveEvent event) {
        if (checkProcessor(event) || checkCancellation(event)) {
            return;
        }

        GameChannel gameChannel = event.getGameChannel();
        Component message = ComponentUtil.fromAPI(event.message());
        Component displayName = PlayerUtil.displayName(event.getPlayer());

        OrDefault<BaseChannelConfig> channelConfig = discordSRV.channelConfig().orDefault(gameChannel);

        Component discordMessage = EnhancedLegacyText.get().buildComponent(channelConfig.map(cfg -> cfg.minecraftToDiscord).get(cfg -> cfg.messageFormat))
                .replace("%message%", message)
                .replace("%player_display_name%", displayName)
                .build();

        String username = new Placeholders(channelConfig.map(cfg -> cfg.minecraftToDiscord).get(cfg -> cfg.usernameFormat))
                .replace("%player_display_name%", () -> PlainTextComponentSerializer.plainText().serialize(displayName))
                .get();

        discordSRV.eventBus().publish(
                new ChatMessageSendEvent(
                        DiscordSerializer.INSTANCE.serialize(discordMessage),
                        username,
                        gameChannel
                )
        );
    }

    @Subscribe(priority = EventPriority.LAST)
    public void onChatSend(ChatMessageSendEvent event) {
        if (checkProcessor(event) || checkCancellation(event)) {
            return;
        }

        JDA jda = discordSRV.jda();
        if (jda == null) {
            return;
        }

        GameChannel channel = event.getTargetChannel();
        BaseChannelConfig channelConfig = discordSRV.channelConfig().get(channel);
        List<String> channelIds = channelConfig instanceof ChannelConfig ? ((ChannelConfig) channelConfig).channelIds : Collections.emptyList();
        if (channelIds.isEmpty()) {
            return;
        }

        for (String channelId : channelIds) {
            TextChannel textChannel = jda.getTextChannelById(channelId);
            if (textChannel == null) {
                continue;
            }

            textChannel.retrieveWebhooks().queue(webhooks -> {
                Webhook webhook = webhooks.get(0);
                WebhookMessage webhookMessage = new WebhookMessageBuilder()
                        .setUsername(event.getDiscordUsername())
                        .setContent(event.getDiscordMessage())
                        .build();
                WebhookClientBuilder.fromJDA(webhook).buildJDA().send(webhookMessage);
            });
        }
    }
}
