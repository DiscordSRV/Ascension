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

package com.discordsrv.common.core.debug.file;

public interface DebugFile {

    String content();

    default Named withName(String name) {
        return new Named(name, 0, this);
    }
    default Named withName(String name, int order) {
        return new Named(name, order, this);
    }

    class Named implements DebugFile {

        private final String fileName;
        private final int order;
        private final DebugFile file;

        public Named(String fileName, int order, DebugFile file) {
            this.fileName = fileName;
            this.order = order;
            this.file = file;
        }

        public int order() {
            return order;
        }

        public String fileName() {
            return fileName;
        }

        @Override
        public String content() {
            return file.content();
        }
    }
}
