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

package com.discordsrv.common.util;

import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.exception.MessageException;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public final class HttpUtil {

    private HttpUtil() {}

    public static ResponseBody checkIfResponseSuccessful(Request request, Response response) throws IOException {
        ResponseBody responseBody = response.body();
        if (responseBody == null || !response.isSuccessful()) {
            String responseString = responseBody == null
                                    ? "response body is null"
                                    : StringUtils.substring(responseBody.string(), 0, 500);
            throw new MessageException("Request to " + request.url().host() + " failed: " + response.code() + ": " + responseString);
        }
        return responseBody;
    }

    public static <T> Task<T> readJson(DiscordSRV discordSRV, Request request, Class<T> type) {
        return discordSRV.scheduler().supply(() -> {
            try (Response response = discordSRV.httpClient().newCall(request).execute()) {
                ResponseBody responseBody = checkIfResponseSuccessful(request, response);

                T result = discordSRV.json().readValue(responseBody.byteStream(), type);
                if (result == null) {
                    throw new MessageException("Response json cannot be parsed");
                }
                return result;
            }
        });
    }
}
