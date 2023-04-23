package com.discordsrv.bukkit.listener.award;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class BukkitAwardForwarder implements IBukkitAwardForwarder {

    public static Listener get(BukkitDiscordSRV discordSRV) {
        try {
            Class.forName("org.bukkit.event.player.PlayerAdvancementDoneEvent");
            try {
                Class.forName("io.papermc.paper.advancement.AdvancementDisplay");

                return new PaperModernAdvancementListener(discordSRV, new BukkitAwardForwarder(discordSRV));
            } catch (ClassNotFoundException ignored) {
                return new BukkitAdvancementListener(discordSRV, new BukkitAwardForwarder(discordSRV));
            }
        } catch (ClassNotFoundException ignored) {
            return new BukkitAchievementListener(discordSRV, new BukkitAwardForwarder(discordSRV));
        }
    }

    private final BukkitDiscordSRV discordSRV;

    protected BukkitAwardForwarder(BukkitDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    public void publishEvent(Player player, MinecraftComponent message, MinecraftComponent advancementName, boolean cancelled) {
        // TODO
    }
}
