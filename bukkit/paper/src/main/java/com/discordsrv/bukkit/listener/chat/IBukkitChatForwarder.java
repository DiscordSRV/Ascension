package com.discordsrv.bukkit.listener.chat;

import com.discordsrv.api.component.MinecraftComponent;
import org.bukkit.entity.Player;

public interface IBukkitChatForwarder {

    void publishEvent(Player player, MinecraftComponent component, boolean cancelled);
}
