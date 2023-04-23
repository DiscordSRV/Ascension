package com.discordsrv.bukkit.listener.award;

import com.discordsrv.api.component.MinecraftComponent;
import org.bukkit.entity.Player;

public interface IBukkitAwardForwarder {
    void publishEvent(Player player, MinecraftComponent message, MinecraftComponent name, boolean cancelled);
}
