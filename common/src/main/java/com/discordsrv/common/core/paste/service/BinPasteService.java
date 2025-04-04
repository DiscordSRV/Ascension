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

package com.discordsrv.common.core.paste.service;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.paste.Paste;
import com.discordsrv.common.core.paste.PasteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class BinPasteService implements PasteService {

    private final DiscordSRV discordSRV;
    private final String binUrl;

    public BinPasteService(DiscordSRV discordSRV, String binUrl) {
        this.discordSRV = discordSRV;
        this.binUrl = binUrl;
    }

    @Override
    public Paste uploadFile(byte[] fileContent) throws IOException {
        ObjectNode json = discordSRV.json().createObjectNode();
        ArrayNode files = json.putArray("files");
        ObjectNode file = files.addObject();

        // "File name must be divisible by 16" not that I care about the file name...
        file.put("name", new String(Base64.getEncoder().encode(new byte[16])));
        file.put("content", new String(fileContent, StandardCharsets.UTF_8));

        Request request = new Request.Builder()
                .url(binUrl + "/v1/post")
                .post(RequestBody.create(json.toString(), MediaType.get("application/json")))
                .build();

        try (Response response = discordSRV.httpClient().newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return null;
            }

            JsonNode responseNode = discordSRV.json().readTree(responseBody.string());
            String binId = responseNode.get("bin").asText();
            return new Paste(binId, binUrl + "/v1/" + binId + ".json", null);
        }
    }

}
