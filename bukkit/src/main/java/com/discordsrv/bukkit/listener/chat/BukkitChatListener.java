package com.discordsrv.bukkit.listener.chat;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.component.util.ComponentUtil;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class BukkitChatListener implements Listener {

    private final IBukkitChatForwarder forwarder;

    public BukkitChatListener(IBukkitChatForwarder forwarder) {
        this.forwarder = forwarder;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAsyncPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        MinecraftComponent component = ComponentUtil.toAPI(
                BukkitComponentSerializer.legacy().deserialize(event.getMessage()));

        forwarder.publishEvent(event.getPlayer(), component, event.isCancelled());
    }
}
