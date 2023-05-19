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
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializer;
import dev.vankka.mcdiscordreserializer.discord.DiscordSerializerOptions;
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializer;
import dev.vankka.mcdiscordreserializer.minecraft.MinecraftSerializerOptions;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

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

    // Not the same as Adventure's TranslationRegistry
    private final TranslationRegistry translationRegistry = new TranslationRegistry();

    public ComponentFactory(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.minecraftSerializer = new MinecraftSerializer(
                MinecraftSerializerOptions.defaults()
                        .addRenderer(new DiscordSRVMinecraftRenderer(discordSRV))
        );
        this.discordSerializer = new DiscordSerializer();
        discordSerializer.setDefaultOptions(
                DiscordSerializerOptions.defaults()
                        .withTranslationProvider(this::provideTranslation)
        );
        this.plainSerializer = PlainTextComponentSerializer.builder()
                .flattener(
                        ComponentFlattener.basic().toBuilder()
                                .mapper(TranslatableComponent.class, this::provideTranslation)
                                .build()
                )
                .build();
    }

    private String provideTranslation(TranslatableComponent component) {
        Translation translation = translationRegistry.lookup(Locale.US, component.key());
        if (translation == null) {
            return null;
        }

        return translation.translate(
                component.args()
                        .stream()
                        .map(discordSerializer::serialize)
                        .map(str -> (Object) str)
                        .toArray(Object[]::new)
        );
    }

    @Override
    public @NotNull MinecraftComponent empty() {
        return MinecraftComponentImpl.empty();
    }

    @Override
    public GameTextBuilder textBuilder(String content) {
        return new EnhancedTextBuilderImpl(discordSRV, content);
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

    public TranslationRegistry translationRegistry() {
        return translationRegistry;
    }
}
