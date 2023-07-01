package com.discordsrv.common.config.main;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class DiscordCommandConfig {

    public ExecuteConfig execute = new ExecuteConfig();

    @ConfigSerializable
    public static class ExecuteConfig {


    }
}
