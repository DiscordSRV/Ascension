package com.discordsrv.fabric.integration;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.integration.LuckPermsIntegration;
import net.luckperms.api.context.ContextConsumer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jspecify.annotations.NonNull;

public class FabricLuckPermsIntegration extends LuckPermsIntegration<ServerPlayerEntity> {

    public FabricLuckPermsIntegration(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void calculate(ServerPlayerEntity target, @NonNull ContextConsumer consumer) {
        calculate(target.getUuid(), consumer);
    }
}
