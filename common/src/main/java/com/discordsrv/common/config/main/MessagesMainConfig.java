package com.discordsrv.common.config.main;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class MessagesMainConfig {

    @Comment("The 3 letter ISO 639-2 code for the default language, if left blank the system default will be used")
    public String defaultLanguage = "eng";

    @Comment("If there should be multiple messages files, one for every language")
    public boolean multiple = false;

    @Comment("If all languages provided with DiscordSRV should be loaded into the messages directory, only functions when \"multiple\" is set to true")
    public boolean loadAllDefaults = true;
}
