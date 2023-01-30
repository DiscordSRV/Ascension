package com.discordsrv.common.config.connection;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class MinecraftAuthConfig {

    @Comment("If minecraftauth.me connections are allowed for Discord linking (requires linked-accounts.provider to be \"auto\" or \"minecraftauth\").\n"
            + "Requires a connection to: minecraftauth.me\n"
            + "Privacy Policy: https://minecraftauth.me/privacy.txt")
    public boolean allow = true;

}
