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

package com.discordsrv.common.config.manager;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.manager.loader.YamlConfigLoaderProvider;
import com.discordsrv.common.config.manager.manager.TranslatedConfigManager;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.objectmapping.ObjectMapper;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public abstract class ConnectionConfigManager<C extends ConnectionConfig>
        extends TranslatedConfigManager<C, YamlConfigurationLoader>
        implements YamlConfigLoaderProvider {

    public ConnectionConfigManager(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public ConfigurationOptions defaultOptions(ObjectMapper.Factory objectMapper) {
        return super.defaultOptions(objectMapper)
                .header("DiscordSRV's configuration file for connections to different external services.\n"
                                + "This file is intended to contain connection details to services in order to keep them out of the config.yml\n"
                                + "and to serve as a easy way to identify and control what external connections are being used.\n"
                                + "\n"
                                + "All domains listed as \"Requires a connection to\" require port 443 (https/wss) unless otherwise specified\n"
                                + "\n"
                                + " ABSOLUTELY DO NOT SEND THIS FILE TO ANYONE - IT ONLY CONTAINS SECRETS\n");
    }

    @Override
    protected String fileName() {
        return ConnectionConfig.FILE_NAME;
    }
}
