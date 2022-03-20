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

package com.discordsrv.common.module.type;

import com.discordsrv.api.module.type.Module;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.logging.NamedLogger;

public class ModuleDelegate extends AbstractModule<DiscordSRV> {

    private final Module module;

    public ModuleDelegate(DiscordSRV discordSRV, Module module) {
        super(discordSRV, new NamedLogger(discordSRV, module.getClass().getName()));
        this.module = module;
    }

    @Override
    public boolean isEnabled() {
        return module.isEnabled();
    }

    @Override
    public void enable() {
        module.enable();
    }

    @Override
    public void reload() {
        module.reload();
    }

    @Override
    public void disable() {
        module.disable();
    }

    @Override
    public String toString() {
        String original = super.toString();
        return original.substring(0, original.length() - 1) + ",module=" + module.getClass().getName() + "(" + module + ")}";
    }
}
