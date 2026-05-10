/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.velocity.module;

import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.velocity.VelocityDiscordSRV;

public class AbstractVelocityModule extends AbstractModule<VelocityDiscordSRV> {

    public AbstractVelocityModule(VelocityDiscordSRV discordSRV, Logger logger) {
        super(discordSRV, logger);
    }

    @Override
    public void enable() {
        discordSRV.proxy().getEventManager().register(discordSRV.plugin(), this);
    }

    @Override
    public void disable() {
        discordSRV.proxy().getEventManager().unregisterListener(discordSRV.plugin(), this);
    }
}
