package com.discordsrv.common.core.profile;

import java.util.Objects;

public class PlayerRewardData {

    private final int id;
    private final String name;
    private boolean pending;

    public PlayerRewardData(int id, String name, boolean pending) {
        this.id = id;
        this.name = name;
        this.pending = pending;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isPending() {
        return pending;
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayerRewardData)) return false;
        PlayerRewardData that = (PlayerRewardData) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
