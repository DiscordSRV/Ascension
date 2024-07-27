/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

public class ComponentFactory implements MinecraftComponentFactory {

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

        return DiscordMentionComponent.of("<#" + Long.toUnsignedString(id) + ">").append(ComponentUtil.fromAPI(
                discordSRV.componentFactory()
                        .textBuilder(guildChannel != null ? format.format : format.unknownFormat)
                        .addContext(guildChannel)
                        .applyPlaceholderService()
                        .build()
        ));
    }

    @NotNull
    public Component makeUserMention(long id, MentionsConfig.FormatUser format, DiscordGuild guild) {
        DiscordUser user = discordSRV.discordAPI().getUserById(id);
        DiscordGuildMember member = guild.getMemberById(id);

        return DiscordMentionComponent.of("<@" + Long.toUnsignedString(id) + ">").append(ComponentUtil.fromAPI(
                discordSRV.componentFactory()
                        .textBuilder(user != null ? (member != null ? format.format : format.formatGlobal) : format.unknownFormat)
                        .addContext(user, member)
                        .applyPlaceholderService()
                        .build()
        ));
    }

    public Component makeRoleMention(long id, MentionsConfig.Format format) {
        DiscordRole role = discordSRV.discordAPI().getRoleById(id);

        return DiscordMentionComponent.of("<@&" + Long.toUnsignedString(id) + ">").append(ComponentUtil.fromAPI(
                discordSRV.componentFactory()
                        .textBuilder(role != null ? format.format : format.unknownFormat)
                        .addContext(role)
                        .applyPlaceholderService()
                        .build()
        ));
    }

    @SuppressWarnings("DataFlowIssue") // isProcessed = processed is not null
    public Component makeEmoteMention(long id, MentionsConfig.EmoteBehaviour behaviour) {
        DiscordCustomEmoji emoji = discordSRV.discordAPI().getEmojiById(id);
        if (emoji == null) {
            return null;
        }

        DiscordChatMessageCustomEmojiRenderEvent event = new DiscordChatMessageCustomEmojiRenderEvent(emoji);
        discordSRV.eventBus().publish(event);

        if (event.isProcessed()) {
            return DiscordMentionComponent.of(emoji.asJDA().getAsMention())
                    .append(ComponentUtil.fromAPI(event.getRenderedEmojiFromProcessing()));
        }

        switch (behaviour) {
            case NAME:
                return DiscordMentionComponent.of(emoji.asJDA().getAsMention()).append(Component.text(":" + emoji.getName() + ":"));
            case BLANK:
            default:
                return null;
        }
    }

    public Component minecraftSerialize(DiscordGuild guild, BaseChannelConfig config, String discordMessage) {
        return DiscordSRVMinecraftRenderer.getWithContext(guild, config, () -> minecraftSerializer().serialize(discordMessage));
    }

    public String discordSerialize(Component component) {
        Component mapped = Component.text().append(component).mapChildrenDeep(comp -> {
            if (comp instanceof DiscordMentionComponent) {
                return Component.text(((DiscordMentionComponent) comp).mention());
            }
            return comp;
        }).children().get(0);
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
