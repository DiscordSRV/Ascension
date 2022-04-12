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

package com.discordsrv.common.command.game.command.subcommand;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.discord.api.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.api.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.command.game.abstraction.GameCommandArguments;
import com.discordsrv.common.command.game.abstraction.GameCommandExecutor;
import com.discordsrv.common.command.game.sender.ICommandSender;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.function.OrDefault;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class BroadcastCommand implements GameCommandExecutor {

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
            Supplier<GameCommandExecutor> executor,
            Supplier<GameCommand> supplier,
            Consumer<GameCommand> consumer
    ) {
        if (supplier.get() == null) {
            consumer.accept(
                    GameCommand.literal(label)
                            .requiredPermission("discordsrv.admin.broadcast")
                            .then(
                                    GameCommand.stringWord("channel")
                                            .then(
                                                    GameCommand.stringGreedy("content")
                                                            .executor(executor.get())
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
    public void execute(ICommandSender sender, GameCommandArguments arguments) {
        String channel = arguments.getString("channel");
        String content = arguments.getString("content");

        List<DiscordMessageChannel> channels = new ArrayList<>();
        List<CompletableFuture<DiscordThreadChannel>> futures = new ArrayList<>();
        try {
            long id = Long.parseUnsignedLong(channel);
            discordSRV.discordAPI().getMessageChannelById(id).ifPresent(channels::add);
        } catch (IllegalArgumentException ignored) {
            OrDefault<BaseChannelConfig> channelConfig = discordSRV.channelConfig().orDefault(null, channel);
            IChannelConfig config = channelConfig.get(cfg -> cfg instanceof IChannelConfig ? (IChannelConfig) cfg : null);

            if (config != null) {
                for (Long channelId : config.channelIds()) {
                    discordSRV.discordAPI().getTextChannelById(channelId).ifPresent(channels::add);
                }

                discordSRV.discordAPI().findOrCreateThreads(channelConfig, config, channels::add, futures, false);
            }
        }

        if (!futures.isEmpty()) {
            CompletableFutureUtil.combine(futures).whenComplete((v, t) -> execute(sender, content, channel, channels));
        } else {
            execute(sender, content, channel, channels);
        }
    }

    private void execute(ICommandSender sender, String content, String channel, List<DiscordMessageChannel> channels) {
        if (channels.isEmpty()) {
            sender.sendMessage(
                    Component.text()
                            .append(Component.text("Channel ", NamedTextColor.RED))
                            .append(Component.text(channel, NamedTextColor.GRAY))
                            .append(Component.text(" not found", NamedTextColor.RED))
            );
            return;
        }

        content = getContent(content);

        for (DiscordMessageChannel messageChannel : channels) {
            messageChannel.sendMessage(SendableDiscordMessage.builder().setContent(content).build());
        }
        sender.sendMessage(Component.text("Broadcasted!", NamedTextColor.GRAY));
    }

    public abstract String getContent(String content);

    public static class Discord extends BroadcastCommand {

        public Discord(DiscordSRV discordSRV) {
            super(discordSRV);
        }

        @Override
        public String getContent(String content) {
            // Keep as is, allow newlines though
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
                    .enhancedBuilder(content)
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
