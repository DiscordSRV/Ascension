/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.MockDiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.linking.RequiredLinkingConfig;
import com.discordsrv.common.feature.linking.requirelinking.RequiredLinkingModule;
import com.discordsrv.common.feature.linking.requirelinking.requirement.RequirementType;
import com.discordsrv.common.feature.linking.requirelinking.requirement.parser.ParsedRequirements;
import com.discordsrv.common.feature.linking.requirelinking.requirement.parser.RequirementParser;
import com.discordsrv.common.helper.Someone;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class RequirementTypeParserTest {

    private final RequirementParser requirementParser = RequirementParser.getInstance();
    private final RequiredLinkingModule<?> module = new RequiredLinkingModule<DiscordSRV>(MockDiscordSRV.getInstance()) {
        @Override
        public RequiredLinkingConfig config() {
            return null;
        }

        @Override
        public void reload() {}

        @Override
        public List<ParsedRequirements> getAllActiveRequirements() {
            return Collections.emptyList();
        }

        @Override
        public void recheck(IPlayer player) {}
    };
    private final List<RequirementType<?>> requirementTypes = Arrays.asList(
            new RequirementType<Boolean>(module) {
                @Override
                public String name() {
                    return "F";
                }

                @Override
                public Boolean parse(String input) {
                    return Boolean.parseBoolean(input);
                }

                @Override
                public CompletableFuture<Boolean> isMet(Boolean value, Someone.Resolved someone) {
                    return CompletableFuture.completedFuture(value);
                }
            },
            new RequirementType<Object>(module) {
                @Override
                public String name() {
                    return "AlwaysError";
                }

                @Override
                public Object parse(String input) {
                    return null;
                }

                @Override
                public CompletableFuture<Boolean> isMet(Object value, Someone.Resolved someone) {
                    return null;
                }
            }
    );

    private boolean parse(String input) {
        return requirementParser.parse(input, requirementTypes)
                .predicate()
                .apply(Someone.of(UUID.randomUUID(), 0L))
                .join();
    }

    @Test
    public void differentCase() {
        assertFalse(parse("f(false) || F(false)"));
    }

    @Test
    public void negate() {
        assertTrue(parse("!F(false)"));
    }

    @Test
    public void negateReverse() {
        assertFalse(parse("!F(true)"));
    }

    @Test
    public void doubleNegate() {
        assertTrue(parse("!!F(true)"));
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
    public void andNegate() {
        assertTrue(parse("F(true) && !F(false)"));
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

    @Test
    public void negateBeforeFunctionNameError() {
        assertExceptionMessageStartsWith("Negation must be before function name", () -> parse("F!(false)"));
    }

}
