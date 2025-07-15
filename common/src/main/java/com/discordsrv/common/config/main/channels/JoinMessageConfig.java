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

package com.discordsrv.common.config.main.channels;

import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.message.preprocess.game.JoinMessagePreProcessEvent;
import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.configurate.annotation.Order;
import com.discordsrv.common.config.configurate.annotation.Untranslated;
import com.discordsrv.common.config.configurate.manager.abstraction.ConfigurateConfigManager;
import com.discordsrv.common.config.documentation.DocumentationURLs;
import com.discordsrv.common.config.main.generic.IMessageConfig;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class JoinMessageConfig implements IMessageConfig {

    public JoinMessageConfig() {
        ConfigurateConfigManager.nullAllFields(this);
    }

    @Order(-3)
    public Boolean enabled = true;

    @Comment("Suggested placeholders:\n"
            + "%message% - The join message (this may not always be available)\n"
            + "%player_prefix% - The player's prefix (LuckPerms meta \"discordsrv_prefix\", otherwise their in-game prefix)\n"
            + "%player_meta_prefix% - The player's prefix from the LuckPerms meta \"discordsrv_prefix\" only\n"
            + "%player_suffix% - The player's suffix (LuckPerms meta \"discordsrv_suffix\", otherwise their in-game suffix)\n"
            + "%player_meta_suffix% - The player's suffix from the LuckPerms meta \"discordsrv_suffix\" only\n"
            + "%player_display_name% - The player's display name\n"
            + "%player_name% - The player's username\n"
            + "%player_avatar_url% - The player's avatar url based on the \"avatar-provider\" configuration\n"
            + "More placeholders at %1 (Player)")
    @Constants.Comment(DocumentationURLs.PLACEHOLDERS)
    @Untranslated(Untranslated.Type.VALUE)
    @Order(-2) // Above first-join
    public SendableDiscordMessage.Builder format = SendableDiscordMessage.builder()
            .addEmbed(
                    DiscordMessageEmbed.builder()
                            .setAuthor("%player_display_name% joined", null, "%player_avatar_url%")
                            .setColor(0x55FF55)
                            .build()
            );

    @Comment("If the \"%1\" permission should determine whether join messages are sent")
    @Constants.Comment("discordsrv.silentjoin")
    public boolean enableSilentPermission = true;

    @Comment("If fake join messages should be sent when players unvanish")
    public boolean sendFakeJoinMessages = true;

    @Comment("Send message if player is vanished")
    public boolean sendMessageForVanishedPlayers = false;

    @Comment("Ignore if the player leaves or vanishes within the given number of milliseconds. This will delay sending the join message")
    @Setting("ignore-if-left-within-ms")
    public Long ignoreIfLeftWithinMS = 250L;

    @Comment("If messages should be sent even if they are cancelled\n"
            + "This option may be removed in the future, fixing other plugins to not cancel messages is recommended")
    public boolean sendEvenIfCancelled = false;

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

    public final IMessageConfig getForEvent(JoinMessagePreProcessEvent event) {
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
