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
import com.discordsrv.api.events.message.process.discord.DiscordChatMessageCustomEmojiRenderEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.generic.MentionsConfig;
import com.discordsrv.common.core.component.renderer.DiscordSRVMinecraftRenderer;
import com.discordsrv.common.core.component.translation.Translation;
import com.discordsrv.common.core.component.translation.TranslationRegistry;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.util.ComponentUtil;
import dev.vankka.enhancedlegacytext.EnhancedLegacyText;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializerOptions;
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer;
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializerOptions;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.ansi.ColorLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class ComponentFactory implements MinecraftComponentFactory {

    private static final String TRANSLATION_KEY_REGEX = ".+\\..+";
    private static final Pattern TRANSLATION_KEY_PATTERN = Pattern.compile(TRANSLATION_KEY_REGEX);
    public static final Class<?> UNRELOCATED_ADVENTURE_COMPONENT;

    static {
        Class<?> clazz = null;
        try {
            clazz = Class.forName("net.kyo".concat("ri.adventure.text.Component"));
        } catch (ClassNotFoundException ignored) {}
        UNRELOCATED_ADVENTURE_COMPONENT = clazz;
    }

    private final DiscordSRV discordSRV;
    private final Logger logger;

    private final MinecraftSerializer minecraftSerializer;
    private final DiscordSerializer discordSerializer;
    private final PlainTextComponentSerializer plainSerializer;
    private final ANSIComponentSerializer ansiSerializer;

    // Not the same as Adventure's TranslationRegistry
    private final TranslationRegistry translationRegistry = new TranslationRegistry();

    public ComponentFactory(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "COMPONENT_FACTORY");

        this.minecraftSerializer = new MinecraftSerializer(
                MinecraftSerializerOptions.defaults()
                        .addRenderer(new DiscordSRVMinecraftRenderer(discordSRV))
        );

        ComponentFlattener flattener = ComponentFlattener.basic().toBuilder()
                .mapper(TranslatableComponent.class, this::provideTranslation)
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

    private final ThreadLocal<Set<String>> translationHistory = new ThreadLocal<>();
    private String provideTranslation(TranslatableComponent component) {
        Set<String> history = translationHistory.get();
        if (history == null) {
            history = new HashSet<>();
        }

        String key = component.key();
        if (history.contains(key)) {
            // Prevent infinite loop here
            logger.debug("Preventing recursive translation: " + key);
            return key;
        }

        Translation translation = translationRegistry.lookup(discordSRV.defaultLocale(), key);
        if (translation == null) {
            // To support datapacks and other mods that don't provide translations but for some reason use the translation component
            // We check if the key is following the pattern of a translation key. Which is "key.subkey" or "key.subkey.subsubkey" etc.
            if (!TRANSLATION_KEY_PATTERN.matcher(key).matches()) {
                return key;
            }

            return null;
        }

        try {
            history.add(key);
            translationHistory.set(history);

            return translation.translate(
                    component.arguments()
                            .stream()
                            .map(argument -> plainSerializer().serialize(argument.asComponent()))
                            .toArray(Object[]::new)
            );
        } finally {
            Set<String> newHistory = translationHistory.get();
            newHistory.remove(key);
            translationHistory.set(newHistory.isEmpty() ? null : newHistory);
        }
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
    public Component makeChannelMention(long id, MentionsConfig.Format format) {
        JDA jda = discordSRV.jda();
        GuildChannel guildChannel = jda != null ? jda.getGuildChannelById(id) : null;

        return DiscordContentComponent.of("<#" + Long.toUnsignedString(id) + ">", ComponentUtil.fromAPI(
                discordSRV.componentFactory()
                        .textBuilder(guildChannel != null ? format.format : format.unknownFormat)
                        .addContext(guildChannel)
                        .applyPlaceholderService()
                        .build()
        ));
    }

    @NotNull
    public Component makeUserMention(
            long id,
            MentionsConfig.FormatUser formatConfig,
            @Nullable DiscordGuild guild,
            @Nullable Set<DiscordUser> users,
            @Nullable Set<DiscordGuildMember> members
    ) {
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

        return DiscordContentComponent.of("<@" + Long.toUnsignedString(id) + ">", ComponentUtil.fromAPI(
                discordSRV.componentFactory()
                        .textBuilder(format)
                        .addContext(user, member)
                        .applyPlaceholderService()
                        .build()
        ));
    }

    public Component makeRoleMention(long id, MentionsConfig.Format format) {
        DiscordRole role = discordSRV.discordAPI().getRoleById(id);

        return DiscordContentComponent.of("<@&" + Long.toUnsignedString(id) + ">", ComponentUtil.fromAPI(
                discordSRV.componentFactory()
                        .textBuilder(role != null ? format.format : format.unknownFormat)
                        .addContext(role)
                        .applyPlaceholderService()
                        .build()
        ));
    }

    public Component makeEmoteMention(long id, MentionsConfig.EmoteBehaviour behaviour) {
        DiscordCustomEmoji emoji = discordSRV.discordAPI().getEmojiById(id);
        if (emoji == null) {
            return null;
        }

        DiscordChatMessageCustomEmojiRenderEvent event = new DiscordChatMessageCustomEmojiRenderEvent(emoji);
        discordSRV.eventBus().publish(event);

        if (event.isProcessed()) {
            return DiscordContentComponent.of(emoji.asJDA().getAsMention(), ComponentUtil.fromAPI(event.getRenderedEmojiFromProcessing()));
        }

        switch (behaviour) {
            case NAME:
                return DiscordContentComponent.of(emoji.asJDA().getAsMention(), Component.text(":" + emoji.getName() + ":"));
            case BLANK:
            default:
                return null;
        }
    }

    public Component minecraftSerialize(ReceivedDiscordMessage message, BaseChannelConfig config, String discordMessage) {
        return DiscordSRVMinecraftRenderer.getWithContext(
                message.getGuild(),
                message.getMentionedUsers(),
                message.getMentionedMembers(),
                config,
                () -> minecraftSerializer().serialize(discordMessage)
        );
    }

    public String discordSerialize(Component component) {
        Component mapped = DiscordContentComponent.remapToDiscord(component);
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

    public TranslationRegistry translationRegistry() {
        return translationRegistry;
    }

}
