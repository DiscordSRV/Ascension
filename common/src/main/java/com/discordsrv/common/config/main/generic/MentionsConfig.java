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

package com.discordsrv.common.config.main.generic;

import com.discordsrv.common.config.configurate.annotation.Untranslated;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class MentionsConfig {

    public Format role = new Format(
            "%role_color%@%role_name%",
            "[color:#5865F2]@deleted-role"
    );
    public Format channel = new Format(
            "[hover:show_text:Click to go to channel][click:open_url:%channel_jump_url%][color:#5865F2]#%channel_name%",
            "[color:#5865F2]#Unknown"
    );
    public FormatUser user = new FormatUser(
            "[hover:show_text:Username: @%user_tag% [italics:on][color:gray](Shift+Click to mention)[color][italics:off]\nRoles: %user_selected_roles:', '|text:'[color:gray][italics:on]None[color][italics]'%]"
                    + "[insert:@%user_tag%][color:#5865F2]"
                    + "@%user_effective_name%",
            "[color:#5865F2]@Unknown user",
            "[hover:show_text:Username: @%user_tag% [italics:on][color:gray](Shift+Click to mention)[color][italics:off]]"
                    + "[insert:@%user_tag%][color:#5865F2]"
                    + "@%user_effective_name%"
    );

    public String messageUrl = "[hover:show_text:Click to go to message][click:open_url:%jump_url%][color:#5865F2]#%channel_name% > ...";

    @Comment("How should custom emoji be shown in-game:\n"
            + "- hide: custom emoji will not be shown in-game\n"
            + "- blank: custom emoji will only be shown in-game if it is rendered by a 3rd party plugin\n"
            + "- name: shows the name of the custom emoji in-game (for example :discordsrv:), unless rendered by a 3rd party plugin")
    public EmoteBehaviour customEmojiBehaviour = EmoteBehaviour.BLANK;

    public enum EmoteBehaviour {
        HIDE,
        BLANK,
        NAME
    }

    @ConfigSerializable
    public static class Format {

        @Comment("The format shown in-game")
        @Untranslated(Untranslated.Type.VALUE)
        public String format = "";

        @Comment("The format when the entity is deleted or can't be looked up")
        @Untranslated(Untranslated.Type.VALUE)
        public String unknownFormat = "";

        @SuppressWarnings("unused") // Configurate
        public Format() {}

        public Format(String format, String unknownFormat) {
            this.format = format;
            this.unknownFormat = unknownFormat;
        }
    }

    @ConfigSerializable
    public static class FormatUser extends Format {

        @Comment("The format shown in-game for users that cannot be linked to a specific Discord server")
        public String formatGlobal = "";

        @SuppressWarnings("unused") // Configurate
        public FormatUser() {}

        public FormatUser(String format, String unknownFormat, String globalFormat) {
            super(format, unknownFormat);
            this.formatGlobal = globalFormat;
        }
    }
}
