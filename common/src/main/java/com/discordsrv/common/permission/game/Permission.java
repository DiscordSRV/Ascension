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

package com.discordsrv.common.permission.game;

public interface Permission {

    String PERMISSION_PREFIX = "discordsrv.";

    String permission();

    /**
     * If a given permission's default should be OP, rather than being granted by default.
     * @return {@code true} if the permission should be restricted to, at least OPs
     */
    boolean requiresOpByDefault();

    static Permission of(String permission) {
        return of(permission, true);
    }

    static Permission of(String permission, boolean requiresOpByDefault) {
        return ofGeneric(PERMISSION_PREFIX + permission, requiresOpByDefault);
    }

    static Permission ofGeneric(String permission) {
        return ofGeneric(permission, true);
    }

    static Permission ofGeneric(String permission, boolean requiresOpByDefault) {
        return new Dynamic(permission, requiresOpByDefault);
    }

    class Dynamic implements Permission {

        private final String permission;
        private final boolean requiresOpByDefault;

        public Dynamic(String permission, boolean requiresOpByDefault) {
            this.permission = permission;
            this.requiresOpByDefault = requiresOpByDefault;
        }

        @Override
        public String permission() {
            return permission;
        }

        @Override
        public boolean requiresOpByDefault() {
            return requiresOpByDefault;
        }
    }

}
