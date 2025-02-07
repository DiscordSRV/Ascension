package com.discordsrv.common.config.main;

import com.discordsrv.common.config.main.generic.AbstractSyncConfig;
import com.discordsrv.common.feature.nicknamesync.NicknameSyncModule;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class NicknameSyncConfig extends AbstractSyncConfig<NicknameSyncConfig, NicknameSyncModule.Game, Long> {

    @Comment("The id for the Discord server where the nicknames should be synced from/to")
    public long serverId = 0L;

    @Override
    public boolean isSet() {
        return serverId != 0;
    }

    @Override
    public NicknameSyncModule.Game gameId() {
        return NicknameSyncModule.Game.INSTANCE;
    }

    @Override
    public Long discordId() {
        return serverId;
    }

    @Override
    public boolean isSameAs(NicknameSyncConfig otherConfig) {
        return false;
    }

    @Override
    public String describe() {
        return Long.toUnsignedString(serverId);
    }
}
