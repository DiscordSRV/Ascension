package com.discordsrv.common.player;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.player.provider.model.SkinInfo;
import net.kyori.adventure.identity.Identity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class OfflinePlayer implements IOfflinePlayer {

    private final DiscordSRV discordSRV;
    private final String username;
    private final Identity identity;
    private final SkinInfo skinInfo;

    public OfflinePlayer(DiscordSRV discordSRV, String username, UUID uuid, SkinInfo skinInfo) {
        this.discordSRV = discordSRV;
        this.username = username;
        this.identity = Identity.identity(uuid);
        this.skinInfo = skinInfo;
    }

    @Override
    public DiscordSRV discordSRV() {
        return discordSRV;
    }

    @Override
    public @Nullable String username() {
        return username;
    }

    @Override
    public @NotNull Identity identity() {
        return identity;
    }

    @Override
    public @Nullable SkinInfo skinInfo() {
        return skinInfo;
    }
}
