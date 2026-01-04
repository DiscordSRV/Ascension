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

package com.discordsrv.common.placeholder;

import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import com.discordsrv.api.placeholder.PlaceholderService;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.MockDiscordSRV;
import com.discordsrv.common.core.placeholder.PlaceholderServiceImpl;
import org.junit.jupiter.api.Test;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class PlaceholderServiceTest {

    private final PlaceholderService service = MockDiscordSRV.getInstance().placeholderService();

    @Test
    public void staticFieldTest() {
        assertEquals("StaticField", service.replacePlaceholders("%static_field%", BasicContext.class));
    }

    @Test
    public void staticMethodTest() {
        assertEquals("StaticMethod", service.replacePlaceholders("%static_method%", BasicContext.class));
    }

    @Test
    public void objectFieldTest() {
        assertEquals("ObjectField", service.replacePlaceholders("%object_field%", new BasicContext()));
    }

    @Test
    public void objectMethodTest() {
        assertEquals("ObjectMethod", service.replacePlaceholders("%object_method%", new BasicContext()));
    }

    @Test
    public void staticMethodWithContextTest() {
        assertEquals("PassedContext", service.replacePlaceholders("%required_context%", BasicContext.class, "PassedContext"));
    }

    @Test
    public void orPrimaryTest() {
        assertEquals("StaticField", service.replacePlaceholders("%static_field|static_method%", BasicContext.class));
    }

    @Test
    public void orSecondaryTest() {
        assertEquals("StaticMethod", service.replacePlaceholders("%invalid|static_method%", BasicContext.class));
    }

    @Test
    public void orEmptyTest() {
        assertEquals("StaticMethod", service.replacePlaceholders("%empty|static_method%", BasicContext.class));
    }

    @Test
    public void additionalContextTest() {
        assertEquals("StaticField", service.replacePlaceholders("%[additional_context]static_field%", new AdditionalContext()));
    }

    @Test
    public void infiniteLoopTest() {
        assertEquals(PlaceholderServiceImpl.INFINITE_LOOP_REPLACEMENT, service.replacePlaceholders("%infinite_loop%", new BasicContext()));
    }

    @Test
    public void taskSuccessTest() {
        assertEquals("completed", service.replacePlaceholders("%task_completed%", new BasicContext()));
    }

    @Test
    public void taskTimeoutTest() {
        assertEquals(PlaceholderServiceImpl.DATA_NOT_AVAILABLE_REPLACEMENT, service.replacePlaceholders("%task_never_ending%", new BasicContext()));
    }

    @Test
    public void taskFailTest() {
        assertEquals(PlaceholderServiceImpl.ERROR_REPLACEMENT, service.replacePlaceholders("%task_failed%", new BasicContext()));
    }

    @Test
    public void unsafeContentTest() {
        String replaced = service.replacePlaceholders("%unsafe_input%", BasicContext.class);

        assertNotNull(replaced);
        assertFalse(replaced.contains(BasicContext.STATIC_FIELD));
        assertEquals(BasicContext.UNSAFE_INPUT, replaced);
    }

    @Test
    public void reLookupTest() {
        PlaceholderServiceImpl placeholderService = new PlaceholderServiceImpl(MockDiscordSRV.getInstance());
        placeholderService.addReLookup(UserContext.class, "user");

        String replaced = placeholderService.replacePlaceholders("%a_user_name%", new BasicContext());

        assertEquals("UserName", replaced);
    }

    @Test
    public void resultMapperTest() {
        PlaceholderServiceImpl placeholderService = new PlaceholderServiceImpl(MockDiscordSRV.getInstance());

        assertEquals(String.valueOf(BasicContext.double4decimal), placeholderService.replacePlaceholders("%double_4decimal%", new BasicContext()));

        NumberFormat twoDecimalFormat = new DecimalFormat("0.##");
        placeholderService.addResultMapper(result -> {
            if (result instanceof Double) {
                return twoDecimalFormat.format(result);
            }
            return null;
        });
        assertEquals(twoDecimalFormat.format(BasicContext.double4decimal), placeholderService.replacePlaceholders("%double_4decimal%", new BasicContext()));
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

    public static class BasicContext {

        @Placeholder("static_field")
        public static String STATIC_FIELD = "StaticField";

        @Placeholder("static_method")
        public static String staticMethod() {
            return "StaticMethod";
        }

        @Placeholder("object_field")
        public String objectField = "ObjectField";

        @Placeholder("object_method")
        public String objectMethod() {
            return "ObjectMethod";
        }

        @Placeholder("required_context")
        public static String requiredContext(String output) {
            return output;
        }

        @Placeholder("empty_content")
        public static String EMPTY = "";

        @Placeholder("infinite_loop")
        public PlaceholderLookupResult infiniteLoop() {
            return PlaceholderLookupResult.newLookup("infinite_loop", Collections.singletonList(this));
        }

        @Placeholder("task_never_ending")
        public Task<String> neverEnding = new Task<>();

        @Placeholder("task_completed")
        public Task<String> completed = Task.completed("completed");

        @Placeholder("task_failed")
        public Task<String> failed = Task.failed(new Exception("failure"));

        @Placeholder("unsafe_input")
        public static String UNSAFE_INPUT = "unsafe input %empty%";

        @Placeholder("a_user")
        public UserContext aUser = new UserContext();

        @Placeholder("double_4decimal")
        public static double double4decimal = 1.2468;
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

    public static class AdditionalContext {

        @Placeholder("additional_context")
        public BasicContext additionalContext = new BasicContext();

    }

    @PlaceholderPrefix("user_")
    public static class UserContext {

        @Placeholder("name")
        public String name = "UserName";
    }
}
