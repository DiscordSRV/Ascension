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

package com.discordsrv.common.config.connection;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

@ConfigSerializable
public class StorageConfig {

    @Comment("The storage backend to use.\n\n"
            + "- H2\n"
            + "- MySQL\n"
            + "- MariaDB")
    public String backend = "H2";

    @Comment("SQL table prefix")
    public String sqlTablePrefix = "discordsrv_";

    @Comment("Connection options for remote databases (MySQL, MariaDB)")
    public Remote remote = new Remote();

    @Comment("Extra connection properties for database drivers")
    public Map<String, String> driverProperties = new LinkedHashMap<String, String>() {{
        put("useSSL", "false");
    }};

    public Properties getDriverProperties() {
        Properties properties = new Properties();
        for (Map.Entry<String, String> property : driverProperties.entrySet()) {
            String key = property.getKey();
            String value = property.getValue();
            if (value.equals("true")) {
                properties.put(key, true);
            } else if (value.equals("false")) {
                properties.put(key, false);
            } else {
                properties.put(key, value);
            }
        }
        return properties;
    }

    public static class Remote {

        @Comment("The database address.\n"
                + "Uses the default port (MySQL: 3306)\n"
                + "for the database if a port isn't specified in the \"address:port\" format\n"
                + "Please make sure the port for your database is open and your firewall(s) allow(s) connections from the server to the database")
        public String databaseAddress = "localhost";

        @Comment("The name of the database")
        public String databaseName = "minecraft";

        @Comment("The database username and password")
        public String username = "root";
        public String password = "";

        @Comment("Connection pool options. Don't touch these unless you know what you're doing")
        public Pool poolOptions = new Pool();

    }

    public static class Pool {

        @Comment("The maximum number of concurrent connections to keep to the database")
        public int maximumPoolSize = 5;

        @Comment("The minimum number of concurrent connections to keep to the database")
        public int minimumPoolSize = 2;

        @Comment("How frequently to attempt to keep connections alive, in order to prevent being timed out by the database or network infrastructure.\n"
                + "The time is specified in milliseconds. Use 0 to disable keepalive."
                + "The default is 0 (disabled)")
        public long keepaliveTime = 0;

        @Comment("The maximum time a connection will be kept open in milliseconds.\n"
                + "The time is specified in milliseconds. Must be at least 30000ms (30 seconds)"
                + "The default is 1800000ms (30 minutes)")
        public long maximumLifetime = 1800000;

    }
}
