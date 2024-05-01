package com.discordsrv.common.sync.cause;

public enum GenericSyncCauses implements ISyncCause {

    API("API"),
    COMMAND("Command"),
    GAME_JOIN("Joined game"),
    LINK("Linked account"),
    TIMER("Timed synchronization"),

    ;

    private final String prettyCause;

    GenericSyncCauses(String prettyCause) {
        this.prettyCause = prettyCause;
    }

    @Override
    public String toString() {
        return prettyCause;
    }
}
