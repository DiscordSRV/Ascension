package com.discordsrv.api.module.type;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface NicknameModule {

    CompletableFuture<String> getNickname(UUID playerUUID);
    CompletableFuture<Void> setNickname(UUID playerUUID, String nickname);

}
