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

import java.util.Objects;

@ConfigSerializable
public class HttpProxyConfig {

    public boolean enabled = false;
    public String host = "example.com";
    public int port = 8080;
    public BasicAuthConfig basicAuth = new BasicAuthConfig();

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        HttpProxyConfig that = (HttpProxyConfig) o;
        return enabled == that.enabled
                && port == that.port
                && Objects.equals(host, that.host)
                && Objects.equals(basicAuth, that.basicAuth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(enabled, host, port, basicAuth);
    }

    @ConfigSerializable
    public static class BasicAuthConfig {

        public boolean enabled = true;
        public String username = "discordsrv";
        public String password = "";

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            BasicAuthConfig that = (BasicAuthConfig) o;
            return enabled == that.enabled
                    && Objects.equals(username, that.username)
                    && Objects.equals(password, that.password);
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, username, password);
        }
    }
}
