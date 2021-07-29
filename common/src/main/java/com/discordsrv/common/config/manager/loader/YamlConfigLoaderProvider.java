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

package com.discordsrv.common.config.manager.loader;

import org.jetbrains.annotations.ApiStatus;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

public interface YamlConfigLoaderProvider extends ConfigLoaderProvider<YamlConfigurationLoader> {

    default ConfigurationOptions defaultOptions() {
        return ConfigurationOptions.defaults();
    }

    default NodeStyle nodeStyle() {
        return NodeStyle.BLOCK;
    }

    default int indent() {
        return 4;
    }

    @Override
    @ApiStatus.NonExtendable
    default AbstractConfigurationLoader.Builder<?, YamlConfigurationLoader> createBuilder() {
        return YamlConfigurationLoader.builder()
                .nodeStyle(nodeStyle())
                .indent(indent());
    }
}
