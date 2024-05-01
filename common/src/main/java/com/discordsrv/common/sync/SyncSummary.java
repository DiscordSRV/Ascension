package com.discordsrv.common.sync;

import com.discordsrv.common.config.main.generic.AbstractSyncConfig;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.someone.Someone;
import com.discordsrv.common.sync.cause.ISyncCause;
import com.discordsrv.common.sync.result.ISyncResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class SyncSummary<C extends AbstractSyncConfig<C, ?, ?>> {

    private final ISyncCause cause;
    private final Someone who;
    private ISyncResult allFailReason;
    private final Map<C, CompletableFuture<ISyncResult>> results = new ConcurrentHashMap<>();

    public SyncSummary(ISyncCause cause, Someone who) {
        this.cause = cause;
        this.who = who;
    }

    public ISyncCause cause() {
        return cause;
    }

    public Someone who() {
        return who;
    }

    public SyncSummary<C> fail(ISyncResult genericFail) {
        this.allFailReason = genericFail;
        return this;
    }

    public ISyncResult allFailReason() {
        return allFailReason;
    }

    public SyncSummary<C> appendResult(C config, ISyncResult result) {
        return appendResult(config, CompletableFuture.completedFuture(result));
    }

    public SyncSummary<C> appendResult(C config, CompletableFuture<ISyncResult> result) {
        this.results.put(config, result);
        return this;
    }

    public CompletableFuture<Map<C, ISyncResult>> resultFuture() {
        return CompletableFutureUtil.combine(results.values())
                .exceptionally(t -> null)
                .thenApply((__) -> {
                    Map<C, ISyncResult> results = new HashMap<>();
                    for (Map.Entry<C, CompletableFuture<ISyncResult>> entry : this.results.entrySet()) {
                        results.put(entry.getKey(), entry.getValue().join());
                    }
                    return results;
                });
    }
}
