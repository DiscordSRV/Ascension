package com.discordsrv.common.bansync.enums;

public enum BanSyncResult {

    // Success, actioned
    BAN_USER("Ban user"),
    BAN_PLAYER("Ban player"),
    UNBAN_USER("Unban user"),
    UNBAN_PLAYER("Unban player"),

    // Nothing done
    ALREADY_IN_SYNC("Already in sync"),
    WRONG_DIRECTION("Wrong direction"),

    // Error
    NO_PUNISHMENT_INTEGRATION("No punishment integration"),
    NO_DISCORD_CONNECTION("No Discord connection"),
    GUILD_DOESNT_EXIST("Guild doesn't exist"),
    INVALID_CONFIG("Invalid config");

    private final String prettyResult;

    BanSyncResult(String prettyResult) {
        this.prettyResult = prettyResult;
    }

    public String prettyResult() {
        return prettyResult;
    }
}
