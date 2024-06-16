package com.discordsrv.common.storage.impl.sql.hikari;

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
