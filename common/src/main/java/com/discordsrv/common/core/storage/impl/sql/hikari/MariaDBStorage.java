/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.core.storage.impl.sql.hikari;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.connection.StorageConfig;
import com.discordsrv.common.exception.StorageException;
import com.zaxxer.hikari.HikariConfig;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class MariaDBStorage extends HikariStorage {

    public MariaDBStorage(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void close() {
        super.close();
    }

    @Override
    public void initialize() {
        try {
            discordSRV.dependencyManager().mariadb().downloadRelocateAndLoad().join();
            super.initialize();
        } catch (IOException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public void createTables(Connection connection, String tablePrefix) throws SQLException {
        // Same table creation language
        MySQLStorage.createTablesMySQL(connection, tablePrefix);
    }

    @Override
    protected void applyConfiguration(HikariConfig config, StorageConfig storageConfig) {
        String address = storageConfig.remote.databaseAddress;
        if (!address.contains(":")) {
            address += ":3306";
        }

        config.setDriverClassName("org.mariadb.jdbc.Driver");
        config.setJdbcUrl("jdbc:mariadb://" + address + "/" + storageConfig.remote.databaseName);
    }
}
