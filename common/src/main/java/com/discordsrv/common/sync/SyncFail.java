package com.discordsrv.common.sync;

import com.discordsrv.common.sync.result.ISyncResult;

public class SyncFail extends RuntimeException {

    private final ISyncResult result;

    public SyncFail(ISyncResult result) {
        this(result, null);
    }

    public SyncFail(ISyncResult result, Throwable cause) {
        super(cause);
        this.result = result;
    }

    public ISyncResult getResult() {
        return result;
    }
}
