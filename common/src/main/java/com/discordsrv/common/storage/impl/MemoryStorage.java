package com.discordsrv.common.storage.impl;

import com.discordsrv.common.storage.Storage;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MemoryStorage implements Storage {

    public static String IDENTIFIER = UUID.randomUUID().toString();

    private final BidiMap<UUID, Long> linkedAccounts = new DualHashBidiMap<>();

    public MemoryStorage() {}

    @Override
    public void initialize() {}

    @Override
    public void close() {
        linkedAccounts.clear();
    }

    @Override
    public @Nullable Long getUserId(@NotNull UUID player) {
        return linkedAccounts.get(player);
    }

    @Override
    public @Nullable UUID getPlayerUUID(long userId) {
        return linkedAccounts.getKey(userId);
    }

    @Override
    public void createLink(@NotNull UUID player, long userId) {
        linkedAccounts.put(player, userId);
    }

    @Override
    public void removeLink(@NotNull UUID player, long userId) {
        linkedAccounts.remove(player, userId);
    }

    @Override
    public int getLinkedAccountCount() {
        return linkedAccounts.size();
    }
}
