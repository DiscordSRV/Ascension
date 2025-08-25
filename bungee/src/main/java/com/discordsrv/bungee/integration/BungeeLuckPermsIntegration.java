package com.discordsrv.bungee.integration;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.integration.LuckPermsIntegration;
import net.luckperms.api.context.ContextConsumer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.jspecify.annotations.NonNull;

public class BungeeLuckPermsIntegration extends LuckPermsIntegration<ProxiedPlayer> {
    public BungeeLuckPermsIntegration(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void calculate(@NonNull ProxiedPlayer target, @NonNull ContextConsumer consumer) {
        calculate(target.getUniqueId(), consumer);
    }
}
