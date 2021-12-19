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

package com.discordsrv.common.component.renderer;

import com.discordsrv.api.component.EnhancedTextBuilder;
import com.discordsrv.api.discord.api.entity.DiscordUser;
import com.discordsrv.api.discord.api.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.api.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.api.entity.guild.DiscordRole;
import com.discordsrv.api.event.events.message.receive.discord.DiscordMessageProcessingEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.main.channels.DiscordToMinecraftChatConfig;
import com.discordsrv.common.function.OrDefault;
import dev.vankka.mcdiscordreserializer.renderer.implementation.DefaultMinecraftRenderer;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.GuildChannel;
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
            DiscordMessageProcessingEvent event,
            OrDefault<DiscordToMinecraftChatConfig> config,
            Runnable runnable
    ) {
        getWithContext(event, config, () -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T getWithContext(
            DiscordMessageProcessingEvent event,
            OrDefault<DiscordToMinecraftChatConfig> config,
            Supplier<T> supplier
    ) {
        CONTEXT.set(new Context(event, config));
        T output = supplier.get();
        CONTEXT.remove();
        return output;
    }

    @Override
    public @NotNull Component appendChannelMention(@NonNull Component component, @NonNull String id) {
        Context context = CONTEXT.get();
        DiscordToMinecraftChatConfig.Mentions.Format format =
                context != null ? context.config.map(cfg -> cfg.mentions).get(cfg -> cfg.channel) : null;
        if (format == null) {
            return component.append(Component.text("<#" + id + ">"));
        }

        GuildChannel guildChannel = discordSRV.jda()
                .map(jda -> jda.getGuildChannelById(id))
                .orElse(null);

        return component.append(ComponentUtil.fromAPI(
                discordSRV.componentFactory()
                        .enhancedBuilder(guildChannel != null ? format.format : format.unknownFormat)
                        .addReplacement("%channel_name%", guildChannel != null ? guildChannel.getName() : null)
                        .applyPlaceholderService()
                        .build()
        ));
    }

    @Override
    public @NotNull Component appendUserMention(@NonNull Component component, @NonNull String id) {
        Context context = CONTEXT.get();
        DiscordToMinecraftChatConfig.Mentions.Format format =
                context != null ? context.config.map(cfg -> cfg.mentions).get(cfg -> cfg.user) : null;
        DiscordGuild guild = context != null
                             ? discordSRV.discordAPI()
                                     .getGuildById(context.event.getGuild().getId())
                                     .orElse(null)
                             : null;
        if (format == null || guild == null) {
            return component.append(Component.text("<@" + id + ">"));
        }

        long userId = MiscUtil.parseLong(id);
        DiscordUser user = discordSRV.discordAPI().getUserById(userId).orElse(null);
        DiscordGuildMember member = guild.getMemberById(userId).orElse(null);

        EnhancedTextBuilder builder = discordSRV.componentFactory()
                .enhancedBuilder(user != null ? format.format : format.unknownFormat);

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
    public @NotNull Component appendRoleMention(@NonNull Component component, @NonNull String id) {
        Context context = CONTEXT.get();
        DiscordToMinecraftChatConfig.Mentions.Format format =
                context != null ? context.config.map(cfg -> cfg.mentions).get(cfg -> cfg.role) : null;
        if (format == null) {
            return component.append(Component.text("<#" + id + ">"));
        }

        long roleId = MiscUtil.parseLong(id);
        DiscordRole role = discordSRV.discordAPI().getRoleById(roleId).orElse(null);

        EnhancedTextBuilder builder = discordSRV.componentFactory()
                .enhancedBuilder(role != null ? format.format : format.unknownFormat);

        if (role != null) {
            builder.addContext(role);
        }

        return component.append(ComponentUtil.fromAPI(
                builder.applyPlaceholderService().build()
        ));
    }

    private static class Context {

        private final DiscordMessageProcessingEvent event;
        private final OrDefault<DiscordToMinecraftChatConfig> config;

        public Context(DiscordMessageProcessingEvent event, OrDefault<DiscordToMinecraftChatConfig> config) {
            this.event = event;
            this.config = config;
        }
    }
}
