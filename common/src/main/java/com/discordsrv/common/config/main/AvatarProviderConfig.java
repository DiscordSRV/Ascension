package com.discordsrv.common.config.main;

import com.discordsrv.common.config.annotation.Order;
import com.discordsrv.common.config.annotation.Untranslated;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class AvatarProviderConfig {

    @Untranslated(Untranslated.Type.VALUE)
    @Order(0)
    @Comment("The template for URLs of player avatars\n" +
            "This will be used for offical Java players only if auto-decide-avatar-url is set to true")
    public String avatarUrlTemplate = "https://crafatar.com/avatars/%uuid-nodashes%.png?size=%size%&overlay#%texture%";

    @Order(1)
    @Comment("Whether to let DiscordSRV decide an appropriate avatar URL\n" +
            "This will result in appropriate head renders being provided for Bedrock and Offline Mode players.")
    public boolean autoDecideAvatarUrl = false;

    @Order(2)
    @Comment("Value for the %size% placeholder")
    public int size = 128;
}
