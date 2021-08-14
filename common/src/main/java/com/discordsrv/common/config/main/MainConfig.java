/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.config.main;

import com.discordsrv.common.config.Config;
import com.discordsrv.common.config.annotation.DefaultOnly;
import com.discordsrv.common.config.main.channels.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.ChannelConfig;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.HashMap;
import java.util.Map;

@ConfigSerializable
public class MainConfig implements Config {

    public static final String FILE_NAME = "config.yaml";

    @Override
    public final String getFileName() {
        return FILE_NAME;
    }

    @DefaultOnly("default")
    public Map<String, BaseChannelConfig> channels = new HashMap<String, BaseChannelConfig>() {{
        put("default", new BaseChannelConfig());
        put("global", new ChannelConfig());
    }};
}
