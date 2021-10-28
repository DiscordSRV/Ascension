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

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.component.EnhancedTextBuilder;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.discord.api.entity.DiscordUser;
import com.discordsrv.api.discord.api.entity.channel.DiscordTextChannel;
import com.discordsrv.api.discord.api.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.api.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.discord.DiscordMessageReceivedEvent;
import com.discordsrv.api.event.events.message.receive.discord.DiscordMessageProcessingEvent;
import com.discordsrv.api.placeholder.util.Placeholders;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.renderer.DiscordSRVMinecraftRenderer;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.channels.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.DiscordToMinecraftChatConfig;
import com.discordsrv.common.function.OrDefault;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

public class DiscordChatListener extends AbstractListener {

    public DiscordChatListener(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Subscribe
    public void onGuildMessageReceived(DiscordMessageReceivedEvent event) {
        DiscordTextChannel channel = event.getTextChannel().orElse(null);
        if (channel == null || !discordSRV.isReady() || event.getMessage().isFromSelf()) {
            return;
        }

        discordSRV.eventBus().publish(
                new DiscordMessageProcessingEvent(
                        event.getMessage(),
                        channel));
    }

    @Subscribe
    public void onDiscordMessageReceive(DiscordMessageProcessingEvent event) {
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        DiscordTextChannel channel = event.getChannel();
        ReceivedDiscordMessage discordMessage = event.getDiscordMessage();
        DiscordUser author = discordMessage.getAuthor();
        Optional<DiscordGuildMember> member = discordMessage.getMember();
        boolean webhookMessage = discordMessage.isWebhookMessage();

        OrDefault<Pair<GameChannel, BaseChannelConfig>> channelPair = discordSRV.channelConfig().orDefault(channel);
        GameChannel gameChannel = channelPair.get(Pair::getKey);
        if (gameChannel == null) {
            return;
        }

        OrDefault<? extends BaseChannelConfig> channelConfig = channelPair.map(Pair::getValue);
        OrDefault<DiscordToMinecraftChatConfig> chatConfig = channelConfig.map(cfg -> cfg.discordToMinecraft);

        DiscordToMinecraftChatConfig.Ignores ignores = chatConfig.get(cfg -> cfg.ignores);
        if (ignores != null) {
            if (ignores.webhooks && webhookMessage) {
                return;
            } else if (ignores.bots && (author.isBot() && !webhookMessage)) {
                return;
            }

            DiscordToMinecraftChatConfig.Ignores.IDs users = ignores.usersAndWebhookIds;
            if (users != null && users.ids.contains(author.getId()) != users.whitelist) {
                return;
            }

            DiscordToMinecraftChatConfig.Ignores.IDs roles = ignores.roleIds;
            if (roles != null && member
                    .map(m -> m.getRoles().stream().anyMatch(role -> roles.ids.contains(role.getId())))
                    .map(hasRole -> hasRole != roles.whitelist)
                    .orElse(false)) {
                return;
            }
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

        Component messageComponent = DiscordSRVMinecraftRenderer.getWithGuildContext(channel.getGuild().getId(), () ->
                discordSRV.componentFactory().minecraftSerializer().serialize(message.toString()));

        EnhancedTextBuilder componentBuilder = discordSRV.componentFactory()
                .enhancedBuilder(format)
                .addContext(discordMessage, author)
                .addReplacement("%message%", messageComponent);
        member.ifPresent(componentBuilder::addContext);

        componentBuilder.applyPlaceholderService();

        MinecraftComponent component = componentBuilder.build();
        if (ComponentUtil.isEmpty(component)) {
            // Empty
            return;
        }

        gameChannel.sendMessage(component);
    }
}
