/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.storage.impl.sql.file;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.connection.StorageConfig;
import com.discordsrv.common.exception.StorageException;
import com.discordsrv.common.storage.impl.sql.SQLStorage;
import dev.vankka.dependencydownload.classloader.IsolatedClassLoader;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class H2Storage extends SQLStorage {

    private IsolatedClassLoader classLoader;
    private Connection connection;

    public H2Storage(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void initialize() {
        try {
            classLoader = discordSRV.dependencyManager().h2().loadIntoIsolated();
        } catch (IOException e) {
            throw new StorageException(e);
        }

        StorageConfig storageConfig = discordSRV.connectionConfig().storage;

        try {
            Class<?> clazz = classLoader.loadClass("org.h2.jdbc.JdbcConnection");
            Constructor<?> constructor = clazz.getConstructor(
                    String.class, // url
                    Properties.class, // info
                    String.class, // username
                    Object.class, // password
                    boolean.class // forbidCreation
            );
            connection = (Connection) constructor.newInstance(
                    "jdbc:h2:" + discordSRV.dataDirectory().resolve("h2-database").toAbsolutePath(),
                    storageConfig.getDriverProperties(),
                    null,
                    null,
                    false
            );
        } catch (ReflectiveOperationException e) {
            throw new StorageException(e);
        }
        super.initialize();
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {}
        }
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (IOException e) {
                discordSRV.logger().error("Failed to close isolated classloader", e);
            }
        }
    }

    @Override
    public synchronized Connection getConnection() {
        return connection;
    }

    @Override
    public boolean isAutoCloseConnections() {
        return false;
    }

    @Override
    public void createTables(Connection connection, String tablePrefix, boolean linkedAccounts) throws SQLException {
        if (linkedAccounts) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(
                        "create table if not exists " + tablePrefix + "linked_accounts "
                                + "(ID int not null auto_increment, "
                                + "PLAYER_UUID varchar(36), "
                                + "USER_ID bigint, "
                                + "constraint LINKED_ACCOUNTS_PK primary key (ID)"
                                + ")");
            }
        }
    }
}
