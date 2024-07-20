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

package com.discordsrv.common.config.main.channels;

import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.event.events.message.receive.game.JoinMessageReceiveEvent;
import com.discordsrv.common.config.util.ConfigUtil;
import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.configurate.annotation.Untranslated;
import com.discordsrv.common.config.main.generic.IMessageConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class JoinMessageConfig implements IMessageConfig {

    public JoinMessageConfig() {
        ConfigUtil.nullAllFields(this);
    }

    public Boolean enabled = true;

    @Untranslated(Untranslated.Type.VALUE)
    public SendableDiscordMessage.Builder format = SendableDiscordMessage.builder()
            .addEmbed(
                    DiscordMessageEmbed.builder()
                            .setAuthor("%player_display_name% joined", null, "%player_avatar_url%")
                            .setColor(0x55FF55)
                            .build()
            );

    @Comment("If the \"%1\" permission should determine if join messages are sent")
    @Constants.Comment("discordsrv.silentjoin")
    public Boolean enableSilentPermission = true;

    @Comment("Ignore if the player leaves within the given amount of milliseconds. This will delay sending the join message")
    public Long ignoreIfLeftWithinMS = 250L;

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public SendableDiscordMessage.Builder format() {
        return format;
    }

    @Nullable
    public FirstJoin firstJoin() {
        // Returns null if first join is unavailable
        return null;
    }

    public final IMessageConfig getForEvent(JoinMessageReceiveEvent event) {
        FirstJoin firstJoin = firstJoin();
        return firstJoin != null && event.isFirstJoin() ? firstJoin : this;
    }

    @ConfigSerializable
    public static class FirstJoin implements IMessageConfig {

        public boolean enabled = true;

        @Untranslated(Untranslated.Type.VALUE)
        public SendableDiscordMessage.Builder format = SendableDiscordMessage.builder()
                .addEmbed(
                        DiscordMessageEmbed.builder()
                                .setAuthor("%player_display_name% joined for the first time", null, "%player_avatar_url%")
                                .setColor(0xFFAA00)
                                .build()
                );

        @Override
        public boolean enabled() {
            return enabled;
        }

        @Override
        public SendableDiscordMessage.Builder format() {
            return format;
        }
    }
}
