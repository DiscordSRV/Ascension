package com.discordsrv.common.sync.result;

public enum GenericSyncResults implements ISyncResult {

    // Success, actioned
    ADD_DISCORD("Add %d"),
    REMOVE_DISCORD("Remove %d"),
    ADD_GAME("Add %g"),
    REMOVE_GAME("Remove %g"),

    // Success, Nothing done
    BOTH_TRUE("Both true"),
    BOTH_FALSE("Both false"),
    WRONG_DIRECTION("Wrong direction"),

    // Error
    NOT_LINKED("Accounts not linked"),

    ;

    private final String message;
    private final boolean success;

    GenericSyncResults(String message) {
        this(message, true);
    }

    GenericSyncResults(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public String getFormat() {
        return message;
    }
}
