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

package com.discordsrv.common.core.component.renderer;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.generic.MentionsConfig;
import com.discordsrv.common.util.ComponentUtil;
import dev.vankka.mcdiscordreserializer.renderer.implementation.DefaultMinecraftRenderer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordSRVMinecraftRenderer extends DefaultMinecraftRenderer {

    private static final Pattern MESSAGE_URL_PATTERN = Pattern.compile("https://(?:(?:ptb|canary)\\.)?discord\\.com/channels/[0-9]{16,20}/([0-9]{16,20})/[0-9]{16,20}");
    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();
    private final DiscordSRV discordSRV;

    public DiscordSRVMinecraftRenderer(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    public static <T> T getWithContext(
            @Nullable DiscordGuild guild,
            @Nullable Set<DiscordUser> users,
            @Nullable Set<DiscordGuildMember> members,
            BaseChannelConfig config,
            Supplier<T> supplier
    ) {
        Context oldValue = CONTEXT.get();
        CONTEXT.set(new Context(guild, users, members, config));
        T output = supplier.get();
        CONTEXT.set(oldValue);
        return output;
    }

    @Override
    public Component appendLink(@NotNull Component part, String link) {
        Component messageLink = makeMessageLink(link);
        if (messageLink == null) {
            return super.appendLink(part, link);
        }

        return part.append(messageLink);
    }

    public Component makeMessageLink(String link) {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            return null;
        }

        Matcher matcher = MESSAGE_URL_PATTERN.matcher(link);
        if (!matcher.matches()) {
            return null;
        }

        String channel = matcher.group(1);
        GuildChannel guildChannel = jda.getGuildChannelById(channel);

        Context context = CONTEXT.get();
        String format = context != null ? context.config.mentions.messageUrl : null;
        if (format == null || guildChannel == null) {
            return null;
        }

        return Component.text()
                .clickEvent(ClickEvent.openUrl(link))
                .append(
                        ComponentUtil.fromAPI(
                                discordSRV.componentFactory()
                                        .textBuilder(format)
                                        .addContext(guildChannel)
                                        .addPlaceholder("jump_url", link)
                                        .applyPlaceholderService()
                                        .build()
                        )
                )
                .build();
    }

    @Override
    public @NotNull Component appendChannelMention(@NotNull Component component, @NotNull String id) {
        Context context = CONTEXT.get();
        MentionsConfig.Format format = context != null ? context.config.mentions.channel : null;
        if (format == null) {
            return component.append(Component.text("<#" + id + ">"));
        }

        return component.append(discordSRV.componentFactory().makeChannelMention(MiscUtil.parseLong(id), format));
    }

    @Override
    public @NotNull Component appendUserMention(@NotNull Component component, @NotNull String id) {
        Context context = CONTEXT.get();
        MentionsConfig.FormatUser format = context != null ? context.config.mentions.user : null;
        if (context == null || format == null) {
            return component.append(Component.text("<@" + id + ">"));
        }

        long userId = MiscUtil.parseLong(id);
        return component.append(discordSRV.componentFactory().makeUserMention(userId, format, context.guild, context.users, context.members));
    }

    @Override
    public @NotNull Component appendRoleMention(@NotNull Component component, @NotNull String id) {
        Context context = CONTEXT.get();
        MentionsConfig.Format format = context != null ? context.config.mentions.role : null;
        if (format == null) {
            return component.append(Component.text("<#" + id + ">"));
        }

        long roleId = MiscUtil.parseLong(id);
        return component.append(discordSRV.componentFactory().makeRoleMention(roleId, format));
    }

    @Override
    public @NotNull Component appendEmoteMention(
            @NotNull Component component,
            @NotNull String name,
            @NotNull String id
    ) {
        Context context = CONTEXT.get();
        MentionsConfig.EmoteBehaviour behaviour = context != null ? context.config.mentions.customEmojiBehaviour : null;
        if (behaviour == null || behaviour == MentionsConfig.EmoteBehaviour.HIDE) {
            return component;
        }

        long emojiId = MiscUtil.parseLong(id);
        Component emoteMention = discordSRV.componentFactory().makeEmoteMention(emojiId, behaviour);
        if (emoteMention == null) {
            return component;
        }

        return component.append(emoteMention);
    }

    private static class Context {

        private final DiscordGuild guild;
        private final Set<DiscordUser> users;
        private final Set<DiscordGuildMember> members;
        private final BaseChannelConfig config;

        public Context(
                DiscordGuild guild,
                Set<DiscordUser> users,
                Set<DiscordGuildMember> members,
                BaseChannelConfig config
        ) {
            this.guild = guild;
            this.users = users;
            this.members = members;
            this.config = config;
        }
    }
}
