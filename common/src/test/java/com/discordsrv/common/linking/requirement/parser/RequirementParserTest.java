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

package com.discordsrv.common.linking.requirement.parser;

import com.discordsrv.common.linking.requirelinking.requirement.Requirement;
import com.discordsrv.common.linking.requirelinking.requirement.parser.RequirementParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class RequirementParserTest {

    private final RequirementParser requirementParser = RequirementParser.getInstance();
    private final List<Requirement<?>> requirements = Arrays.asList(
            new Requirement<Boolean>() {
                @Override
                public String name() {
                    return "F";
                }

                @Override
                public Boolean parse(String input) {
                    return Boolean.parseBoolean(input);
                }

                @Override
                public CompletableFuture<Boolean> isMet(Boolean value, UUID player, long userId) {
                    return CompletableFuture.completedFuture(value);
                }
            },
            new Requirement<Object>() {
                @Override
                public String name() {
                    return "AlwaysError";
                }

                @Override
                public Object parse(String input) {
                    return null;
                }

                @Override
                public CompletableFuture<Boolean> isMet(Object value, UUID player, long userId) {
                    return null;
                }
            }
    );

    private boolean parse(String input) {
        return requirementParser.parse(input, requirements, new ArrayList<>()).apply(null, 0L).join();
    }

    @Test
    public void differentCase() {
        assertFalse(parse("f(false) || F(false)"));
    }

    @Test
    public void orFail() {
        assertFalse(parse("F(false) || F(false)"));
    }

    @Test
    public void orPass() {
        assertTrue(parse("F(true) || F(false)"));
    }

    @Test
    public void andFail() {
        assertFalse(parse("F(true) && F(false)"));
    }

    @Test
    public void andPass() {
        assertTrue(parse("F(true) && F(true)"));
    }

    @Test
    public void complexFail() {
        assertFalse(parse("F(true) && (F(false) && F(true))"));
    }

    @Test
    public void complexPass() {
        assertTrue(parse("F(true) && (F(false) || F(true))"));
    }

    private void assertExceptionMessageStartsWith(String exceptionMessage, Executable executable) {
        try {
            executable.execute();
        } catch (Throwable e) {
            if (!e.getMessage().startsWith(exceptionMessage)) {
                fail("Exception message did not start with: " + exceptionMessage + " Actually: " + e.getMessage());
            }
        }
    }

    @Test
    public void noConditionError() {
        assertExceptionMessageStartsWith("No condition", () -> parse("&&"));
    }

    @Test
    public void operatorLengthError() {
        assertExceptionMessageStartsWith("Operators must be exactly two of the same character", () -> parse("F(true) & F(true)"));
    }

    @Test
    public void danglingOperatorError() {
        assertExceptionMessageStartsWith("Dangling operator", () -> parse("F(true) &&"));
    }

    @Test
    public void emptyBracketsError() {
        assertExceptionMessageStartsWith("Empty brackets", () -> parse("()"));
    }

    @Test
    public void unacceptableValueError() {
        assertExceptionMessageStartsWith("Unacceptable function value for", () -> parse("AlwaysError()"));
    }

}
