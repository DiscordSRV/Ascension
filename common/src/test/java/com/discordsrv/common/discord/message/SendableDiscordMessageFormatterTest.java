/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.discord.message;

import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.common.MockDiscordSRV;
import com.discordsrv.common.placeholder.PlaceholderServiceTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SendableDiscordMessageFormatterTest {

    private final PlaceholderService service = MockDiscordSRV.getInstance().placeholderService();

    @Test
    public void placeholderTest() {
        SendableDiscordMessage message = SendableDiscordMessage.builder()
                .setContent("%static_field%")
                .toFormatter()
                .applyPlaceholderService(service)
                .addContext(PlaceholderServiceTest.BasicContext.class)
                .build();

        String content = message.getContent();
        Assertions.assertNotNull(content);
        Assertions.assertTrue(content.contains(PlaceholderServiceTest.BasicContext.STATIC_FIELD));
    }

    @Test
    public void unsafeContentTest() {
        SendableDiscordMessage message = SendableDiscordMessage.builder()
                .setContent("%unsafe_input%")
                .toFormatter()
                .applyPlaceholderService(service)
                .addContext(PlaceholderServiceTest.BasicContext.class)
                .build();

        String content = message.getContent();
        Assertions.assertNotNull(content);
        Assertions.assertEquals(content, PlaceholderServiceTest.BasicContext.UNSAFE_INPUT);
    }
}
