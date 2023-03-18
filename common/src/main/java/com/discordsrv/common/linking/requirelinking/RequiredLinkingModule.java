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

package com.discordsrv.common.linking.requirelinking;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.linking.impl.MinecraftAuthenticationLinker;
import com.discordsrv.common.linking.requirelinking.requirement.*;
import com.discordsrv.common.linking.requirelinking.requirement.parser.RequirementParser;
import com.discordsrv.common.module.type.AbstractModule;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public abstract class RequiredLinkingModule<T extends DiscordSRV> extends AbstractModule<T> {

    private final List<Requirement<?>> availableRequirements = new ArrayList<>();

    public RequiredLinkingModule(T discordSRV) {
        super(discordSRV);
    }

    @Override
    public void reloadNoResult() {
        List<Requirement<?>> requirements = new ArrayList<>();

        requirements.add(new DiscordRoleRequirement(discordSRV));
        requirements.add(new DiscordServerRequirement(discordSRV));
        requirements.add(new DiscordBoostingRequirement(discordSRV));

        if (discordSRV.linkProvider() instanceof MinecraftAuthenticationLinker) {
            requirements.addAll(MinecraftAuthRequirement.createRequirements(discordSRV));
        }

        synchronized (availableRequirements) {
            availableRequirements.clear();
            availableRequirements.addAll(requirements);
        }
    }

    protected List<CompiledRequirement> compile(List<String> requirements) {
        List<CompiledRequirement> checks = new ArrayList<>();
        for (String requirement : requirements) {
            BiFunction<UUID, Long, CompletableFuture<Boolean>> function = RequirementParser.getInstance().parse(requirement, availableRequirements);
            checks.add(new CompiledRequirement(requirement, function));
        }
        return checks;
    }

    public static class CompiledRequirement {

        private final String input;
        private final BiFunction<UUID, Long, CompletableFuture<Boolean>> function;

        protected CompiledRequirement(String input, BiFunction<UUID, Long, CompletableFuture<Boolean>> function) {
            this.input = input;
            this.function = function;
        }

        public String input() {
            return input;
        }

        public BiFunction<UUID, Long, CompletableFuture<Boolean>> function() {
            return function;
        }
    }

}
