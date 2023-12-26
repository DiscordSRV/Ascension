package com.discordsrv.common.config.messages;

import com.discordsrv.common.config.Config;
import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.configurate.annotation.Untranslated;
import com.discordsrv.common.config.helper.MinecraftMessage;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class MessagesConfig implements Config {

    public static final String FILE_NAME = "messages.yaml";

    @Override
    public final String getFileName() {
        return FILE_NAME;
    }

    public Minecraft minecraft = new Minecraft();

    @ConfigSerializable
    public static class Minecraft {
        private static final String ERROR_COLOR = "&c";
        private static final String SUCCESS_COLOR = "&a";
        private static final String NEUTRAL_COLOR = "&b";

        private MinecraftMessage make(String rawFormat) {
            return new MinecraftMessage(rawFormat);
        }

        @Comment("Generic")
        @Constants(ERROR_COLOR)
        public MinecraftMessage noPermission = make("%1Sorry, but you do not have permission to use that command");
        @Constants(ERROR_COLOR)
        public MinecraftMessage pleaseSpecifyPlayer = make("%1Please specify the Minecraft player");
        @Constants(ERROR_COLOR)
        public MinecraftMessage pleaseSpecifyUser = make("%1Please specify the Discord user");
        @Constants(ERROR_COLOR)
        public MinecraftMessage pleaseSpecifyPlayerOrUser = make("%1Please specify the Minecraft player or Discord user");
        @Constants(ERROR_COLOR)
        public MinecraftMessage playerNotFound = make("%1Minecraft player not found");
        @Constants(ERROR_COLOR)
        public MinecraftMessage userNotFound = make("%1Discord user not found");
        @Constants(ERROR_COLOR)
        public MinecraftMessage unableToCheckLinkingStatus = make("%1Unable to check linking status, please try again later");

        @Untranslated(Untranslated.Type.COMMENT)
        @Comment("/discord link")
        @Constants(ERROR_COLOR)
        public MinecraftMessage alreadyLinked1st = make("%1You are already linked");
        @Constants(ERROR_COLOR)
        public MinecraftMessage pleaseSpecifyPlayerAndUserToLink = make("%1Please specify the Minecraft player and the Discord user to link");
        @Constants(ERROR_COLOR)
        public MinecraftMessage playerAlreadyLinked3rd = make("%1That player is already linked");
        @Constants(ERROR_COLOR)
        public MinecraftMessage userAlreadyLinked3rd = make("%1That player is already linked");
        @Constants(ERROR_COLOR)
        public MinecraftMessage pleaseWaitBeforeRunningThatCommandAgain = make("%1Please wait before running that command again");
        @Constants(ERROR_COLOR)
        public MinecraftMessage unableToLinkAtThisTime = make("%1Unable to check linking status, please try again later");
        @Constants(NEUTRAL_COLOR)
        public MinecraftMessage checkingLinkStatus = make("%1Checking linking status...");
        @Constants(SUCCESS_COLOR)
        public MinecraftMessage nowLinked1st = make("%1You are now linked!");
        @Constants({
                SUCCESS_COLOR,
                NEUTRAL_COLOR,
                SUCCESS_COLOR + "[hover:show_text:%player_uuid%][click:copy_to_clipboard:%player_uuid%]%player_name|text:'<Unknown>'%[click][hover]" + NEUTRAL_COLOR,
                SUCCESS_COLOR + "[hover:show_text:%user_id%][click:copy_to_clipboard:%user_id%]@%user_name%[click][hover]" + NEUTRAL_COLOR
        })
        public MinecraftMessage nowLinked3rd = make("%1Link created successfully %2(%3 and %4)");
        @Constants({
                NEUTRAL_COLOR,
                "&f[click:open_url:%minecraftauth_link%][hover:show_text:Click to open]%minecraftauth_link_simple%[click]" + NEUTRAL_COLOR,
                "&fMinecraftAuth"
        })
        public MinecraftMessage minecraftAuthLinking = make("%1Please visit %2 to link your account through %4");

        @Untranslated(Untranslated.Type.COMMENT)
        @Comment("/discord linked")
        @Constants({
                SUCCESS_COLOR + "[hover:show_text:%user_id%][click:copy_to_clipboard:%user_id%]@%user_name%[click][hover]",
                NEUTRAL_COLOR,
                SUCCESS_COLOR + "[hover:show_text:%player_uuid%][click:copy_to_clipboard:%player_uuid%]%player_name|text:'<Unknown>'%[click][hover]"
        })
        public MinecraftMessage discordUserLinkedTo = make("%1 %2is linked to %3");

        @Untranslated(Untranslated.Type.COMMENT)
        @Comment("/discord linked")
        @Constants({
                SUCCESS_COLOR + "[hover:show_text:%user_id%][click:copy_to_clipboard:%user_id%]@%user_name%[click][hover]",
                NEUTRAL_COLOR,
                ERROR_COLOR
        })
        public MinecraftMessage discordUserUnlinked = make("%1 %2is %3unlinked");

        @Constants({
                SUCCESS_COLOR + "[hover:show_text:%player_uuid%][click:copy_to_clipboard:%player_uuid%]%player_name|text:'<Unknown>'%[click][hover]",
                NEUTRAL_COLOR,
                SUCCESS_COLOR + "[hover:show_text:%user_id%][click:copy_to_clipboard:%user_id%]@%user_name%[click][hover]"
        })
        public MinecraftMessage minecraftPlayerLinkedTo = make("%1 %2is linked to %3");

        @Constants({
                SUCCESS_COLOR + "[hover:show_text:%player_uuid%][click:copy_to_clipboard:%player_uuid%]%player_name|text:'<Unknown>'%[click][hover]",
                NEUTRAL_COLOR,
                ERROR_COLOR
        })
        public MinecraftMessage minecraftPlayerUnlinked = make("%1 %2is %3unlinked");

    }

    public Discord discord = new Discord();

    @ConfigSerializable
    public static class Discord {

        private static final String SUCCESS_PREFIX = "✅ ";
        private static final String INPUT_ERROR_PREFIX = "\uD83D\uDDD2️ ";
        private static final String ERROR_PREFIX = "❌ ";

        @Comment("Generic")
        @Constants(INPUT_ERROR_PREFIX)
        public String pleaseSpecifyPlayer = "%1Please specify the Minecraft player";
        @Constants(INPUT_ERROR_PREFIX)
        public String pleaseSpecifyUser = "%1Please specify the Discord user";
        @Constants(INPUT_ERROR_PREFIX)
        public String pleaseSpecifyPlayerOrUser = "%1Please specify the Minecraft player or Discord user";
        @Constants(ERROR_PREFIX)
        public String playerNotFound = "%1Minecraft player not found";
        @Constants(ERROR_PREFIX)
        public String userNotFound = "%1Discord user not found";
        @Constants(ERROR_PREFIX)
        public String unableToCheckLinkingStatus = "%1Unable to check linking status, please try again later";

        @Untranslated(Untranslated.Type.COMMENT)
        @Comment("/discord link")
        @Constants(ERROR_PREFIX)
        public String playerAlreadyLinked3rd = "%1That Minecraft player is already linked";
        @Constants(ERROR_PREFIX)
        public String userAlreadyLinked3rd = "%1That Discord user is already linked";
        @Constants({
                SUCCESS_PREFIX,
                "**%player_name%** (%player_uuid%)",
                "**%user_name%** (%user_id%)"
        })
        public String nowLinked3rd = "%1Link created successfully\n%2 and %3";

        @Untranslated(Untranslated.Type.COMMENT)
        @Comment("/discord linked")
        @Constants({
                SUCCESS_PREFIX,
                "**%user_name%** (<@%user_id%>)",
                "**%player_name%** (%player_uuid%)"
        })
        public String discordUserLinkedTo = "%1%2 is linked to %3";

        @Untranslated(Untranslated.Type.COMMENT)
        @Comment("/discord linked")
        @Constants({
                ERROR_PREFIX,
                "**%user_name%** (%user_id%)"
        })
        public String discordUserUnlinked = "%1%2 is __unlinked__";

        @Constants({
                SUCCESS_PREFIX,
                "**%player_name%** (%player_uuid%)",
                "**%user_name%** (<@%user_id%>)"
        })
        public String minecraftPlayerLinkedTo = "%1%2 is linked to %3";

        @Constants({
                ERROR_PREFIX,
                "**%player_name%** (%player_uuid%)"
        })
        public String minecraftPlayerUnlinked = "%1%2 is __unlinked__";
    }
}
