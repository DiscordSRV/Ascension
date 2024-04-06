package com.discordsrv.common.bansync.enums;

public enum BanSyncResult {

    // Success, actioned
    BAN_USER,
    BAN_PLAYER,
    UNBAN_USER,
    UNBAN_PLAYER,

    // Nothing done
    ALREADY_IN_SYNC,
    WRONG_DIRECTION,

    // Error
    NO_PUNISHMENT_INTEGRATION,
    NO_DISCORD_CONNECTION,
    GUILD_DOESNT_EXIST

}
