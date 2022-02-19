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

package com.discordsrv.common.storage;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.storage.impl.sql.file.H2Storage;
import com.discordsrv.common.storage.impl.sql.hikari.MySQLStorage;

import java.util.function.Function;

public enum StorageType {

    H2(H2Storage::new, false),
    MYSQL(MySQLStorage::new, true);

    private final Function<DiscordSRV, Storage> storageFunction;
    private final boolean hikari;

    StorageType(Function<DiscordSRV, Storage> storageFunction, boolean hikari) {
        this.storageFunction = storageFunction;
        this.hikari = hikari;
    }

    public Function<DiscordSRV, Storage> storageFunction() {
        return storageFunction;
    }

    public boolean hikari() {
        return hikari;
    }
}
