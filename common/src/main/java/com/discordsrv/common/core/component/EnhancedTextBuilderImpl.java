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

import com.discordsrv.api.color.Color;
import com.discordsrv.api.component.GameTextBuilder;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.placeholder.PlaceholderServiceImpl;
import com.discordsrv.common.util.ComponentUtil;
import dev.vankka.enhancedlegacytext.EnhancedComponentBuilder;
import dev.vankka.enhancedlegacytext.EnhancedLegacyText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnhancedTextBuilderImpl implements GameTextBuilder {

    private final List<Object> context = PlaceholderServiceImpl.contextList(8);
    private final Map<Pattern, Function<@NotNull Matcher, @Nullable Object>> replacements = new LinkedHashMap<>();
    private boolean placeholderServiceApplied = false;

    private final DiscordSRV discordSRV;
    private final String enhancedFormat;

    public EnhancedTextBuilderImpl(DiscordSRV discordSRV, String enhancedFormat) {
        this.discordSRV = discordSRV;
        this.enhancedFormat = enhancedFormat;
    }

    @Override
    public @NotNull GameTextBuilder addContext(Collection<Object> context) {
        for (Object o : context) {
            if (o == null) {
                continue;
            }
            this.context.add(o);
        }
        return applyPlaceholderService();
    }

    @Override
    public @NotNull GameTextBuilder addReplacement(@NotNull Pattern target, @NotNull Function<@NotNull Matcher, @Nullable Object> replacement) {
        this.replacements.put(target, wrapFunction(replacement));
        return this;
    }

    @Override
    public @NotNull GameTextBuilder applyPlaceholderService() {
        if (placeholderServiceApplied) {
            return this;
        }

        this.replacements.put(
                PlaceholderService.PATTERN,
                wrapFunction(matcher -> discordSRV.placeholderService().getReplacement(matcher, context))
        );
        this.placeholderServiceApplied = true;
        return this;
    }

    private Function<Matcher, Object> wrapFunction(Function<Matcher, Object> function) {
        return matcher -> {
            Object result = function.apply(matcher);
            if (result instanceof Color) {
                // Convert Color to something it'll understand
                return TextColor.color(((Color) result).rgb());
            } else if (result instanceof MinecraftComponent) {
                // Convert to adventure component from API
                return ComponentUtil.fromAPI((MinecraftComponent) result);
            }
            return result;
        };
    }

    @Override
    public @NotNull MinecraftComponent build() {
        EnhancedComponentBuilder builder = EnhancedLegacyText.get()
                .buildComponent(enhancedFormat);

        replacements.forEach(builder::replaceAll);

        Component component = builder.build();
        return ComponentUtil.toAPI(component);
    }
}
