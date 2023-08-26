package com.discordsrv.common.config.messages;

import com.discordsrv.common.config.Config;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class MessagesConfig implements Config {

    public static final String FILE_NAME = "messages.yaml";

    @Override
    public final String getFileName() {
        return FILE_NAME;
    }

    public String noDiscordConnection = "&cDiscord connection not available yet, please try again later";
    public String unableToCheckLinkingStatus = "&cUnable to check linking status, please try again later";
}
