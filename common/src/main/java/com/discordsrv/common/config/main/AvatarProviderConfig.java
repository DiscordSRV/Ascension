package com.discordsrv.common.config.main;

import com.discordsrv.common.config.configurate.annotation.Untranslated;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class AvatarProviderConfig {

    @Comment("Whether to let DiscordSRV decide an appropriate avatar URL automatically\n" +
            "This will result in appropriate head renders being provided for Bedrock players (when using Floodgate) and Offline Mode players (via username).")
    public boolean autoDecideAvatarUrl = true;

    @Untranslated(Untranslated.Type.VALUE)
    @Comment("The template for URLs of player avatars\n" +
            "This will be used for offical Java players only if auto-decide-avatar-url is set to true\n" +
            "This will be used ALWAYS if auto-decide-avatar-url is set to false")
    public String avatarUrlTemplate = "https://crafatar.com/avatars/%player_uuid_nodashes%.png?size=128&overlay#%player_texture%";
}
