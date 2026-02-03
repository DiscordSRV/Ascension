/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.core.component;

import com.discordsrv.api.component.GameTextBuilder;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.component.MinecraftComponentAdapter;
import com.discordsrv.api.component.MinecraftComponentFactory;
import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.guild.DiscordCustomEmoji;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.events.message.render.game.CustomEmojiRenderEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.DiscordToMinecraftChatConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.generic.MentionsConfig;
import com.discordsrv.common.core.component.renderer.DiscordSRVMinecraftRenderer;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.feature.mention.MentionUtil;
import com.discordsrv.common.util.ComponentUtil;
import dev.vankka.enhancedlegacytext.EnhancedLegacyText;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializerOptions;
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer;
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializerOptions;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.renderer.TranslatableComponentRenderer;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.Translator;
import net.kyori.ansi.ColorLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ComponentFactory implements MinecraftComponentFactory {

    private static final Pattern MESSAGE_URL_PATTERN = Pattern.compile("https://(?:(?:ptb|canary)\\.)?discord\\.com/channels/[0-9]{16,20}/([0-9]{16,20})/[0-9]{16,20}");
    public static final Class<?> UNRELOCATED_ADVENTURE_COMPONENT;

    static {
        Class<?> clazz = null;
        try {
            clazz = Class.forName("net.kyo".concat("ri.adventure.text.Component"));
        } catch (ClassNotFoundException ignored) {}
        UNRELOCATED_ADVENTURE_COMPONENT = clazz;
    }

    protected final DiscordSRV discordSRV;
    protected final Logger logger;

    private final MinecraftSerializer minecraftSerializer;
    private final DiscordSerializer discordSerializer;
    private final PlainTextComponentSerializer plainSerializer;
    private final ANSIComponentSerializer ansiSerializer;

    private final Translators translators = new Translators();
    private final TranslatableComponentRenderer<Locale> translatableComponentRenderer = TranslatableComponentRenderer.usingTranslationSource(translators);

    public ComponentFactory(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "COMPONENT_FACTORY");

        this.minecraftSerializer = new MinecraftSerializer(
                MinecraftSerializerOptions.defaults()
                        .addRenderer(new DiscordSRVMinecraftRenderer(discordSRV))
        );

        ComponentFlattener flattener = ComponentFlattener.basic().toBuilder()
                .mapper(TranslatableComponent.class, translatableComponent -> {
                    Component translated = translatableComponentRenderer.render(translatableComponent, Locale.US);
                    // Avoid recursion, use plain text serializer without special flattener
                    return PlainTextComponentSerializer.plainText().serialize(translated);
                })
                .build();
        this.discordSerializer = new DiscordSerializer(
                DiscordSerializerOptions.defaults()
                        .withFlattener(flattener)
        );
        this.plainSerializer = PlainTextComponentSerializer.builder()
                .flattener(flattener)
                .build();
        this.ansiSerializer = ANSIComponentSerializer.builder()
                .colorLevel(ColorLevel.INDEXED_8)
                .flattener(flattener)
                .build();
    }

    @Override
    public @NotNull MinecraftComponent fromJson(@NotNull String json) {
        return new MinecraftComponentImpl(json);
    }

    @Override
    public <T> @NotNull MinecraftComponentAdapter<T> makeAdapter(Class<?> gsonSerializerClass, @Nullable Class<T> componentClass) {
        return new MinecraftComponentAdapterImpl<>(gsonSerializerClass, componentClass);
    }

    @Override
    public @NotNull GameTextBuilder textBuilder(@NotNull String enhancedLegacyText) {
        return new EnhancedTextBuilderImpl(discordSRV, enhancedLegacyText);
    }

    public @NotNull Component parse(@NotNull String textInput) {
        if (textInput.contains(String.valueOf(LegacyComponentSerializer.SECTION_CHAR))) {
            return LegacyComponentSerializer.legacySection().deserialize(textInput);
        }

        return EnhancedLegacyText.get().parse(textInput);
    }

    // Mentions

    @NotNull
    public Component makeChannelMention(long id, BaseChannelConfig config, @Nullable DiscordUser requester) {
        MentionsConfig.Format format = config.mentions.channel;

        JDA jda = discordSRV.jda();
        GuildChannel guildChannel = jda != null ? jda.getGuildChannelById(id) : null;
        if (guildChannel != null && !MentionUtil.canMentionChannel(guildChannel, requester)) {
            guildChannel = null;
        }

        return DiscordMentionComponent.of(Message.MentionType.CHANNEL, Long.toUnsignedString(id), ComponentUtil.fromAPI(
                discordSRV.componentFactory()
                        .textBuilder(guildChannel != null ? format.format : format.unknownFormat)
                        .addContext(guildChannel, config)
                        .build()
        ));
    }

    @Nullable
    public Component makeMessageLink(String link, BaseChannelConfig config, @Nullable DiscordUser requester) {
        Matcher matcher = MESSAGE_URL_PATTERN.matcher(link);
        if (!matcher.matches()) {
            return null;
        }

        String channelId = matcher.group(1);

        JDA jda = discordSRV.jda();
        GuildChannel guildChannel = jda != null ? jda.getGuildChannelById(channelId) : null;
        if (config == null || guildChannel == null || !MentionUtil.canMentionChannel(guildChannel, requester)) {
            return null;
        }

        String format = config.mentions.messageUrl;
        return Component.text()
                .clickEvent(ClickEvent.openUrl(link))
                .append(
                        ComponentUtil.fromAPI(
                                discordSRV.componentFactory()
                                        .textBuilder(format)
                                        .addContext(guildChannel)
                                        .addPlaceholder("jump_url", link)
                                        .build()
                        )
                )
                .build();
    }

    @NotNull
    public Component makeUserMention(
            long id,
            BaseChannelConfig config,
            @Nullable DiscordGuild guild,
            @Nullable Set<DiscordUser> users,
            @Nullable Set<DiscordGuildMember> members
    ) {
        MentionsConfig.FormatUser formatConfig = config.mentions.user;

        DiscordGuildMember member = members == null ? null : members
                .stream().filter(m -> m.getUser().getId() == id)
                .findAny().orElse(null);
        if (member == null && guild != null) {
            member = guild.getMemberById(id);
        }

        DiscordUser user = member != null ? member.getUser() : null;
        if (user == null && users != null) {
            user = users.stream()
                    .filter(u -> u.getId() == id)
                    .findAny().orElse(null);
        }
        if (user == null) {
            user = discordSRV.discordAPI().getUserById(id);
        }

        String format;
        if (member != null) {
            format = formatConfig.format;
        } else if (user != null) {
            format = formatConfig.formatGlobal;
        } else {
            format = formatConfig.unknownFormat;
        }

        return DiscordMentionComponent.of(Message.MentionType.USER, Long.toUnsignedString(id), ComponentUtil.fromAPI(
                discordSRV.componentFactory()
                        .textBuilder(format)
                        .addContext(user, member, config)
                        .build()
        ));
    }

    public Component makeRoleMention(long id, BaseChannelConfig config) {
        MentionsConfig.Format format = config.mentions.role;
        DiscordRole role = discordSRV.discordAPI().getRoleById(id);

        return DiscordMentionComponent.of(Message.MentionType.ROLE, Long.toUnsignedString(id), ComponentUtil.fromAPI(
                discordSRV.componentFactory()
                        .textBuilder(role != null ? format.format : format.unknownFormat)
                        .addContext(role, config)
                        .build()
        ));
    }

    public Component makeEveryoneRoleMention(long roleId, BaseChannelConfig config) {
        DiscordRole role = discordSRV.discordAPI().getRoleById(roleId);

        return DiscordMentionComponent.of(Message.MentionType.EVERYONE, "@everyone", ComponentUtil.fromAPI(
                discordSRV.componentFactory()
                        .textBuilder(config.mentions.everyoneRoleFormat)
                        .addContext(role, config)
                        .build()
        ));
    }

    public Component makeHereRoleMention(long roleId, BaseChannelConfig config) {
        DiscordRole role = discordSRV.discordAPI().getRoleById(roleId);

        return DiscordMentionComponent.of(Message.MentionType.EVERYONE, "@here", ComponentUtil.fromAPI(
                discordSRV.componentFactory()
                        .textBuilder(config.mentions.hereRoleFormat)
                        .addContext(role, config)
                        .build()
        ));
    }

    public Component makeEmoteMention(long id, MentionsConfig.EmoteBehaviour behaviour) {
        DiscordCustomEmoji emoji = discordSRV.discordAPI().getEmojiById(id);
        if (emoji == null) {
            return null;
        }

        CustomEmojiRenderEvent event = new CustomEmojiRenderEvent(emoji);
        discordSRV.eventBus().publish(event);

        if (event.isProcessed()) {
            return DiscordMentionComponent.of(Message.MentionType.EMOJI, emoji.asJDA().getAsMention(), ComponentUtil.fromAPI(event.getRenderedEmojiFromProcessing()));
        }

        switch (behaviour) {
            case NAME:
                return DiscordMentionComponent.of(Message.MentionType.EMOJI, emoji.asJDA().getAsMention(), Component.text(":" + emoji.getName() + ":"));
            case BLANK:
            default:
                return null;
        }
    }

    public Component minecraftSerialize(ReceivedDiscordMessage message, BaseChannelConfig config, String discordMessage) {
        DiscordToMinecraftChatConfig.FormattingLimitConfig formattingLimit = config.discordToMinecraft.formattingLimit;
        DiscordUser user = message.getAuthor();
        DiscordGuildMember member = message.getMember();
        boolean allowed = formattingLimit.roleAndUserIds.stream().anyMatch(id -> {
            if (id == user.getId()) {
                return true;
            }
            if (member == null) {
                return false;
            }
            for (DiscordRole role : member.getRoles()) {
                if (role.getId() == id) {
                    return true;
                }
            }
            return false;
        }) != formattingLimit.blacklist;

        return DiscordSRVMinecraftRenderer.getWithContext(
                message.getGuild(),
                message.getAuthor(),
                message.getMentionedUsers(),
                message.getMentionedMembers(),
                config,
                allowed,
                () -> minecraftSerializer().serialize(discordMessage)
        );
    }

    public String discordSerialize(Component component) {
        Component mapped = DiscordMentionComponent.remapToDiscord(component);
        return discordSerializer().serialize(mapped);
    }

    public MinecraftSerializer minecraftSerializer() {
        return minecraftSerializer;
    }

    public DiscordSerializer discordSerializer() {
        return discordSerializer;
    }

    public PlainTextComponentSerializer plainSerializer() {
        return plainSerializer;
    }

    public ANSIComponentSerializer ansiSerializer() {
        return ansiSerializer;
    }

    public List<Translator> translators() {
        return translators.translators;
    }

    private static class Translators implements Translator {

        public final List<Translator> translators = new ArrayList<>();

        @Override
        public @NotNull Key name() {
            return Key.key("discordsrv", "translators");
        }

        @Override
        public boolean canTranslate(@NotNull String key, @NotNull Locale locale) {
            for (Translator translator : translators) {
                if (translator.canTranslate(key, locale)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public @Nullable Component translate(@NotNull TranslatableComponent component, @NotNull Locale locale) {
            for (Translator translator : translators) {
                Component translation = translator.translate(component, locale);
                if (translation != null) {
                    return translation;
                }
            }
            return null;
        }

        @Override
        public @Nullable MessageFormat translate(@NotNull String key, @NotNull Locale locale) {
            for (Translator translator : translators) {
                MessageFormat translation = translator.translate(key, locale);
                if (translation != null) {
                    return translation;
                }
            }
            return null;
        }
    }
}
