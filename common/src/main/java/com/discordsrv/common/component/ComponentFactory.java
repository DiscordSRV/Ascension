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

package com.discordsrv.common.component;

import com.discordsrv.api.component.GameTextBuilder;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.component.MinecraftComponentFactory;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.renderer.DiscordSRVMinecraftRenderer;
import com.discordsrv.common.component.translation.Translation;
import com.discordsrv.common.component.translation.TranslationRegistry;
import dev.vankka.enhancedlegacytext.EnhancedLegacyText;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializerOptions;
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer;
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializerOptions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.ansi.ColorLevel;
import org.jetbrains.annotations.NotNull;

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
    private final MinecraftSerializer minecraftSerializer;
    private final DiscordSerializer discordSerializer;
    private final PlainTextComponentSerializer plainSerializer;
    private final ANSIComponentSerializer ansiSerializer;

    // Not the same as Adventure's TranslationRegistry
    private final TranslationRegistry translationRegistry = new TranslationRegistry();

    public ComponentFactory(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
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

    private String provideTranslation(TranslatableComponent component) {
        Translation translation = translationRegistry.lookup(discordSRV.defaultLocale(), component.key());
        if (translation == null) {
            return null;
        }

        return translation.translate(
                component.arguments()
                        .stream()
                        // Prevent infinite loop here by using the default PlainTextSerializer
                        .map(argument -> PlainTextComponentSerializer.plainText().serialize(argument.asComponent()))
                        .toArray(Object[]::new)
        );
    }

    @Override
    public @NotNull MinecraftComponent empty() {
        return MinecraftComponentImpl.empty();
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
