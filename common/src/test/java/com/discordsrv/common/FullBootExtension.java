package com.discordsrv.common;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.*;

public class FullBootExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    public static String BOT_TOKEN = System.getenv("DISCORDSRV_AUTOTEST_BOT_TOKEN");
    public static String TEST_CHANNEL_ID = System.getenv("DISCORDSRV_AUTOTEST_CHANNEL_ID");

    public boolean started = false;

    @Override
    public void beforeAll(ExtensionContext context) {
        Assumptions.assumeTrue(BOT_TOKEN != null, "Automated testing bot token");
        Assumptions.assumeTrue(TEST_CHANNEL_ID != null, "Automated testing channel id");

        if (started) return;
        started = true;

        try {
            System.out.println("Enabling...");
            MockDiscordSRV.INSTANCE.enable();
            System.out.println("Enabled successfully");
        } catch (Throwable e) {
            Assertions.fail(e);
        }
    }

    @Override
    public void close() {
        MockDiscordSRV.INSTANCE.disable();
    }
}
