package com.discordsrv.common.config;

import com.discordsrv.common.FullBootExtension;
import com.discordsrv.common.MockDiscordSRV;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(FullBootExtension.class)
public class ConfigsLoadOnEnableTest {

    @Test
    public void configsLoaded() {
        Assertions.assertTrue(MockDiscordSRV.INSTANCE.configLoaded, "Config loaded");
        Assertions.assertTrue(MockDiscordSRV.INSTANCE.connectionConfigLoaded, "Connection config loaded");
        Assertions.assertTrue(MockDiscordSRV.INSTANCE.messagesConfigLoaded, "Messages config loaded");
    }
}
