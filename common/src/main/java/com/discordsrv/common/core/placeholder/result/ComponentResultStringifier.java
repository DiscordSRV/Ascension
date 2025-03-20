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

package com.discordsrv.common.core.placeholder.result;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.placeholder.format.FormattedText;
import com.discordsrv.api.placeholder.format.PlainPlaceholderFormat;
import com.discordsrv.api.placeholder.mapper.PlaceholderResultMapper;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

public class ComponentResultStringifier implements PlaceholderResultMapper {

    private final DiscordSRV discordSRV;

    public ComponentResultStringifier(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public CharSequence convertResult(@NotNull Object result) {
        if (result instanceof MinecraftComponent) {
            result = ComponentUtil.fromAPI((MinecraftComponent) result);
        }
        if (result instanceof Component) {
            Component component = (Component) result;
            PlainPlaceholderFormat.Formatting mappingState = PlainPlaceholderFormat.FORMATTING.get();
            switch (mappingState) {
                case DISCORD_MARKDOWN:
                    return FormattedText.of(discordSRV.componentFactory().discordSerialize(component));
                case ANSI:
                    return discordSRV.componentFactory().ansiSerializer().serialize(component);
                case LEGACY:
                    return LegacyComponentSerializer.legacySection().serialize(component);
                case PLAIN:
                default:
                    return discordSRV.componentFactory().plainSerializer().serialize(component);
            }
        }
        return null;
    }
}
