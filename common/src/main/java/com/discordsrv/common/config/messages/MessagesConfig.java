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
        private MinecraftMessage make(String rawFormat) {
            return new MinecraftMessage(rawFormat);
        }

        @Comment("Generic")
        @Constants("&c")
        public MinecraftMessage noPermission = make("%1Sorry, but you do not have permission to use that command");

        @Untranslated(Untranslated.Type.COMMENT)
        @Comment("/discord link")
        @Constants("&c")
        public MinecraftMessage unableToCheckLinkingStatus = make("%1Unable to check linking status, please try again later");
        @Constants("&c")
        public MinecraftMessage alreadyLinked = make("%1You are already linked");
        @Constants("&c")
        public MinecraftMessage pleaseWaitBeforeRunningThatCommandAgain = make("%1Please wait before running that command again");
        @Constants("&c")
        public MinecraftMessage unableToLinkAtThisTime = make("%1Unable to check linking status, please try again later");
        @Constants("&b")
        public MinecraftMessage checkingLinkStatus = make("%1Checking linking status...");
        @Constants("&b")
        public MinecraftMessage youAreNowLinked = make("%1You are now linked!");
        @Constants({
                "&b",
                "&7[click:open_url:%minecraftauth_link%][hover:show_text:Click to open]%minecraftauth_link_simple%[click]&b",
                "&7MinecraftAuth"
        })
        public MinecraftMessage minecraftAuthLinking = make("%1Please visit %2 to link your account through %3");

    }

    public Discord discord = new Discord();

    @ConfigSerializable
    public static class Discord {

    }

    public Both both = new Both();

    public static class Both {

        @Comment("Generic")
        public String invalidTarget = "Invalid target";
        public String placeSpecifyTarget = "Please specify the target";
    }
}
