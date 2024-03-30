package com.discordsrv.api.module.type;

import java.util.UUID;

public interface NicknameModule {

    String getNickname(UUID playerUUID);
    void setNickname(UUID playerUUID, String nickname);

}
