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

package com.discordsrv.common.command.game.commands.subcommand;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.command.game.abstraction.command.GameCommandArguments;
import com.discordsrv.common.command.game.abstraction.command.GameCommandExecutor;
import com.discordsrv.common.command.game.abstraction.command.GameCommandSuggester;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.config.helper.MinecraftMessage;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.helper.DestinationLookupHelper;
import com.discordsrv.common.permission.game.Permissions;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class BroadcastCommand implements GameCommandExecutor, GameCommandSuggester {

    private static GameCommand DISCORD;
    private static GameCommand MINECRAFT;
    private static GameCommand JSON;

    public static GameCommand discord(DiscordSRV discordSRV) {
        return make(discordSRV, config -> config.broadcastDiscordCommandDescription, "broadcastd",
                    () -> new Discord(discordSRV), () -> DISCORD, cmd -> DISCORD = cmd);
    }

    public static GameCommand minecraft(DiscordSRV discordSRV) {
        return make(discordSRV, config -> config.broadcastMinecraftCommandDescription, "broadcast",
                    () -> new Minecraft(discordSRV), () -> MINECRAFT, cmd -> MINECRAFT = cmd);
    }

    public static GameCommand json(DiscordSRV discordSRV) {
        return make(discordSRV, config -> config.broadcastRawCommandDescription, "broadcastraw",
                    () -> new Json(discordSRV), () -> JSON, cmd -> JSON = cmd);
    }

    private static GameCommand make(
            DiscordSRV discordSRV,
            Function<MessagesConfig, MinecraftMessage> translationFunction,
            String label,
            Supplier<? extends BroadcastCommand> executor,
            Supplier<GameCommand> supplier,
            Consumer<GameCommand> consumer
    ) {
        if (supplier.get() == null) {
            BroadcastCommand command = executor.get();
            consumer.accept(
                    GameCommand.literal(label)
                            .addDescriptionTranslations(discordSRV.getAllTranslations(translationFunction))
                            .requiredPermission(Permissions.COMMAND_BROADCAST)
                            .then(
                                    GameCommand.string("channel")
                                            .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.broadcastChannelParameterCommandDescription))
                                            .suggester(command)
                                            .then(
                                                    GameCommand.stringGreedy("content")
                                                            .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.broadcastMessageParameterCommandDescription))
                                                            .suggester((__, ___, ____) -> Collections.emptyList())
                                                            .executor(command)
                                            )
                            )
            );
        }

        return supplier.get();
    }

    protected final DiscordSRV discordSRV;

    private BroadcastCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public void execute(ICommandSender sender, GameCommandArguments arguments, GameCommand command, String rootAlias) {
        doExecute(sender, arguments);
    }

    @SuppressWarnings("unchecked")
    private <CC extends BaseChannelConfig & IChannelConfig> void doExecute(ICommandSender sender, GameCommandArguments arguments) {
        String channel = arguments.getString("channel");
        String content = arguments.getString("content");

        Task<DestinationLookupHelper.LookupResult> future = null;
        try {
            long id = Long.parseUnsignedLong(channel);

            DiscordMessageChannel messageChannel = discordSRV.discordAPI().getMessageChannelById(id);
            if (messageChannel instanceof DiscordGuildMessageChannel) {
                future = Task.completed(new DestinationLookupHelper.LookupResult(
                        Collections.singletonList((DiscordGuildMessageChannel) messageChannel),
                        Collections.emptyList()
                ));
            }
        } catch (IllegalArgumentException ignored) {
            BaseChannelConfig channelConfig = discordSRV.channelConfig().resolve(channel);
            CC config = channelConfig != null ? (CC) channelConfig : null;

            if (config != null) {
                future = discordSRV.destinations().lookupDestination(config.destination(), true, false);
            }
        }

        if (future == null) {
            future = Task.completed(null);
        }

        future.whenComplete((pair, t) -> doBroadcast(sender, content, channel, pair.channels(), pair.errors()));
    }

    @Override
    public List<String> suggestValues(
            ICommandSender sender,
            GameCommandArguments previousArguments,
            String currentInput
    ) {
        String input = currentInput.toLowerCase(Locale.ROOT);
        return discordSRV.channelConfig().getKeys().stream()
                .filter(key -> key.toLowerCase(Locale.ROOT).startsWith(input))
                .collect(Collectors.toList());
    }

    private void doBroadcast(ICommandSender sender, String content, String channel, List<DiscordGuildMessageChannel> channels, List<Throwable> errors) {
        boolean noChannels = channels == null || channels.isEmpty();
        if (noChannels || !errors.isEmpty()) {
            sender.sendMessage(ComponentUtil.fromAPI(
                    discordSRV.messagesConfig(sender).channelNotFound
                            .textBuilder()
                            .addPlaceholder("channel", channel)
                            .build()
            ));
        }
        if (noChannels) {
            return;
        }

        content = getContent(content);
        SendableDiscordMessage message = SendableDiscordMessage.builder()
                .setContent(content)
                .toFormatter()
                .applyPlaceholderService()
                .build();
        for (DiscordMessageChannel messageChannel : channels) {
            messageChannel.sendMessage(message);
        }
        sender.sendMessage(discordSRV.messagesConfig(sender).broadcasted.asComponent());
    }

    public abstract String getContent(String content);

    public static class Discord extends BroadcastCommand {

        public Discord(DiscordSRV discordSRV) {
            super(discordSRV);
        }

        @Override
        public String getContent(String content) {
            return content.replace("\\n", "\n");
        }
    }

    public static class Minecraft extends BroadcastCommand {

        public Minecraft(DiscordSRV discordSRV) {
            super(discordSRV);
        }

        @Override
        public String getContent(String content) {
            MinecraftComponent component = discordSRV.componentFactory()
                    .textBuilder(content)
                    .applyPlaceholderService()
                    .build();

            return discordSRV.componentFactory().discordSerialize(ComponentUtil.fromAPI(component));
        }
    }

    public static class Json extends BroadcastCommand {

        public Json(DiscordSRV discordSRV) {
            super(discordSRV);
        }

        @Override
        public String getContent(String content) {
            Component component = GsonComponentSerializer.gson().deserialize(content);
            return discordSRV.componentFactory().discordSerialize(component);
        }
    }
}
