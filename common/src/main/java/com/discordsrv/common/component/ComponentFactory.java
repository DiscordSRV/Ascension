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

package com.discordsrv.common.component;

import com.discordsrv.api.component.EnhancedTextBuilder;
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
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class ComponentFactory implements MinecraftComponentFactory {

    private final DiscordSRV discordSRV;
    private final MinecraftSerializer minecraftSerializer;
    private final DiscordSerializer discordSerializer;

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
                        .withTranslationProvider(translatableComponent -> {
                            Translation translation = translationRegistry.lookup(Locale.US, translatableComponent.key());
                            if (translation == null) {
                                return null;
                            }

                            return translation.translate(
                                    translatableComponent.args()
                                            .stream()
                                            .map(discordSerializer::serialize)
                                            .map(str -> (Object) str)
                                            .toArray(Object[]::new)
                            );
                        })
        );
    }

    @Override
    public @NotNull MinecraftComponent empty() {
        return MinecraftComponentImpl.empty();
    }

    @Override
    public EnhancedTextBuilder enhancedBuilder(String content) {
        return new EnhancedTextBuilderImpl(discordSRV, content);
    }

    public MinecraftSerializer minecraftSerializer() {
        return minecraftSerializer;
    }

    public DiscordSerializer discordSerializer() {
        return discordSerializer;
    }

    public TranslationRegistry translationRegistry() {
        return translationRegistry;
    }
}
