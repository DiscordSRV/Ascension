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

package com.discordsrv.common.config.configurate.manager;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.configurate.manager.abstraction.TranslatedConfigManager;
import com.discordsrv.common.config.configurate.manager.loader.YamlConfigLoaderProvider;
import com.discordsrv.common.config.main.MainConfig;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.function.Supplier;

public abstract class MainConfigManager<C extends MainConfig>
        extends TranslatedConfigManager<C, YamlConfigurationLoader>
        implements YamlConfigLoaderProvider {

    private final Supplier<C> configSupplier;

    public MainConfigManager(DiscordSRV discordSRV, Supplier<C> configSupplier) {
        super(discordSRV);
        this.configSupplier = configSupplier;
    }

    protected MainConfigManager(Path dataDirectory, Supplier<C> configSupplier) {
        super(dataDirectory);
        this.configSupplier = configSupplier;
    }

    @Override
    protected Field headerField() throws ReflectiveOperationException {
        return MainConfig.class.getField("HEADER");
    }

    @Override
    public String fileName() {
        return MainConfig.FILE_NAME;
    }

    @Override
    public C createConfiguration() {
        return configSupplier.get();
    }
}
