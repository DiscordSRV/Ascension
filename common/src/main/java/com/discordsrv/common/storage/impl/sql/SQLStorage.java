/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.discordsrv.common.storage.impl.sql;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.exception.StorageException;
import com.discordsrv.common.function.CheckedConsumer;
import com.discordsrv.common.function.CheckedFunction;
import com.discordsrv.common.linking.impl.StorageLinker;
import com.discordsrv.common.storage.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.UUID;

public abstract class SQLStorage implements Storage {

    protected final DiscordSRV discordSRV;

    public SQLStorage(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    public abstract Connection getConnection();
    public abstract boolean isAutoCloseConnections();
    public abstract void createTables(Connection connection, String tablePrefix, boolean linkedAccounts) throws SQLException;

    private void useConnection(CheckedConsumer<Connection> connectionConsumer) throws StorageException {
        useConnection(connection -> {
            connectionConsumer.accept(connection);
            return null;
        });
    }

    private <T> T useConnection(CheckedFunction<Connection, T> connectionFunction) throws StorageException {
        try {
            if (isAutoCloseConnections()) {
                try (Connection connection = getConnection()) {
                    return connectionFunction.apply(connection);
                }
            } else {
                return connectionFunction.apply(getConnection());
            }
        } catch (Throwable e) {
            throw new StorageException(e);
        }
    }

    private void exceptEffectedRows(int rows, int expect) {
        if (rows != expect) {
            throw new StorageException("Excepted to effect " + expect + " rows, actually effected " + rows);
        }
    }

    protected String tablePrefix() {
        return discordSRV.connectionConfig().storage.sqlTablePrefix;
    }

    @Override
    public void initialize() {
        useConnection((CheckedConsumer<Connection>) connection -> createTables(
                connection,
                discordSRV.connectionConfig().storage.sqlTablePrefix,
                discordSRV.linkProvider() instanceof StorageLinker
        ));
    }

    @Override
    public @Nullable Long getUserId(@NotNull UUID player) {
        return useConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("select USER_ID from " + tablePrefix() + "LINKED_ACCOUNTS where PLAYER_UUID = ?;")) {
                statement.setString(1, player.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getLong("USER_ID");
                    }
                }
            }
            return null;
        });
    }

    @Override
    public @Nullable UUID getPlayerUUID(long userId) {
        return useConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("select PLAYER_UUID from " + tablePrefix() + "LINKED_ACCOUNTS where USER_ID = ?;")) {
                statement.setLong(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String value = resultSet.getString("PLAYER_UUID");
                        if (value == null) {
                            return null;
                        }
                        return UUID.fromString(value);
                    }
                }
            }
            return null;
        });
    }

    @Override
    public void createLink(@NotNull UUID player, long userId) {
        useConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("insert into " + tablePrefix() + "LINKED_ACCOUNTS (PLAYER_UUID, USER_ID) values (?, ?);")) {
                statement.setString(1, player.toString());
                statement.setLong(2, userId);

                exceptEffectedRows(statement.executeUpdate(), 1);
            }
        });
    }

    @Override
    public int getLinkedAccountCount() {
        return useConnection(connection -> {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("select count(*) from " + tablePrefix() + "LINKED_ACCOUNTS;")) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1);
                    }
                }
            }
            return 0;
        });
    }
}
