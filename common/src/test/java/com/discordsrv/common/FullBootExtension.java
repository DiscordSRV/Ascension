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

package com.discordsrv.common;

import com.discordsrv.api.DiscordSRVApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class FullBootExtension implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {

    public static String BOT_TOKEN = System.getenv("DISCORDSRV_AUTOTEST_BOT_TOKEN");
    public static String TEXT_CHANNEL_ID = System.getenv("DISCORDSRV_AUTOTEST_CHANNEL_ID");
    public static String FORUM_CHANNEL_ID = System.getenv("DISCORDSRV_AUTOTEST_FORUM_ID");
    public static String VOICE_CHANNEL_ID = System.getenv("DISCORDSRV_AUTOTEST_VOICE_ID");

    public static boolean started = false;

    @Override
    public void beforeAll(ExtensionContext context) {
        Assumptions.assumeTrue(BOT_TOKEN != null, "Automated testing bot token");
        Assumptions.assumeTrue(TEXT_CHANNEL_ID != null, "Automated testing text channel id");
        Assumptions.assumeTrue(FORUM_CHANNEL_ID != null, "Automated testing forum channel id");
        Assumptions.assumeTrue(VOICE_CHANNEL_ID != null, "Automated testing voice channel id");

        if (started) return;
        started = true;
        context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL).put("Full boot extension", this);

        try {
            System.out.println("Enabling...");
            MockDiscordSRV.getInstance().enable();
            MockDiscordSRV.getInstance().waitForStatus(DiscordSRVApi.Status.CONNECTED);
            System.out.println("Enabled successfully");
        } catch (Throwable e) {
            Assertions.fail(e);
        }
    }

    @Override
    public void close() {
        System.out.println("Disabling...");
        MockDiscordSRV.getInstance().disable();
        System.out.println("Disabled successfully");
    }
}
