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

package com.discordsrv.common.command.game.commands.subcommand;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.discord.entity.channel.DiscordGuildMessageChannel;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.command.game.abstraction.GameCommandArguments;
import com.discordsrv.common.command.game.abstraction.GameCommandExecutor;
import com.discordsrv.common.command.game.abstraction.GameCommandSuggester;
import com.discordsrv.common.command.game.sender.ICommandSender;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public abstract class BroadcastCommand implements GameCommandExecutor, GameCommandSuggester {

    private static GameCommand DISCORD;
    private static GameCommand MINECRAFT;
    private static GameCommand JSON;

    public static GameCommand discord(DiscordSRV discordSRV) {
        return make("broadcastd", () -> new Discord(discordSRV), () -> DISCORD, cmd -> DISCORD = cmd);
    }

    public static GameCommand minecraft(DiscordSRV discordSRV) {
        return make("broadcast", () -> new Minecraft(discordSRV), () -> MINECRAFT, cmd -> MINECRAFT = cmd);
    }

    public static GameCommand json(DiscordSRV discordSRV) {
        return make("broadcastraw", () -> new Json(discordSRV), () -> JSON, cmd -> JSON = cmd);
    }

    private static GameCommand make(
            String label,
            Supplier<? extends BroadcastCommand> executor,
            Supplier<GameCommand> supplier,
            Consumer<GameCommand> consumer
    ) {
        if (supplier.get() == null) {
            BroadcastCommand command = executor.get();
            consumer.accept(
                    GameCommand.literal(label)
                            .requiredPermission("discordsrv.admin.broadcast")
                            .then(
                                    GameCommand.string("channel")
                                            .suggester(command)
                                            .then(
                                                    GameCommand.stringGreedy("content")
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
    public void execute(ICommandSender sender, GameCommandArguments arguments, String label) {
        doExecute(sender, arguments);
    }

    @SuppressWarnings("unchecked") // Wacky generics
    private <CC extends BaseChannelConfig & IChannelConfig> void doExecute(ICommandSender sender, GameCommandArguments arguments) {
        String channel = arguments.getString("channel");
        String content = arguments.getString("content");

        List<DiscordMessageChannel> channels = new ArrayList<>();
        CompletableFuture<List<DiscordGuildMessageChannel>> future = null;
        try {
            long id = Long.parseUnsignedLong(channel);

            DiscordMessageChannel messageChannel = discordSRV.discordAPI().getMessageChannelById(id);
            if (messageChannel != null) {
                channels.add(messageChannel);
            }
        } catch (IllegalArgumentException ignored) {
            BaseChannelConfig channelConfig = discordSRV.channelConfig().resolve(null, channel);
            CC config = channelConfig instanceof IChannelConfig ? (CC) channelConfig : null;

            if (config != null) {
                future = discordSRV.discordAPI().findOrCreateDestinations(config, true, false);
            }
        }

        if (future != null) {
            future.whenComplete((messageChannels, t) -> doBroadcast(sender, content, channel, messageChannels));
        } else {
            doBroadcast(sender, content, channel, channels);
        }
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

    private void doBroadcast(ICommandSender sender, String content, String channel, List<? extends DiscordMessageChannel> channels) {
        if (channels.isEmpty()) {
            sender.sendMessage(
                    Component.text()
                            .append(Component.text("Channel ", NamedTextColor.RED))
                            .append(Component.text(channel, NamedTextColor.GRAY))
                            .append(Component.text(" not found", NamedTextColor.RED))
            );
            return;
        }


        SendableDiscordMessage message = getDiscordContent(content);
        for (DiscordMessageChannel messageChannel : channels) {
            messageChannel.sendMessage(message);
        }
        sender.sendMessage(Component.text("Broadcasted!", NamedTextColor.GRAY));
    }

    public SendableDiscordMessage getDiscordContent(String content) {
        content = getContent(content);
        return SendableDiscordMessage.builder().setContent(content).build();
    }

    public abstract String getContent(String content);

    public static class Discord extends BroadcastCommand {

        public Discord(DiscordSRV discordSRV) {
            super(discordSRV);
        }

        @Override
        public SendableDiscordMessage getDiscordContent(String content) {
            return SendableDiscordMessage.builder()
                    // Keep as is, allow newlines though
                    .setContent(content.replace("\\n", "\n"))
                    .toFormatter()
                    .applyPlaceholderService()
                    .build();
        }

        // See above
        @Override
        public String getContent(String content) {
            return null;
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

            return discordSRV.componentFactory().discordSerializer().serialize(ComponentUtil.fromAPI(component));
        }
    }

    public static class Json extends BroadcastCommand {

        public Json(DiscordSRV discordSRV) {
            super(discordSRV);
        }

        @Override
        public String getContent(String content) {
            Component component = GsonComponentSerializer.gson().deserialize(content);
            return discordSRV.componentFactory().discordSerializer().serialize(component);
        }
    }
}
