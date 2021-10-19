package com.discordsrv.common.placeholder;

import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.common.MockDiscordSRV;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlaceholderServiceTest {

    private PlaceholderService service = MockDiscordSRV.INSTANCE.placeholderService();

    @Test
    public void staticFieldTest() {
        assertEquals("a", service.replacePlaceholders("%static_field%", PlaceholderContext.class));
    }

    @Test
    public void staticMethodTest() {
        assertEquals("b", service.replacePlaceholders("%static_method%", PlaceholderContext.class));
    }

    @Test
    public void objectFieldTest() {
        assertEquals("c", service.replacePlaceholders("%object_field%", new PlaceholderContext()));
    }

    @Test
    public void objectMethodTest() {
        assertEquals("d", service.replacePlaceholders("%object_method%", new PlaceholderContext()));
    }

    @Test
    public void staticMethodWithContextTest() {
        assertEquals("e", service.replacePlaceholders("%static_method_with_context%", PlaceholderContext.class, "e"));
    }

    public static class PlaceholderContext {

        @Placeholder("static_field")
        public static String STATIC_FIELD = "a";

        @Placeholder("static_method")
        public static String staticMethod() {
            return "b";
        }

        @Placeholder("object_field")
        public String localField = "c";

        @Placeholder("object_method")
        public String objectMethod() {
            return "d";
        }

        @Placeholder("static_method_with_context")
        public static String objectMethodWithContext(String output) {
            return output;
        }
    }
}
