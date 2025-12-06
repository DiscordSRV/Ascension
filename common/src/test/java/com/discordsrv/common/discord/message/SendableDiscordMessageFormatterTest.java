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
