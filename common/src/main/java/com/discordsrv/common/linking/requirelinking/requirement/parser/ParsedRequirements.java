/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.linking.requirelinking.requirement.parser;

import com.discordsrv.common.linking.requirelinking.requirement.Requirement;
import com.discordsrv.common.someone.Someone;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class ParsedRequirements {

    private final String input;
    private final Function<Someone.Resolved, CompletableFuture<Boolean>> predicate;
    private final List<Requirement<?>> usedRequirements;

    public ParsedRequirements(
            String input,
            Function<Someone.Resolved, CompletableFuture<Boolean>> predicate,
            List<Requirement<?>> usedRequirements
    ) {
        this.input = input;
        this.predicate = predicate;
        this.usedRequirements = usedRequirements;
    }

    public String input() {
        return input;
    }

    public Function<Someone.Resolved, CompletableFuture<Boolean>> predicate() {
        return predicate;
    }

    public List<Requirement<?>> usedRequirements() {
        return usedRequirements;
    }
}
