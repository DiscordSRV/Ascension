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

    public String testOption = "test option";

}
