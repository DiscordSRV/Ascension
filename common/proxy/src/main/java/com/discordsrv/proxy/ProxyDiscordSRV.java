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

package com.discordsrv.proxy;

import com.discordsrv.common.AbstractDiscordSRV;
import com.discordsrv.common.bootstrap.IBootstrap;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.main.MainConfig;
import com.discordsrv.proxy.modules.ServerSwitchMessageModule;

public abstract class ProxyDiscordSRV<B extends IBootstrap, C extends MainConfig, CC extends ConnectionConfig> extends AbstractDiscordSRV<B, C, CC> {

    public ProxyDiscordSRV(B bootstrap) {
        super(bootstrap);
    }

    @Override
    protected void enable() throws Throwable {
        super.enable();

        registerModule(ServerSwitchMessageModule::new);

        startedMessage();
    }
}
