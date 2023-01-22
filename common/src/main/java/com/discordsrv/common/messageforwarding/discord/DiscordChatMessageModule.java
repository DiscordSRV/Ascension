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

package com.discordsrv.common.messageforwarding.discord;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.component.GameTextBuilder;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.events.message.DiscordMessageReceiveEvent;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.message.receive.discord.DiscordChatMessageProcessingEvent;
import com.discordsrv.api.placeholder.util.Placeholders;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.renderer.DiscordSRVMinecraftRenderer;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.DiscordIgnoresConfig;
import com.discordsrv.common.config.main.channels.DiscordToMinecraftChatConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.function.OrDefault;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.AbstractModule;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class DiscordChatMessageModule extends AbstractModule<DiscordSRV> {

    public DiscordChatMessageModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "DISCORD_TO_MINECRAFT"));
    }

    @Override
    public boolean isEnabled() {
        for (OrDefault<BaseChannelConfig> config : discordSRV.channelConfig().getAllChannels()) {
            if (config.map(cfg -> cfg.discordToMinecraft).get(cfg -> cfg.enabled, false)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull Collection<DiscordGatewayIntent> requiredIntents() {
        return Arrays.asList(DiscordGatewayIntent.GUILD_MESSAGES, DiscordGatewayIntent.MESSAGE_CONTENT);
    }

    @Subscribe
    public void onDiscordMessageReceived(DiscordMessageReceiveEvent event) {
        if (!discordSRV.isReady() || event.getMessage().isFromSelf()
                || !(event.getTextChannel() != null || event.getThreadChannel() != null)) {
            return;
        }

        discordSRV.eventBus().publish(new DiscordChatMessageProcessingEvent(event.getMessage(), event.getChannel()));
    }

    @Subscribe
    public void onDiscordChatMessageProcessing(DiscordChatMessageProcessingEvent event) {
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        Map<GameChannel, OrDefault<BaseChannelConfig>> channels = discordSRV.channelConfig().orDefault(event.getChannel());
        if (channels == null || channels.isEmpty()) {
            return;
        }

        for (Map.Entry<GameChannel, OrDefault<BaseChannelConfig>> entry : channels.entrySet()) {
            process(event, entry.getKey(), entry.getValue());
        }
        event.markAsProcessed();
    }

    private void process(DiscordChatMessageProcessingEvent event, GameChannel gameChannel, OrDefault<BaseChannelConfig> channelConfig) {
        OrDefault<DiscordToMinecraftChatConfig> chatConfig = channelConfig.map(cfg -> cfg.discordToMinecraft);
        if (!chatConfig.get(cfg -> cfg.enabled, true)) {
            return;
        }

        DiscordMessageChannel channel = event.getChannel();
        ReceivedDiscordMessage discordMessage = event.getDiscordMessage();
        DiscordUser author = discordMessage.getAuthor();
        DiscordGuildMember member = discordMessage.getMember();
        boolean webhookMessage = discordMessage.isWebhookMessage();

        DiscordIgnoresConfig ignores = chatConfig.get(cfg -> cfg.ignores);
        if (ignores != null && ignores.shouldBeIgnored(webhookMessage, author, member)) {
            // TODO: response for humans
            return;
        }

        String format = chatConfig.opt(cfg -> webhookMessage ? cfg.webhookFormat : cfg.format)
                .map(option -> option.replace("\\n", "\n"))
                .orElse(null);
        if (format == null) {
            return;
        }

        Placeholders message = new Placeholders(event.getMessageContent());
        chatConfig.opt(cfg -> cfg.contentRegexFilters)
                .ifPresent(filters -> filters.forEach(message::replaceAll));

        Component messageComponent = DiscordSRVMinecraftRenderer.getWithContext(event, chatConfig, () ->
                discordSRV.componentFactory().minecraftSerializer().serialize(message.toString()));

        GameTextBuilder componentBuilder = discordSRV.componentFactory()
                .textBuilder(format)
                .addContext(discordMessage, author, channel, channelConfig)
                .addReplacement("%message%", messageComponent);
        if (member != null) {
            componentBuilder.addContext(member);
        }

        componentBuilder.applyPlaceholderService();

        MinecraftComponent component = componentBuilder.build();
        if (ComponentUtil.isEmpty(component)) {
            // Empty
            return;
        }

        gameChannel.sendMessage(component);
    }
}
