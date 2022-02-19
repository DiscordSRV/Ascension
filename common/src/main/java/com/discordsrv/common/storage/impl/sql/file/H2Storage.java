package com.discordsrv.common.storage.impl.sql.file;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.connection.StorageConfig;
import com.discordsrv.common.dependency.DependencyLoader;
import com.discordsrv.common.exception.StorageException;
import com.discordsrv.common.storage.impl.sql.SQLStorage;
import dev.vankka.dependencydownload.classloader.IsolatedClassLoader;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class H2Storage extends SQLStorage {

    private final DiscordSRV discordSRV;
    private IsolatedClassLoader classLoader;
    private Connection connection;

    public H2Storage(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public void initialize() {
        try {
            classLoader = DependencyLoader.h2(discordSRV).loadIntoIsolated();
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
                    Boolean.class // forbidCreation
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
}
