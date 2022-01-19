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

public interface Module {

    default boolean isEnabled() {
        return true;
    }

    /**
     * Returns the priority of this Module given the lookup type.
     * @param type the type being looked up this could be an interface
     * @return the priority of this module, higher is more important. Default is 0
     */
    @SuppressWarnings("unused") // API
    default int priority(Class<?> type) {
        return 0;
    }

    default void enable() {}
    default void disable() {}
    default void reload() {}
}
