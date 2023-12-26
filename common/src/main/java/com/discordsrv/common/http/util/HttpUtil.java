package com.discordsrv.common.http.util;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.exception.MessageException;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

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

    public static <T> CompletableFuture<T> readJson(DiscordSRV discordSRV, Request request, Class<T> type) {
        CompletableFuture<T> future = new CompletableFuture<>();
        discordSRV.scheduler().run(() -> {
            try (Response response = discordSRV.httpClient().newCall(request).execute()) {
                ResponseBody responseBody = checkIfResponseSuccessful(request, response);

                T result = discordSRV.json().readValue(responseBody.byteStream(), type);
                if (result == null) {
                    throw new MessageException("Response json cannot be parsed");
                }
                future.complete(result);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }
}
