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

package com.discordsrv.common.component.renderer;

import com.discordsrv.api.component.GameTextBuilder;
import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.event.events.message.receive.discord.DiscordChatMessageProcessingEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.channels.DiscordToMinecraftChatConfig;
import com.discordsrv.common.function.OrDefault;
import dev.vankka.mcdiscordreserializer.renderer.implementation.DefaultMinecraftRenderer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class DiscordSRVMinecraftRenderer extends DefaultMinecraftRenderer {

    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();
    private final DiscordSRV discordSRV;

    public DiscordSRVMinecraftRenderer(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    public static void runInContext(
            DiscordChatMessageProcessingEvent event,
            OrDefault<DiscordToMinecraftChatConfig> config,
            Runnable runnable
    ) {
        getWithContext(event, config, () -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T getWithContext(
            DiscordChatMessageProcessingEvent event,
            OrDefault<DiscordToMinecraftChatConfig> config,
            Supplier<T> supplier
    ) {
        Context oldValue = CONTEXT.get();
        CONTEXT.set(new Context(event, config));
        T output = supplier.get();
        CONTEXT.set(oldValue);
        return output;
    }

    @Override
    public @NotNull Component appendChannelMention(@NotNull Component component, @NotNull String id) {
        Context context = CONTEXT.get();
        DiscordToMinecraftChatConfig.Mentions.Format format =
                context != null ? context.config.map(cfg -> cfg.mentions).get(cfg -> cfg.channel) : null;
        if (format == null) {
            return component.append(Component.text("<#" + id + ">"));
        }

        JDA jda = discordSRV.jda();
        if (jda == null) {
            return Component.empty();
        }

        GuildChannel guildChannel = jda.getGuildChannelById(id);

        return component.append(ComponentUtil.fromAPI(
                discordSRV.componentFactory()
                        .textBuilder(guildChannel != null ? format.format : format.unknownFormat)
                        .addReplacement("%channel_name%", guildChannel != null ? guildChannel.getName() : null)
                        .applyPlaceholderService()
                        .build()
        ));
    }

    @Override
    public @NotNull Component appendUserMention(@NotNull Component component, @NotNull String id) {
        Context context = CONTEXT.get();
        DiscordToMinecraftChatConfig.Mentions.Format format =
                context != null ? context.config.map(cfg -> cfg.mentions).get(cfg -> cfg.user) : null;
        DiscordGuild guild = context != null
                             ? discordSRV.discordAPI().getGuildById(context.event.getGuild().getId())
                             : null;
        if (format == null || guild == null) {
            return component.append(Component.text("<@" + id + ">"));
        }

        long userId = MiscUtil.parseLong(id);
        DiscordUser user = discordSRV.discordAPI().getUserById(userId);
        DiscordGuildMember member = guild.getMemberById(userId);

        GameTextBuilder builder = discordSRV.componentFactory()
                .textBuilder(user != null ? format.format : format.unknownFormat);

        if (user != null) {
            builder.addContext(user);
        }
        if (member != null) {
            builder.addContext(member);
        }

        return component.append(ComponentUtil.fromAPI(
                builder.applyPlaceholderService().build()
        ));
    }

    @Override
    public @NotNull Component appendRoleMention(@NotNull Component component, @NotNull String id) {
        Context context = CONTEXT.get();
        DiscordToMinecraftChatConfig.Mentions.Format format =
                context != null ? context.config.map(cfg -> cfg.mentions).get(cfg -> cfg.role) : null;
        if (format == null) {
            return component.append(Component.text("<#" + id + ">"));
        }

        long roleId = MiscUtil.parseLong(id);
        DiscordRole role = discordSRV.discordAPI().getRoleById(roleId);

        GameTextBuilder builder = discordSRV.componentFactory()
                .textBuilder(role != null ? format.format : format.unknownFormat);

        if (role != null) {
            builder.addContext(role);
        }

        return component.append(ComponentUtil.fromAPI(
                builder.applyPlaceholderService().build()
        ));
    }

    private static class Context {

        private final DiscordChatMessageProcessingEvent event;
        private final OrDefault<DiscordToMinecraftChatConfig> config;

        public Context(DiscordChatMessageProcessingEvent event, OrDefault<DiscordToMinecraftChatConfig> config) {
            this.event = event;
            this.config = config;
        }
    }
}
