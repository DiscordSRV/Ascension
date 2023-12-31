/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.placeholder;

import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.common.MockDiscordSRV;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlaceholderServiceTest {

    private final PlaceholderService service = MockDiscordSRV.INSTANCE.placeholderService();

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

    @Test
    public void orPrimaryTest() {
        assertEquals("a", service.replacePlaceholders("%static_field|static_method%", PlaceholderContext.class));
    }

    @Test
    public void orSecondaryTest() {
        assertEquals("b", service.replacePlaceholders("%invalid|static_method%", PlaceholderContext.class));
    }

    @Test
    public void orEmptyTest() {
        assertEquals("b", service.replacePlaceholders("%empty|static_method%", PlaceholderContext.class));
    }

    @Test
    public void prefixFailTest() {
        assertEquals("%placeholder%", service.replacePlaceholders("%placeholder%", PrefixContext.class));
    }

    @Test
    public void prefixTest() {
        assertEquals("value", service.replacePlaceholders("%prefix_placeholder%", PrefixContext.class));
    }

    @Test
    public void prefixInheritFailTest() {
        assertEquals("%prefix_noprefix%", service.replacePlaceholders("%prefix_noprefix%", PrefixInheritanceContext.class));
    }

    @Test
    public void prefixInheritTest() {
        assertEquals("value", service.replacePlaceholders("%noprefix%", PrefixInheritanceContext.class));
    }

    public static class PlaceholderContext {

        @Placeholder("static_field")
        public static String STATIC_FIELD = "a";

        @Placeholder("static_method")
        public static String staticMethod() {
            return "b";
        }

        @Placeholder("empty")
        public static String EMPTY = "";

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

    @PlaceholderPrefix("prefix_")
    public static class PrefixContext {

        @Placeholder("placeholder")
        public static String placeholder = "value";
    }
    public static class PrefixInheritanceContext extends PrefixContext {

        @Placeholder("noprefix")
        public static String noPrefix = "value";
    }
}
