package com.discordsrv.api.punishment;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public class Punishment {

    private final Instant until;
    private final String reason;
    private final String punisher;

    public Punishment(@Nullable Instant until, @Nullable String reason, @Nullable String punisher) {
        this.until = until;
        this.reason = reason;
        this.punisher = punisher;
    }

    public Instant until() {
        return until;
    }

    public String reason() {
        return reason;
    }

    public String punisher() {
        return punisher;
    }
}
