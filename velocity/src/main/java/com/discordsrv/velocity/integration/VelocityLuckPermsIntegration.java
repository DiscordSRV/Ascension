package com.discordsrv.velocity.integration;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.integration.LuckPermsIntegration;
import com.velocitypowered.api.proxy.Player;
import net.luckperms.api.context.ContextConsumer;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class VelocityLuckPermsIntegration extends LuckPermsIntegration<Player> {
    public VelocityLuckPermsIntegration(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void calculate(@NotNull Player target, @NonNull ContextConsumer consumer) {
        calculate(target.getUniqueId(), consumer);
    }
}
