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

package com.discordsrv.common.module.type;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.logging.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.OverridingMethodsMustInvokeSuper;

public abstract class PluginIntegration<DT extends DiscordSRV> extends AbstractModule<DT> {

    public PluginIntegration(DT discordSRV) {
        super(discordSRV);
    }

    public PluginIntegration(DT discordSRV, Logger logger) {
        super(discordSRV, logger);
    }

    @NotNull
    public abstract String getIntegrationName();

    @Override
    @OverridingMethodsMustInvokeSuper
    public boolean isEnabled() {
        if (discordSRV.config().integrations().disabledIntegrations.contains(getIntegrationName())) {
            return false;
        }
        return super.isEnabled();
    }
}
