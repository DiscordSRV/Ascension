package com.discordsrv.common.config.main;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class MessagesMainConfig {

    @Comment("The language code for the default language, if left blank the system default will be used.\n"
            + "This should be in the ISO 639-1 format or ISO 639-1 (for example \"en\"), a underscore and a ISO 3166-1 country code to specify dialect (for example \"pt_BR\")")
    public String defaultLanguage = "en";

    @Comment("If there should be a messages file per language (based on the player's or user's language), otherwise using the default")
    public boolean multiple = false;

    @Comment("If all languages provided with DiscordSRV should be loaded into the messages directory, only functions when \"multiple\" is set to true")
    public boolean loadAllDefaults = true;
}
