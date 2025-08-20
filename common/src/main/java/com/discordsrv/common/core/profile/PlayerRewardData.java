package com.discordsrv.common.core.profile;

import java.util.Objects;

public class PlayerRewardData {

    private final String name;
    private int id;
    private boolean pending;

    public PlayerRewardData(String name, boolean pending) {
        this(-1, name, pending);
    }

    public PlayerRewardData(int id, String name, boolean pending) {
        this.id = id;
        this.name = name;
        this.pending = pending;
    }

    public int getId() {
        return id;
    }

    public void setId(int rewardId) {
        if (id != -1) {
            throw new IllegalStateException("Cannot change ID of an already initialized PlayerRewardData");
        }
        this.id = rewardId;
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
