package com.discordsrv.common.config.main;

import com.discordsrv.common.config.main.generic.AbstractSyncConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class BanSyncConfig extends AbstractSyncConfig<BanSyncConfig, Void, Long> {

    @Comment("The id for the Discord server where the bans should be synced from/to")
    public long serverId = 0L;

    @Comment("The reason applied when creating new bans in Minecraft")
    public String gameBanReasonFormat = "%reason%";

    @Comment("The punisher applied when creating new bans in Minecraft")
    public String gamePunisherFormat = "@%user_effective_server_name%";

    @Comment("The reason applied when creating new bans in Discord")
    public String discordBanReasonFormat = "Banned by %punishment_punisher% in Minecraft for %punishment_reason%, ends: %punishment_until:'YYYY-MM-dd HH:mm:ss zzz'|text:'Never'%";

    @Comment("The reason applied when removing bans in Discord")
    public String discordUnbanReasonFormat = "Unbanned in Minecraft";

    @Comment("The amount of hours to delete Discord messages, when syncing bans from Minecraft to Discord")
    public int discordMessageHoursToDelete = 0;

    @Comment("Resync upon linking")
    public boolean resyncUponLinking = true;

    @Override
    public boolean isSet() {
        return serverId != 0;
    }

    @Override
    public Void gameId() {
        return null;
    }

    @Override
    public Long discordId() {
        return serverId;
    }

    @Override
    public boolean isSameAs(BanSyncConfig config) {
        return false;
    }

    @Override
    public String describe() {
        return "Ban sync";
    }
}
