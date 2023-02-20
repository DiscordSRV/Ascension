package com.discordsrv.common.channel;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.player.IPlayer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

public class GlobalChannel implements GameChannel {

    private final DiscordSRV discordSRV;

    public GlobalChannel(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public @NotNull String getOwnerName() {
        return "DiscordSRV";
    }

    @Override
    public @NotNull String getChannelName() {
        return "global";
    }

    @Override
    public boolean isChat() {
        return true;
    }

    @Override
    public void sendMessage(@NotNull MinecraftComponent minecraftComponent) {
        Component component = ComponentUtil.fromAPI(minecraftComponent);
        for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
            player.sendMessage(component);
        }
    }
}
