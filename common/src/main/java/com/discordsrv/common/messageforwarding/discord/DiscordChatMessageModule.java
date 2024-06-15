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
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.discord.message.DiscordMessageDeleteEvent;
import com.discordsrv.api.event.events.discord.message.DiscordMessageReceiveEvent;
import com.discordsrv.api.event.events.discord.message.DiscordMessageUpdateEvent;
import com.discordsrv.api.event.events.message.forward.discord.DiscordChatMessageForwardedEvent;
import com.discordsrv.api.event.events.message.process.discord.DiscordChatMessageProcessEvent;
import com.discordsrv.api.event.events.message.receive.discord.DiscordChatMessageReceiveEvent;
import com.discordsrv.api.placeholder.util.Placeholders;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.renderer.DiscordSRVMinecraftRenderer;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.channels.DiscordToMinecraftChatConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.generic.DiscordIgnoresConfig;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.AbstractModule;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Pattern;

public class DiscordChatMessageModule extends AbstractModule<DiscordSRV> {

    // Filter for ASCII control characters which have no use being displayed, but might be misinterpreted somewhere
    // Notably this excludes, 0x09 HT (\t), 0x0A LF (\n), 0x0B VT (\v) and 0x0D CR (\r) (which may be used for text formatting)
    private static final Pattern ASCII_CONTROL_FILTER = Pattern.compile("[\\u0000-\\u0008\\u000C\\u000E-\\u001F\\u007F]");

    // A regex filter matching the unicode regular expression character category "Other Symbol"
    // https://unicode.org/reports/tr18/#General_Category_Property
    private static final Pattern EMOJI_FILTER = Pattern.compile("\\p{So}");

    private final Map<String, MessageSend> sends = new ConcurrentHashMap<>();

    public DiscordChatMessageModule(DiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "DISCORD_TO_MINECRAFT"));
    }

    public String getKey(ReceivedDiscordMessage message) {
        return getKey(message.getChannel(), message.getId());
    }

    public String getKey(DiscordMessageChannel channel, long messageId) {
        return Long.toUnsignedString(channel.getId()) + "-" + Long.toUnsignedString(messageId);
    }

    @Override
    public boolean isEnabled() {
        for (BaseChannelConfig config : discordSRV.channelConfig().getAllChannels()) {
            if (config.discordToMinecraft.enabled) {
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull Collection<DiscordGatewayIntent> requiredIntents() {
        return EnumSet.of(DiscordGatewayIntent.GUILD_MESSAGES, DiscordGatewayIntent.MESSAGE_CONTENT);
    }

    @Subscribe
    public void onDiscordMessageReceived(DiscordMessageReceiveEvent event) {
        if (!discordSRV.isReady() || event.getMessage().isFromSelf()
                || !(event.getTextChannel() != null || event.getThreadChannel() != null)) {
            return;
        }

        discordSRV.eventBus().publish(new DiscordChatMessageReceiveEvent(event.getMessage(), event.getChannel()));
    }

    @Subscribe
    public void onDiscordChatMessageReceive(DiscordChatMessageReceiveEvent event) {
        if (checkCancellation(event)) {
            return;
        }

        Map<GameChannel, BaseChannelConfig> channels = discordSRV.channelConfig().resolve(event.getChannel());
        if (channels == null || channels.isEmpty()) {
            return;
        }

        ReceivedDiscordMessage message = event.getMessage();

        for (Map.Entry<GameChannel, BaseChannelConfig> entry : channels.entrySet()) {
            GameChannel gameChannel = entry.getKey();
            BaseChannelConfig config = entry.getValue();
            if (!config.discordToMinecraft.enabled) {
                continue;
            }

            long delayMillis = config.discordToMinecraft.delayMillis;
            if (delayMillis == 0) {
                process(message, gameChannel, config);
                return;
            }

            String key = getKey(message);
            MessageSend send = new MessageSend(message, gameChannel, config);

            sends.put(key, send);
            send.setFuture(discordSRV.scheduler().runLater(() -> processSend(key), Duration.ofMillis(delayMillis)));
        }
    }

    private void processSend(String key) {
        MessageSend send = sends.remove(key);
        if (send != null) {
            process(send.getMessage(), send.getGameChannel(), send.getConfig());
        }
    }

    @Subscribe
    public void onDiscordMessageUpdate(DiscordMessageUpdateEvent event) {
        ReceivedDiscordMessage message = event.getMessage();
        MessageSend send = sends.get(getKey(message));
        if (send != null) {
            send.setMessage(message);
        }
    }

    @Subscribe
    public void onDiscordMessageDelete(DiscordMessageDeleteEvent event) {
        MessageSend send = sends.remove(getKey(event.getChannel(), event.getMessageId()));
        if (send != null) {
            send.getFuture().cancel(false);
        }
    }

    private void process(ReceivedDiscordMessage discordMessage, GameChannel gameChannel, BaseChannelConfig channelConfig) {
        DiscordChatMessageProcessEvent event = new DiscordChatMessageProcessEvent(discordMessage.getChannel(), discordMessage, gameChannel);
        discordSRV.eventBus().publish(event);
        if (checkCancellation(event) || checkProcessor(event)) {
            return;
        }

        DiscordToMinecraftChatConfig chatConfig = channelConfig.discordToMinecraft;
        if (!chatConfig.enabled) {
            return;
        }

        DiscordGuild guild = discordMessage.getGuild();
        DiscordMessageChannel channel = discordMessage.getChannel();
        DiscordUser author = discordMessage.getAuthor();
        DiscordGuildMember member = discordMessage.getMember();
        boolean webhookMessage = discordMessage.isWebhookMessage();

        DiscordIgnoresConfig ignores = chatConfig.ignores;
        if (ignores != null && ignores.shouldBeIgnored(webhookMessage, author, member)) {
            // TODO: response for humans
            return;
        }

        String format = webhookMessage ? chatConfig.webhookFormat : chatConfig.format;
        if (StringUtils.isBlank(format)) {
            return;
        }

        Placeholders message = new Placeholders(event.getContent());
        message.replaceAll(ASCII_CONTROL_FILTER, "");
        if (chatConfig.unicodeEmojiBehaviour == DiscordToMinecraftChatConfig.EmojiBehaviour.HIDE) {
            message.replaceAll(EMOJI_FILTER, "");
        }
        chatConfig.contentRegexFilters.forEach(message::replaceAll);

        boolean attachments = !discordMessage.getAttachments().isEmpty() && format.contains("message_attachments");
        String finalMessage = message.toString();
        if (finalMessage.trim().isEmpty() && !attachments) {
            // No sending empty messages
            return;
        }

        Component messageComponent = DiscordSRVMinecraftRenderer.getWithContext(guild, chatConfig, () ->
                discordSRV.componentFactory().minecraftSerializer().serialize(finalMessage));

        if (discordSRV.componentFactory().plainSerializer().serialize(messageComponent).trim().isEmpty() && !attachments) {
            // Check empty-ness again after rendering
            return;
        }

        GameTextBuilder componentBuilder = discordSRV.componentFactory()
                .textBuilder(format)
                .addContext(discordMessage, author, member, channel, channelConfig)
                .applyPlaceholderService()
                .addPlaceholder("message", messageComponent);

        MinecraftComponent component = DiscordSRVMinecraftRenderer.getWithContext(guild, chatConfig, componentBuilder::build);
        if (ComponentUtil.isEmpty(component)) {
            // Empty
            return;
        }

        gameChannel.sendMessage(component);

        Collection<? extends DiscordSRVPlayer> players = gameChannel.getRecipients();
        for (DiscordSRVPlayer player : players) {
            gameChannel.sendMessageToPlayer(player, component);
        }

        discordSRV.eventBus().publish(new DiscordChatMessageForwardedEvent(component, gameChannel));
    }

    public static class MessageSend {

        private ReceivedDiscordMessage message;
        private final GameChannel gameChannel;
        private final BaseChannelConfig config;
        private ScheduledFuture<?> future;

        public MessageSend(ReceivedDiscordMessage message, GameChannel gameChannel, BaseChannelConfig config) {
            this.message = message;
            this.gameChannel = gameChannel;
            this.config = config;
        }

        public ReceivedDiscordMessage getMessage() {
            return message;
        }

        public void setMessage(ReceivedDiscordMessage message) {
            this.message = message;
        }

        public GameChannel getGameChannel() {
            return gameChannel;
        }

        public BaseChannelConfig getConfig() {
            return config;
        }

        public ScheduledFuture<?> getFuture() {
            return future;
        }

        public void setFuture(ScheduledFuture<?> future) {
            this.future = future;
        }
    }
}
