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

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.api.discord.connection.details.DiscordCacheFlag;
import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.discord.connection.details.DiscordMemberCachePolicy;
import com.discordsrv.api.module.type.Module;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.logging.NamedLogger;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

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
    public @NotNull Collection<DiscordGatewayIntent> requiredIntents() {
        return module.requiredIntents();
    }

    @Override
    public @NotNull Collection<DiscordCacheFlag> requiredCacheFlags() {
        return module.requiredCacheFlags();
    }

    @Override
    public @NotNull Collection<DiscordMemberCachePolicy> requiredMemberCachingPolicies() {
        return module.requiredMemberCachingPolicies();
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
    public Set<DiscordSRVApi.ReloadResult> reload() {
        return module.reload();
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
