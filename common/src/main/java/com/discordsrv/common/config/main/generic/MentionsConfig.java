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
    public Format user = new Format(
            "[hover:show_text:Username: @%user_tag%\nRoles: %user_roles:', '|text:'[color:gray][italics:on]None[color][italics]'%][color:#5865F2]@%user_effective_server_name|user_effective_name%",
            "[color:#5865F2]@Unknown user"
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
}
