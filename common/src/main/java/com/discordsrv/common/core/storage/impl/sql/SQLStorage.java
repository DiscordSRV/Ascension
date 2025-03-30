/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.core.storage.impl.sql;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.profile.DiscordProfileData;
import com.discordsrv.common.core.profile.GameProfileData;
import com.discordsrv.common.core.storage.Storage;
import com.discordsrv.common.exception.StorageException;
import com.discordsrv.common.feature.linking.AccountLink;
import com.discordsrv.common.feature.linking.LinkStore;
import com.discordsrv.common.util.function.CheckedConsumer;
import com.discordsrv.common.util.function.CheckedFunction;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public abstract class SQLStorage implements Storage {

    protected static final String LINKED_ACCOUNTS_TABLE_NAME = "linked_accounts";
    protected static final String LINKING_CODES_TABLE_NAME = "linking_codes";
    protected static final String GAME_PROFILE_TABLE_NAME = "game_profile";
    protected static final String DISCORD_PROFILE_TABLE_NAME = "discord_profile";
    protected static final String REWARD_TABLE_NAME = "reward";
    protected static final String GAME_GRANTED_REWARDS_TABLE_NAME = "game_granted_rewards";
    protected static final String DISCORD_GRANTED_REWARDS_TABLE_NAME = "discord_granted_rewards";

    protected final DiscordSRV discordSRV;

    public SQLStorage(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    public abstract Connection getConnection();
    public abstract boolean isAutoCloseConnections();
    public abstract void createTables(Connection connection, String tablePrefix) throws SQLException;

    protected static void createRewardsTablesGeneric(Connection connection, String tablePrefix) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "create table if not exists " + tablePrefix + GAME_PROFILE_TABLE_NAME + " ("
                            + "ID int not null auto_increment,"
                            + "PLAYER_UUID varchar(36),"
                            + "constraint GAME_PROFILE_PK primary key (ID),"
                            + "constraint GAME_PROFILE_UQ unique (PLAYER_UUID)"
                            + ");"
            );
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "create table if not exists " + tablePrefix + DISCORD_PROFILE_TABLE_NAME + " ("
                            + "ID int not null auto_increment,"
                            + "USER_ID bigint,"
                            + "constraint DISCORD_PROFILE_PK primary key (ID),"
                            + "constraint DISCORD_PROFILE_UQ unique (USER_ID)"
                            + ");"
            );
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "create table if not exists " + tablePrefix + REWARD_TABLE_NAME + " ("
                            + "ID int not null auto_increment,"
                            + "REWARD varchar(32),"
                            + "constraint REWARD_PK primary key (ID),"
                            + "constraint REWARD_UQ unique (REWARD)"
                            + ");"
            );
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "create table if not exists " + tablePrefix + GAME_GRANTED_REWARDS_TABLE_NAME + " ("
                            + "ID int not null auto_increment,"
                            + "PROFILE_ID int,"
                            + "REWARD_ID int,"
                            + "constraint GAME_GRANTED_REWARD_PK primary key (ID),"
                            + "constraint GAME_GRANTED_REWARD_UQ unique (PROFILE_ID, REWARD_ID),"
                            + "foreign key (PROFILE_ID) references " + tablePrefix + GAME_PROFILE_TABLE_NAME + "(ID),"
                            + "foreign key (REWARD_ID) references " + tablePrefix + REWARD_TABLE_NAME + "(ID)"
                            + ");"
            );
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "create table if not exists " + tablePrefix + DISCORD_GRANTED_REWARDS_TABLE_NAME + " ("
                            + "ID int not null auto_increment,"
                            + "PROFILE_ID int,"
                            + "REWARD_ID int,"
                            + "constraint DISCORD_GRANTED_REWARD_PK primary key (ID),"
                            + "constraint DISCORD_GRANTED_REWARD_UQ unique (PROFILE_ID, REWARD_ID),"
                            + "foreign key (PROFILE_ID) references " + tablePrefix + DISCORD_PROFILE_TABLE_NAME + "(ID),"
                            + "foreign key (REWARD_ID) references " + tablePrefix + REWARD_TABLE_NAME + "(ID)"
                            + ");"
            );
        }
    }

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
        String tablePrefix = discordSRV.connectionConfig().storage.sqlTablePrefix;
        if (!tablePrefix.matches("[\\w_-]*")) {
            throw new IllegalStateException("SQL Table prefix may not contain non alphanumeric characters, dashes and underscores!");
        }

        return tablePrefix;
    }

    @Override
    public void initialize() {
        useConnection((CheckedConsumer<Connection>) connection -> createTables(
                connection,
                tablePrefix()
        ));
    }

    @Override
    public @Nullable AccountLink getLinkByPlayerUUID(@NotNull UUID playerUUID) {
        return useConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("select USER_ID, CREATED, LASTSEEN from " + tablePrefix() + LINKED_ACCOUNTS_TABLE_NAME + " where PLAYER_UUID = ?;")) {
                statement.setString(1, playerUUID.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return getAccountLink(playerUUID, resultSet.getLong("USER_ID"), resultSet);
                    }
                }
            }
            return null;
        });
    }

    @Override
    public @Nullable AccountLink getLinkByUserId(long userId) {
        return useConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("select PLAYER_UUID, CREATED, LASTSEEN from " + tablePrefix() + LINKED_ACCOUNTS_TABLE_NAME + " where USER_ID = ?;")) {
                statement.setLong(1, userId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        String value = resultSet.getString("PLAYER_UUID");
                        if (value == null) {
                            return null;
                        }
                        return getAccountLink(UUID.fromString(value), userId, resultSet);
                    }
                }
            }
            return null;
        });
    }

    private AccountLink getAccountLink(UUID playerUUID, long userId, ResultSet resultSet) throws SQLException {
        LocalDateTime created = resultSet.getObject("CREATED", LocalDateTime.class);
        LocalDateTime used = resultSet.getObject("LASTSEEN", LocalDateTime.class);
        return new AccountLink(playerUUID, userId, created, used);
    }

    @Override
    public void createLink(@NotNull AccountLink link) {
        useConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("insert into " + tablePrefix() + LINKED_ACCOUNTS_TABLE_NAME + " (PLAYER_UUID, USER_ID, CREATED, LASTSEEN) values (?, ?, ?, ?);")) {
                statement.setString(1, link.playerUUID().toString());
                statement.setLong(2, link.userId());
                statement.setObject(3, link.created());
                statement.setObject(4, link.lastSeen());

                exceptEffectedRows(statement.executeUpdate(), 1);
            }
        });
    }

    @Override
    public void removeLink(@NotNull UUID playerUUID, long userId) {
        useConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("delete from " + tablePrefix() + LINKED_ACCOUNTS_TABLE_NAME + " where PLAYER_UUID = ?;")) {
                statement.setString(1, playerUUID.toString());
                exceptEffectedRows(statement.executeUpdate(), 1);
            }
        });
    }

    @Override
    public int getLinkedAccountCount() {
        return useConnection(connection -> {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet resultSet = statement.executeQuery("select count(*) from " + tablePrefix() + LINKED_ACCOUNTS_TABLE_NAME + ";")) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1);
                    }
                }
            }
            return 0;
        });
    }

    private long getTimeMS() {
        return Calendar.getInstance().getTimeInMillis();
    }

    @Override
    public Pair<UUID, String> getLinkingCode(String code) {
        return useConnection(connection -> {
            // Get the uuid for the code
            try (PreparedStatement statement = connection.prepareStatement("select PLAYERUUID, PLAYERUSERNAME from " + tablePrefix() + LINKING_CODES_TABLE_NAME + " where CODE = ? LIMIT 1;")) {
                statement.setString(1, code);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        UUID uuid = UUID.fromString(resultSet.getString("PLAYERUUID"));
                        String username = resultSet.getString("PLAYERUSERNAME");
                        return Pair.of(uuid, username);
                    }
                }
            }
            return null;
        });
    }

    @Override
    public void removeLinkingCode(@NotNull UUID playerUUID) {
        useConnection(connection -> {
            try (PreparedStatement statement = connection.prepareStatement("delete from " + tablePrefix() + LINKING_CODES_TABLE_NAME + " WHERE PLAYERUUID = ?;")) {
                statement.setString(1, playerUUID.toString());
                statement.executeUpdate();
            }
        });
    }

    @Override
    public void storeLinkingCode(@NotNull UUID playerUUID, @NotNull String username, String code) {
        useConnection(connection -> {
            // Remove existing code
            try (PreparedStatement statement = connection.prepareStatement("delete from " + tablePrefix() + LINKING_CODES_TABLE_NAME + " where PLAYERUUID = ?;")) {
                statement.setString(1, playerUUID.toString());
                statement.executeUpdate();
            }

            // Insert new code
            try (PreparedStatement statement = connection.prepareStatement("insert into " + tablePrefix() + LINKING_CODES_TABLE_NAME + " (PLAYERUUID, PLAYERUSERNAME, CODE, EXPIRY) VALUES (?, ?, ?, ?);")) {
                statement.setString(1, playerUUID.toString());
                statement.setString(2, username);
                statement.setString(3, code);
                statement.setLong(4, getTimeMS() + LinkStore.LINKING_CODE_EXPIRY_TIME.toMillis());
                exceptEffectedRows(statement.executeUpdate(), 1);
            }
        });
    }

    private List<Integer> getOrCreateRewards(Connection connection, Set<String> rewards) throws SQLException {
        Map<String, Integer> rewardMap = new HashMap<>();
        connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        try (Statement statement = connection.createStatement()) {
            statement.execute("BEGIN TRANSACTION;");
        }
        try (PreparedStatement statement = connection.prepareStatement("select ID, REWARD from " + tablePrefix() + REWARD_TABLE_NAME + ";")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rewardMap.put(
                            resultSet.getString("REWARD"),
                            resultSet.getInt("ID")
                    );
                }
            }
        }

        List<Integer> rewardIds = new ArrayList<>();
        Set<String> missingRewards = new LinkedHashSet<>();
        for (String reward : rewards) {
            Integer rewardId = rewardMap.get(reward);
            if (rewardId == null) {
                missingRewards.add(reward);
                continue;
            }

            rewardIds.add(rewardId);
        }

        for (String missingReward : missingRewards) {
            try (PreparedStatement statement = connection.prepareStatement("insert into " + tablePrefix() + REWARD_TABLE_NAME + " (REWARD) VALUES (?);")) {
                statement.setString(1, missingReward);
                exceptEffectedRows(statement.executeUpdate(), 1);
            }
            try (PreparedStatement statement = connection.prepareStatement("select ID from " + tablePrefix() + REWARD_TABLE_NAME + " where REWARD = ?;")) {
                statement.setString(1, missingReward);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("Reward not found after insert");
                    }
                    rewardIds.add(resultSet.getInt("ID"));
                }
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("COMMIT;");
        }
        return rewardIds;
    }

    private void alterRewardTable(Connection connection, String tableName, int profileId, Set<String> rewards) throws SQLException {
        List<Integer> rewardIds = getOrCreateRewards(connection, rewards);
        List<Integer> currentRewardIds = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
                "select REWARD_ID from " + tablePrefix() + tableName + " where PROFILE_ID = ?"
        )) {
            statement.setInt(1, profileId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    currentRewardIds.add(resultSet.getInt("REWARD_ID"));
                }
            }
        }

        List<Integer> missingRewards = rewardIds.stream()
                .filter(rewardId -> !currentRewardIds.contains(rewardId))
                .collect(Collectors.toList());
        for (Integer missingRewardId : missingRewards) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "insert into " + tablePrefix() + tableName + " (PROFILE_ID, REWARD_ID) VALUES (?, ?);"
            )) {
                statement.setInt(1, profileId);
                statement.setInt(2, missingRewardId);
                exceptEffectedRows(statement.executeUpdate(), 1);
            }
        }

        List<Integer> removedRewards = currentRewardIds.stream()
                .filter(rewardId -> !rewardIds.contains(rewardId))
                .collect(Collectors.toList());
        for (Integer removedRewardId : removedRewards) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "delete from " + tablePrefix() + tableName + " where PROFILE_ID = ? and REWARD_ID = ?;"
            )) {
                statement.setInt(1, profileId);
                statement.setInt(2, removedRewardId);
                exceptEffectedRows(statement.executeUpdate(), 1);
            }
        }
    }

    private Integer getGameProfile(Connection connection, UUID playerUUID) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select ID from " + tablePrefix() + GAME_PROFILE_TABLE_NAME + " where PLAYER_UUID = ?")) {
            statement.setString(1, playerUUID.toString());

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("ID");
                }
            }
        }
        return null;
    }

    @Override
    public GameProfileData getGameProfileData(@NotNull UUID playerUUID) {
        return useConnection(connection -> {
            Integer profileId = getGameProfile(connection, playerUUID);
            if (profileId == null) {
                return null;
            }

            Set<String> grantedRewards = new HashSet<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "select r.REWARD from " + tablePrefix() + GAME_GRANTED_REWARDS_TABLE_NAME + " gr"
                            + " inner join " + tablePrefix() + REWARD_TABLE_NAME + " r on r.ID = gr.REWARD_ID"
                            + " where gr.PROFILE_ID = ?"
            )) {
                statement.setInt(1, profileId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        grantedRewards.add(resultSet.getString("REWARD"));
                    }
                }
            }

            return new GameProfileData(playerUUID, grantedRewards);
        });
    }

    @Override
    public void saveGameProfileData(@NotNull GameProfileData profile) {
        UUID playerUUID = profile.getPlayerUUID();
        useConnection(connection -> {
            Integer profileId = getGameProfile(connection, playerUUID);
            if (profileId == null) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "insert into " + tablePrefix() + GAME_PROFILE_TABLE_NAME + " (PLAYER_UUID) VALUES (?);"
                )) {
                    statement.setString(1, playerUUID.toString());
                    exceptEffectedRows(statement.executeUpdate(), 1);
                }

                profileId = getGameProfile(connection, playerUUID);
                if (profileId == null) {
                    throw new SQLException("Profile not found after insert");
                }
            }

            alterRewardTable(connection, GAME_GRANTED_REWARDS_TABLE_NAME, profileId, profile.getGrantedRewards());
        });
    }

    private Integer getDiscordProfile(Connection connection, long userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "select ID from " + tablePrefix() + DISCORD_PROFILE_TABLE_NAME + " where USER_ID = ?")) {
            statement.setLong(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("ID");
                }
            }
        }
        return null;
    }

    @Override
    public DiscordProfileData getDiscordProfileData(long userId) {
        return useConnection(connection -> {
            Integer profileId = getDiscordProfile(connection, userId);
            if (profileId == null) {
                return null;
            }

            Set<String> grantedRewards = new HashSet<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "select r.REWARD from " + tablePrefix() + DISCORD_GRANTED_REWARDS_TABLE_NAME + " dr"
                            + " inner join " + tablePrefix() + REWARD_TABLE_NAME + " r on r.ID = dr.REWARD_ID"
                            + " where dr.PROFILE_ID = ?"
            )) {
                statement.setInt(1, profileId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        grantedRewards.add(resultSet.getString("REWARD"));
                    }
                }
            }

            return new DiscordProfileData(userId, grantedRewards);
        });
    }

    @Override
    public void saveDiscordProfileData(@NotNull DiscordProfileData profile) {
        long userId = profile.getUserId();
        useConnection(connection -> {
            Integer profileId = getDiscordProfile(connection, userId);
            if (profileId == null) {
                try (PreparedStatement statement = connection.prepareStatement(
                        "insert into " + tablePrefix() + DISCORD_PROFILE_TABLE_NAME + " (USER_ID) VALUES (?);"
                )) {
                    statement.setLong(1, userId);
                    exceptEffectedRows(statement.executeUpdate(), 1);
                }

                profileId = getDiscordProfile(connection, userId);
                if (profileId == null) {
                    throw new SQLException("Profile not found after insert");
                }
            }

            alterRewardTable(connection, DISCORD_GRANTED_REWARDS_TABLE_NAME, profileId, profile.getGrantedRewards());
        });
    }
}
