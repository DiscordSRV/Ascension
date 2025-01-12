package com.discordsrv.fabric.module;

import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.fabric.FabricDiscordSRV;

public abstract class AbstractFabricModule extends AbstractModule<FabricDiscordSRV> {
    protected boolean enabled = false;

    public AbstractFabricModule(FabricDiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void enable() {
        enabled = true;
        this.register();
    }

    @Override
    public void disable() {
        enabled = false;
    }

    public void register() {}
}