package com.discordsrv.common.sync.enums;

import com.discordsrv.common.sync.ISyncResult;

public enum SyncResults implements ISyncResult {

    // Change made
    ADD_GAME("Add game"),
    REMOVE_GAME("Remove game"),
    ADD_DISCORD("Add Discord"),
    REMOVE_DISCORD("Remove Discord"),

    // Nothing happened
    BOTH_TRUE("Both sides true"),
    BOTH_FALSE("Both sides false"),
    WRONG_DIRECTION("Wrong direction"),

    NOT_LINKED("Accounts not linked"),

    ;

    private final String prettyResult;
    private final boolean success;

    SyncResults(String prettyResult) {
        this(prettyResult, true);
    }

    SyncResults(String prettyResult, boolean success) {
        this.prettyResult = prettyResult;
        this.success = success;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return prettyResult;
    }
}
