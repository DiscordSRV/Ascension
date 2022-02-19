package com.discordsrv.common.storage.impl.sql.hikari;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.connection.StorageConfig;
import com.discordsrv.common.exception.StorageException;
import com.discordsrv.common.storage.impl.sql.SQLStorage;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class HikariStorage extends SQLStorage {

    protected final DiscordSRV discordSRV;
    private HikariDataSource hikariDataSource;

    public HikariStorage(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    protected abstract void applyConfiguration(HikariConfig config, StorageConfig storageConfig);

    protected <T extends ClassLoader> T initializeWithContext(T classLoader) {
        Thread currentThread = Thread.currentThread();
        ClassLoader originalContext = currentThread.getContextClassLoader();
        try {
            currentThread.setContextClassLoader(classLoader);
            initializeInternal();
        } finally {
            currentThread.setContextClassLoader(originalContext);
        }
        return classLoader;
    }

    private void initializeInternal() {
        StorageConfig storageConfig = discordSRV.connectionConfig().storage;
        StorageConfig.Remote remoteConfig = storageConfig.remote;
        StorageConfig.Pool poolConfig = remoteConfig.poolOptions;

        HikariConfig config = new HikariConfig();
        config.setPoolName("discordsrv-pool");
        config.setUsername(remoteConfig.username);
        config.setPassword(remoteConfig.password);
        config.setMinimumIdle(poolConfig.minimumPoolSize);
        config.setMaximumPoolSize(poolConfig.maximumPoolSize);
        config.setMaxLifetime(poolConfig.maximumLifetime);
        config.setKeepaliveTime(poolConfig.keepaliveTime);
        applyConfiguration(config, storageConfig);

        hikariDataSource = new HikariDataSource(config);
        super.initialize();
    }

    @Override
    public void initialize() {
        initializeInternal();
    }

    @Override
    public void close() {
        if (hikariDataSource != null) {
            hikariDataSource.close();
        }
    }

    @Override
    public Connection getConnection() {
        try {
            return hikariDataSource.getConnection();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    @Override
    public boolean isAutoCloseConnections() {
        return true;
    }
}
