package com.discordsrv.common.bansync.enums;

import com.discordsrv.common.sync.result.ISyncResult;

public enum BanSyncResult implements ISyncResult {

    // Error
    NO_PUNISHMENT_INTEGRATION("No punishment integration"),
    NO_DISCORD_CONNECTION("No Discord connection"),
    GUILD_DOESNT_EXIST("Guild doesn't exist"),
    INVALID_CONFIG("Invalid config"),
    ;

    private final String format;

    BanSyncResult(String format) {
        this.format = format;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public String getFormat() {
        return format;
    }
}
