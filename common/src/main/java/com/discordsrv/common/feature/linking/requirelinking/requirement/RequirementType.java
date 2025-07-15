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

package com.discordsrv.common.feature.linking.requirelinking.requirement;

import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.feature.linking.requirelinking.RequiredLinkingModule;
import com.discordsrv.common.helper.Someone;

public abstract class RequirementType<T> extends AbstractModule<DiscordSRV> {

    protected final RequiredLinkingModule<? extends DiscordSRV> module;

    public RequirementType(RequiredLinkingModule<? extends DiscordSRV> module) {
        super(module.discordSRV());
        this.module = module;
    }

    public final void stateChanged(Someone someone, T value, boolean newState) {
        module.stateChanged(someone, this, value, newState);
    }

    public abstract String name();
    public abstract T parse(String input);
    public abstract Task<Boolean> isMet(T value, Someone.Resolved someone);

}
