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

package com.discordsrv.common.config.main.channels;

import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.configurate.annotation.Untranslated;
import com.discordsrv.common.config.configurate.manager.abstraction.ConfigurateConfigManager;
import com.discordsrv.common.config.documentation.DocumentationURLs;
import com.discordsrv.common.config.helper.SendableDiscordMessageTemplate;
import com.discordsrv.common.config.main.generic.IMessageConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class LeaveMessageConfig implements IMessageConfig {

    public LeaveMessageConfig() {
        ConfigurateConfigManager.nullAllFields(this);
    }

    public Boolean enabled = true;

    @Comment("Suggested placeholders:\n"
            + "%message% - The leave message (this may not always be available)\n"
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
    public SendableDiscordMessageTemplate format = new SendableDiscordMessageTemplate(
            SendableDiscordMessage.builder()
                    .addEmbed(
                            DiscordMessageEmbed.builder()
                                    .setAuthor("%player_display_name% left", null, "%player_avatar_url%")
                                    .setColor(0xFF5555)
                                    .build()
                    )
    );

    @Comment("If the \"%1\" permission should determine whether leave messages are sent")
    @Constants.Comment("discordsrv.silentquit")
    public Boolean enableSilentPermission = true;

    @Comment("If fake leave messages should be sent when players vanish")
    public boolean sendFakeMessages = true;

    @Comment("Send message if player is vanished")
    public boolean sendMessageForVanishedPlayers = false;

    @Comment("Ignore if the player joined within the given number of milliseconds")
    @Setting("ignore-if-joined-within-ms")
    public Long ignoreIfJoinedWithinMS = 250L;

    @Comment("If messages should be sent even if they are cancelled.\n"
            + "This option may be removed in the future, fixing other plugins to not cancel messages is recommended")
    public boolean sendEvenIfCancelled = false;

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public SendableDiscordMessageTemplate format() {
        return format;
    }
}
