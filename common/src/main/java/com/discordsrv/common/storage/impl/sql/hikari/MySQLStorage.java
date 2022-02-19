package com.discordsrv.common.storage.impl.sql.hikari;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.connection.StorageConfig;
import com.discordsrv.common.dependency.DependencyLoader;
import com.discordsrv.common.exception.StorageException;
import com.zaxxer.hikari.HikariConfig;
import dev.vankka.dependencydownload.classloader.IsolatedClassLoader;

import java.io.IOException;
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
    public void initialize() {
        try {
            classLoader = initializeWithContext(DependencyLoader.mysql(discordSRV).loadIntoIsolated());
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

        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
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
