package com.discordsrv.common.command.game;

import com.discordsrv.common.config.main.generic.GameCommandFilterConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GameCommandFilterTest {

    private final ExecutionHelper helper = new ExecutionHelper();

    @Test
    public void test1() {
        Assertions.assertTrue(GameCommandFilterConfig.isCommandMatch("test", "test", false, helper));
    }

    @Test
    public void test2() {
        Assertions.assertFalse(GameCommandFilterConfig.isCommandMatch("test", "tester", false, helper));
    }

    @Test
    public void argumentTest() {
        Assertions.assertTrue(GameCommandFilterConfig.isCommandMatch("test arg", "test arg", false, helper));
    }

    @Test
    public void suggestTest() {
        Assertions.assertTrue(GameCommandFilterConfig.isCommandMatch("test arg", "test", true, helper));
    }

    @Test
    public void extraTest() {
        Assertions.assertTrue(GameCommandFilterConfig.isCommandMatch("test arg", "test arg extra arguments after that", false, helper));
    }

    @Test
    public void argumentOverflowTest1() {
        Assertions.assertFalse(GameCommandFilterConfig.isCommandMatch("test arg", "test argument", false, helper));
    }

    @Test
    public void sameCommandTest1() {
        Assertions.assertFalse(GameCommandFilterConfig.isCommandMatch("plugin1:test", "test", false, helper));
    }

    @Test
    public void sameCommandTest2() {
        Assertions.assertTrue(GameCommandFilterConfig.isCommandMatch("plugin2:test", "test", false, helper));
    }

    @Test
    public void regexTest1() {
        Assertions.assertTrue(GameCommandFilterConfig.isCommandMatch("/test/", "test", false, helper));
    }

    @Test
    public void regexTest2() {
        Assertions.assertFalse(GameCommandFilterConfig.isCommandMatch("/test/", "test extra", false, helper));
    }

    @Test
    public void regexTest3() {
        Assertions.assertTrue(GameCommandFilterConfig.isCommandMatch("/test( argument)?/", "test argument", false, helper));
    }

    @Test
    public void regexTest4() {
        Assertions.assertFalse(GameCommandFilterConfig.isCommandMatch("/test( argument)?/", "test fail", false, helper));
    }

    @Test
    public void regexTest5() {
        Assertions.assertTrue(GameCommandFilterConfig.isCommandMatch("/test( argument)?/", "test", true, helper));
    }

    public static class ExecutionHelper implements GameCommandExecutionHelper {

        private final List<String> TEST1_USED = Collections.singletonList("plugin1:test");
        private final List<String> TEST1 = Arrays.asList("test", "plugin1:test");
        private final List<String> TEST2 = Arrays.asList("test", "plugin2:test");
        private final List<String> TESTER = Arrays.asList("tester", "plugin2:tester");

        @Override
        public CompletableFuture<List<String>> suggestCommands(List<String> parts) {
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
