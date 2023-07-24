package com.discordsrv.common.player.provider;

import com.discordsrv.common.FullBootExtension;
import com.discordsrv.common.MockDiscordSRV;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(FullBootExtension.class)
public class PlayerProviderBootTest {

    @Test
    public void subscribed() {
        Assertions.assertTrue(MockDiscordSRV.INSTANCE.playerProviderSubscribed, "Player provider subscribed");
    }
}
