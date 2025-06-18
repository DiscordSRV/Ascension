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

import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.configurate.manager.abstraction.ConfigurateConfigManager;
import com.discordsrv.common.config.documentation.DocumentationURLs;
import com.discordsrv.common.config.main.generic.DiscordIgnoresConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class MirroringConfig {

    public MirroringConfig() {
        ConfigurateConfigManager.nullAllFields(this);
    }

    @Comment("Requires the \"Message Content Intent\"")
    public Boolean enabled = true;

    @Comment("Users to not mirror")
    public DiscordIgnoresConfig ignores = new DiscordIgnoresConfig();

    @Comment("The format of the username of mirrored messages\n"
            + "It's recommended to include some special character if in-game messages use webhooks,\n"
            + "in order to prevent Discord users and in-game players with the same name being grouped together\n"
            + "\n"
            + "Suggested placeholders:\n"
            + "%user_effective_name% - The name of the Discord user globally\n"
            + "\n"
            + "More placeholders at %1 (Message, User, User (Server Member))\n"
            + "User (Server Member) placeholders are not available for webhook messages")
    @Constants.Comment(DocumentationURLs.PLACEHOLDERS)
    public String usernameFormat = "%user_effective_name% \uD83D\uDD03";

    @Comment("The format when a message is a reply.\n"
            + "Suggested placeholders:\n"
            + "%message% - the formatted message content\n"
            + "%message_jump_url% - the message link\n"
            + "\n"
            + "More placeholders at %1 (Message, User, User (Server Member))\n"
            + "User (Server Member) placeholders are not available for webhook messages")
    @Constants.Comment(DocumentationURLs.PLACEHOLDERS)
    public String replyFormat = "[In reply to %user_effective_name%](%message_jump_url%)\n%message%";

    @Comment("Attachment related options")
    public AttachmentConfig attachments = new AttachmentConfig();

    @ConfigSerializable
    public static class AttachmentConfig {

        @Comment("Maximum size (in kB) to download and re-upload, set to 0 for unlimited or -1 to disable re-uploading.\n"
                + "The default value is -1 (disabled)\n\n"
                + "When this is enabled, files smaller than the specified limit are downloaded and then re-uploaded to each mirror channel individually.\n"
                + "Please consider limiting the users allowed to attach files if this is enabled,\n"
                + "as spam of large files may result in a lot of downstream and upstream data usage")
        public int maximumSizeKb = -1;

        @Comment("If attachments should be placed into an embed in mirrored messages instead of re-uploading")
        public boolean embedAttachments = true;
    }
}
