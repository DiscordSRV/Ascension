package com.discordsrv.bukkit.integration;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.integration.LuckPermsIntegration;
import net.luckperms.api.context.ContextConsumer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class BukkitLuckPermsIntegration extends LuckPermsIntegration<Player> {
    public BukkitLuckPermsIntegration(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void calculate(@NonNull Player target, @NonNull ContextConsumer consumer) {
        calculate(target.getUniqueId(), consumer);
    }
}
