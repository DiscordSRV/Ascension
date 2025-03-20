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

package com.discordsrv.common.command.game;

import com.discordsrv.api.task.Task;
import com.discordsrv.common.command.game.abstraction.GameCommandExecutionHelper;
import com.discordsrv.common.config.main.generic.GameCommandExecutionConditionConfig;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameCommandFilterTest {

    private static final long USER_ID = 1337L;
    private static final long USER_ID2 = 1234L;
    private final ExecutionHelper helper = new ExecutionHelper();

    @Test
    public void test1() {
        assertTrue(GameCommandExecutionConditionConfig.isCommandMatch("test", "test", false, helper));
    }

    @Test
    public void test2() {
        assertFalse(GameCommandExecutionConditionConfig.isCommandMatch("test", "tester", false, helper));
    }

    @Test
    public void testNoHelper1() {
        assertTrue(GameCommandExecutionConditionConfig.isCommandMatch("test", "test", false, null));
    }

    @Test
    public void testNoHelper2() {
        assertFalse(GameCommandExecutionConditionConfig.isCommandMatch("test", "tester", false, null));
    }

    @Test
    public void argumentTest() {
        assertTrue(GameCommandExecutionConditionConfig.isCommandMatch("test arg", "test arg", false, helper));
    }

    @Test
    public void suggestTest() {
        assertTrue(GameCommandExecutionConditionConfig.isCommandMatch("test arg", "test", true, helper));
    }

    @Test
    public void suggestLeadUpArgumentTest() {
        assertTrue(GameCommandExecutionConditionConfig.isCommandMatch("test arg arg", "test arg", true, helper));
    }

    @Test
    public void suggestLeadUpArgumentFailTest() {
        assertFalse(GameCommandExecutionConditionConfig.isCommandMatch("test arg arg", "test arg2", true, helper));
    }

    @Test
    public void extraTest() {
        assertTrue(
                GameCommandExecutionConditionConfig.isCommandMatch("test arg", "test arg extra arguments after that", false, helper));
    }

    @Test
    public void argumentOverflowTest1() {
        assertFalse(
                GameCommandExecutionConditionConfig.isCommandMatch("test arg", "test argument", false, helper));
    }

    @Test
    public void sameCommandTest1() {
        assertFalse(GameCommandExecutionConditionConfig.isCommandMatch("plugin1:test", "test", false, helper));
    }

    @Test
    public void sameCommandTest2() {
        assertTrue(GameCommandExecutionConditionConfig.isCommandMatch("plugin2:test", "test", false, helper));
    }

    @Test
    public void regexTest1() {
        assertTrue(GameCommandExecutionConditionConfig.isCommandMatch("/test/", "test", false, helper));
    }

    @Test
    public void regexTest2() {
        assertFalse(GameCommandExecutionConditionConfig.isCommandMatch("/test/", "test extra", false, helper));
    }

    @Test
    public void regexTest3() {
        assertTrue(
                GameCommandExecutionConditionConfig.isCommandMatch("/test( argument)?/", "test argument", false, helper));
    }

    @Test
    public void regexTest4() {
        assertFalse(
                GameCommandExecutionConditionConfig.isCommandMatch("/test( argument)?/", "test fail", false, helper));
    }

    @Test
    public void regexTest5() {
        assertTrue(
                GameCommandExecutionConditionConfig.isCommandMatch("/test( argument)?/", "test", true, helper));
    }

    @Test
    public void regexMissTest1() {
        assertFalse(
                GameCommandExecutionConditionConfig.isCommandMatch("/test argument/", "test", false, helper));
    }

    @Test
    public void regexMissTest2() {
        assertFalse(
                GameCommandExecutionConditionConfig.isCommandMatch("/test argument/", "argument", false, helper));
    }

    @Test
    public void invalidRegexTest() {
        assertFalse(GameCommandExecutionConditionConfig.isCommandMatch("/test", "test", true, helper));
    }

    // Config test: blacklist

    @Test
    public void configEmptyBlacklist() {
        GameCommandExecutionConditionConfig config = new GameCommandExecutionConditionConfig();
        config.blacklist = true;
        config.commands.clear();
        config.roleAndUserIds.add(USER_ID);

        assertTrue(config.isAcceptableCommand(Collections.emptyList(), USER_ID, "test", false, helper));
    }

    @Test
    public void configBlacklistFail() {
        GameCommandExecutionConditionConfig config = new GameCommandExecutionConditionConfig();
        config.blacklist = true;
        config.commands.add("test");
        config.roleAndUserIds.add(USER_ID);

        assertFalse(config.isAcceptableCommand(Collections.emptyList(), USER_ID, "test", false, helper));
    }

    @Test
    public void configBlacklistPass() {
        GameCommandExecutionConditionConfig config = new GameCommandExecutionConditionConfig();
        config.blacklist = true;
        config.commands.add("tester");
        config.roleAndUserIds.add(USER_ID);

        assertTrue(config.isAcceptableCommand(Collections.emptyList(), USER_ID, "test", false, helper));
    }

    @Test
    public void configEmptyWhitelist() {
        GameCommandExecutionConditionConfig config = new GameCommandExecutionConditionConfig();
        config.blacklist = false;
        config.commands.clear();
        config.roleAndUserIds.add(USER_ID);

        assertFalse(config.isAcceptableCommand(Collections.emptyList(), USER_ID, "test", false, helper));
    }

    @Test
    public void configWhitelistFail() {
        GameCommandExecutionConditionConfig config = new GameCommandExecutionConditionConfig();
        config.blacklist = false;
        config.commands.add("tester");
        config.roleAndUserIds.add(USER_ID);

        assertFalse(config.isAcceptableCommand(Collections.emptyList(), USER_ID, "test", false, helper));
    }

    @Test
    public void configWhitelistPass() {
        GameCommandExecutionConditionConfig config = new GameCommandExecutionConditionConfig();
        config.blacklist = false;
        config.commands.add("test");
        config.roleAndUserIds.add(USER_ID);

        assertTrue(config.isAcceptableCommand(Collections.emptyList(), USER_ID, "test", false, helper));
    }

    @Test
    public void configWhitelistPassRoleId() {
        GameCommandExecutionConditionConfig config = new GameCommandExecutionConditionConfig();
        config.blacklist = false;
        config.commands.add("test");
        config.roleAndUserIds.add(USER_ID);

        assertTrue(config.isAcceptableCommand(Collections.singletonList(USER_ID), USER_ID2, "test", false, helper));
    }

    @Test
    public void configMiss() {
        GameCommandExecutionConditionConfig config = new GameCommandExecutionConditionConfig();
        config.blacklist = true;
        config.commands.clear();
        config.roleAndUserIds.clear();

        assertFalse(config.isAcceptableCommand(Collections.emptyList(), USER_ID, "test", false, helper));
    }

    @Test
    public void configMiss2() {
        GameCommandExecutionConditionConfig config = new GameCommandExecutionConditionConfig();
        config.blacklist = true;
        config.commands.clear();
        config.roleAndUserIds.clear();
        config.roleAndUserIds.add(USER_ID2);

        assertFalse(config.isAcceptableCommand(Collections.emptyList(), USER_ID, "test", false, helper));
    }

    public static class ExecutionHelper implements GameCommandExecutionHelper {

        private final List<String> TEST1_USED = Collections.singletonList("plugin1:test");
        private final List<String> TEST1 = Arrays.asList("test", "plugin1:test");
        private final List<String> TEST2 = Arrays.asList("test", "plugin2:test");
        private final List<String> TESTER = Arrays.asList("tester", "plugin2:tester");

        @Override
        public Task<List<String>> suggestCommands(List<String> parts) {
            return null;
        }

        @Override
        public List<String> getAliases(String command) {
            if (TEST1_USED.contains(command)) {
                return TEST1;
            } else if (TEST2.contains(command)) {
                return TEST2;
            } else if (TESTER.contains(command)) {
                return TESTER;
            }
            return Collections.emptyList();
        }

        @Override
        public boolean isSameCommand(String command1, String command2) {
            return getAliases(command1) == getAliases(command2);
        }
    }
}
