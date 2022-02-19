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
import com.discordsrv.common.exception.StorageException;
import com.discordsrv.common.storage.impl.sql.SQLStorage;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class HikariStorage extends SQLStorage {

    protected final DiscordSRV discordSRV;
    private HikariDataSource hikariDataSource;

    public HikariStorage(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    protected abstract void applyConfiguration(HikariConfig config, StorageConfig storageConfig);

    protected void initializeWithContext(ClassLoader classLoader) {
        Thread currentThread = Thread.currentThread();
        ClassLoader originalContext = currentThread.getContextClassLoader();
        try {
            currentThread.setContextClassLoader(classLoader);
            initializeInternal();
        } finally {
            currentThread.setContextClassLoader(originalContext);
        }
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

        try {
            hikariDataSource = new HikariDataSource(config);
        } catch (RuntimeException e) {
            // Avoid running into runtime ClassNotFoundException by not using this as the catch
            if (e instanceof HikariPool.PoolInitializationException) {
                // Already logged by Hikari, so we'll throw an empty exception
                throw new StorageException((Throwable) null);
            }
            throw e;
        }
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
