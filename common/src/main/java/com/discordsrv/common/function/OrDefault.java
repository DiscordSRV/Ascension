/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.function;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public class OrDefault<T> {

    private final T primary;
    private final T secondary;

    public OrDefault(T primary, T secondary) {
        this.primary = primary;
        this.secondary = secondary;
    }

    public <R> R get(Function<T, R> function, R otherwise) {
        R value = get(function);
        return value != null ? value : otherwise;
    }

    @Nullable
    public <R> R get(Function<T, R> function) {
        if (primary != null) {
            R primaryValue = function.apply(primary);
            if (primaryValue != null) {
                return primaryValue;
            }
        }

        return function.apply(secondary);
    }

    public <R> OrDefault<R> map(Function<T, R> mappingFunction) {
        R primaryValue = null;
        R secondaryValue = null;
        if (primary != null) {
            primaryValue = mappingFunction.apply(primary);
        }
        if (secondary != null) {
            secondaryValue = mappingFunction.apply(secondary);
        }
        return new OrDefault<>(primaryValue, secondaryValue);
    }
}
