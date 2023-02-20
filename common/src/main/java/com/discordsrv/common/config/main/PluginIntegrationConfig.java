package com.discordsrv.common.config.main;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class PluginIntegrationConfig {

    @Comment("Plugin integrations that should be disabled. Specify the names or ids of plugins to disable integrations for")
    public List<String> disabledIntegrations = new ArrayList<>();
}
