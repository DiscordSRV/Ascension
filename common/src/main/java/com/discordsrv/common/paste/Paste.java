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

package com.discordsrv.common.paste;

public class Paste {

    private final String id;
    private final String url;
    private final byte[] decryptionKey;

    public Paste(String id, String url, byte[] decryptionKey) {
        this.id = id;
        this.url = url;
        this.decryptionKey = decryptionKey;
    }

    public String id() {
        return id;
    }

    public String url() {
        return url;
    }

    public byte[] decryptionKey() {
        return decryptionKey;
    }

    public Paste withDecryptionKey(byte[] decryptionKey) {
        return new Paste(id, url, decryptionKey);
    }
}
