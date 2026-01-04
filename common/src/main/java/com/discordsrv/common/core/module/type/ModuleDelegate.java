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

package com.discordsrv.common.core.module.type;

import com.discordsrv.api.discord.connection.details.DiscordCacheFlag;
import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.module.Module;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.logging.NamedLogger;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Consumer;

public class ModuleDelegate extends AbstractModule<DiscordSRV> {

    private final Module module;

    public ModuleDelegate(DiscordSRV discordSRV, Module module) {
        super(discordSRV, new NamedLogger(discordSRV, module.getClass().getName()));
        this.module = module;
    }

    public Module getBase() {
        return module;
    }

    @Override
    protected Object getEventBusListener() {
        return getBase();
    }

    @Override
    public boolean isEnabled() {
        return module.isEnabled();
    }

    @Override
    public @NotNull Collection<DiscordGatewayIntent> requiredIntents() {
        return module.requiredIntents();
    }

    @Override
    public @NotNull Collection<DiscordCacheFlag> requiredCacheFlags() {
        return module.requiredCacheFlags();
    }

    @Override
    public int priority(Class<?> type) {
        return module.priority(type);
    }

    @Override
    public int shutdownOrder() {
        return module.shutdownOrder();
    }

    @Override
    public void enable() {
        module.enable();
    }

    @Override
    public void reload(Consumer<ReloadResult> resultConsumer) {
        module.reload(resultConsumer);
    }

    @Override
    public void disable() {
        module.disable();
    }

    @Override
    public String toString() {
        return super.toString() + "{module=" + module.getClass().getName() + "(" + module + ")}";
    }
}
