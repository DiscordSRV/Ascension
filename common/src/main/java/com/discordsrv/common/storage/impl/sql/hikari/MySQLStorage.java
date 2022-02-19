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

package com.discordsrv.common.storage.impl.sql.hikari;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.connection.StorageConfig;
import com.discordsrv.common.dependency.DependencyLoader;
import com.discordsrv.common.exception.StorageException;
import com.zaxxer.hikari.HikariConfig;
import dev.vankka.dependencydownload.classloader.IsolatedClassLoader;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public class MySQLStorage extends HikariStorage {

    private IsolatedClassLoader classLoader;

    public MySQLStorage(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void close() {
        super.close();
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (IOException e) {
                discordSRV.logger().error("Failed to close isolated classloader", e);
            }
        }
    }

    @Override
    public void createTables(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(
                    "create table if not exists LINKED_ACCOUNTS "
                            + "(ID int not null auto_increment, "
                            + "PLAYER_UUID varchar(36), "
                            + "USER_ID bigint, "
                            + "constraint LINKED_ACCOUNTS_PK primary key (ID)"
                            + ")");
        }
    }

    @Override
    public void initialize() {
        try {
            initializeWithContext(classLoader = DependencyLoader.mysql(discordSRV).loadIntoIsolated());
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    protected void applyConfiguration(HikariConfig config, StorageConfig storageConfig) {
        String address = storageConfig.remote.databaseAddress;
        if (!address.contains(":")) {
            address += ":3306";
        }

        config.setDriverClassName("com.mysql.cj.jdbc.NonRegisteringDriver");
        config.setJdbcUrl("jdbc:mysql://" + address + "/" + storageConfig.remote.databaseName);
        for (Map.Entry<Object, Object> entry : storageConfig.getDriverProperties().entrySet()) {
            config.addDataSourceProperty((String) entry.getKey(), entry.getValue());
        }

        // https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
        config.addDataSourceProperty("prepStmtCacheSize", 250);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        config.addDataSourceProperty("cachePrepStmts", true);
        config.addDataSourceProperty("useServerPrepStmts", true);
        config.addDataSourceProperty("cacheServerConfiguration", true);
        config.addDataSourceProperty("useLocalSessionState", true);
        config.addDataSourceProperty("rewriteBatchedStatements", true);
        config.addDataSourceProperty("maintainTimeStats", false);
    }
}
