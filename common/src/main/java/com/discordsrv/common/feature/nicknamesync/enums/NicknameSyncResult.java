package com.discordsrv.common.feature.nicknamesync.enums;

import com.discordsrv.common.abstraction.sync.result.ISyncResult;

public enum NicknameSyncResult implements ISyncResult {

    MATCH("Both sides match", false),
    SET_DISCORD("Set Discord nickname", true),
    SET_GAME("Set game nickname", true);

    private final String message;
    private final boolean update;

    NicknameSyncResult(String message, boolean update) {
        this.message = message;
        this.update = update;
    }

    @Override
    public boolean isError() {
        return false;
    }

    @Override
    public boolean isUpdate() {
        return update;
    }

    @Override
    public String getFormat() {
        return message;
    }
}
