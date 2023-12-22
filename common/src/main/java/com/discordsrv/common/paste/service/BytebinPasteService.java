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

package com.discordsrv.common.paste.service;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.paste.Paste;
import com.discordsrv.common.paste.PasteService;
import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.*;

public class BytebinPasteService implements PasteService {

    private final DiscordSRV discordSRV;
    private final String bytebinUrl;

    public BytebinPasteService(DiscordSRV discordSRV, String bytebinUrl) {
        this.discordSRV = discordSRV;
        this.bytebinUrl = bytebinUrl;
    }

    @Override
    public Paste uploadFile(byte[] fileContent) throws Throwable {
        Request request = new Request.Builder()
                .url(bytebinUrl + "/post")
                //.header("Content-Encoding", "gzip")
                .post(RequestBody.create(fileContent, MediaType.get("application/octet-stream")))
                .build();

        try (Response response = discordSRV.httpClient().newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return null;
            }

            JsonNode responseNode = discordSRV.json().readTree(responseBody.string());
            String key = responseNode.get("key").asText();
            return new Paste(key, bytebinUrl + "/" + key, null);
        }
    }
}
