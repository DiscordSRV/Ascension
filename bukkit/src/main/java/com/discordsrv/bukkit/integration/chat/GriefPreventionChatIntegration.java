package com.discordsrv.bukkit.integration.chat;

import com.discordsrv.api.event.bus.EventPriority;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.message.receive.game.GameChatMessageReceiveEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.PluginIntegration;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.jetbrains.annotations.NotNull;

public class GriefPreventionChatIntegration extends PluginIntegration<BukkitDiscordSRV> {

    public GriefPreventionChatIntegration(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "GRIEFPREVENTION"));
    }

    @Override
    public @NotNull String getIntegrationName() {
        return "GriefPrevention";
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
        } catch (ClassNotFoundException ignored) {
            return false;
        }

        return super.isEnabled();
    }

    @Subscribe(priority = EventPriority.EARLY)
    public void onGameChatMessageReceive(GameChatMessageReceiveEvent event) {
        GriefPrevention griefPrevention = (GriefPrevention) discordSRV.server().getPluginManager().getPlugin(getIntegrationName());
        if (griefPrevention == null) {
            return;
        }

        DiscordSRVPlayer player = event.getPlayer();
        if (griefPrevention.dataStore.isSoftMuted(player.uniqueId())) {
            logger().debug(player.username() + " is softmuted");
            event.setCancelled(true);
        }
    }
}
