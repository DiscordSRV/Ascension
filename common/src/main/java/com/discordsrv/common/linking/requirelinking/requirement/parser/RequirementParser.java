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

package com.discordsrv.common.linking.requirelinking.requirement.parser;

import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.linking.requirelinking.requirement.Requirement;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;

public class RequirementParser {

    private static RequirementParser INSTANCE;

    public static RequirementParser getInstance() {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new RequirementParser());
    }

    private RequirementParser() {}

    @SuppressWarnings("unchecked")
    public <T> BiFunction<UUID, Long, CompletableFuture<Boolean>> parse(String input, List<Requirement<?>> requirements) {
        List<Requirement<T>> reqs = new ArrayList<>(requirements.size());
        requirements.forEach(r -> reqs.add((Requirement<T>) r));

        Func func = parse(input, new AtomicInteger(0), reqs);
        return func::test;
    }

    private <T> Func parse(String input, AtomicInteger iterator, List<Requirement<T>> requirements) {
        StringBuilder functionNameBuffer = new StringBuilder();
        StringBuilder functionValueBuffer = new StringBuilder();
        boolean isFunctionValue = false;

        Func func = null;
        Operator operator = null;
        boolean operatorSecond = false;

        Function<String, RuntimeException> error = text -> {
            int i = iterator.get();
            return new IllegalArgumentException(text + "\n" + input + "\n" + StringUtils.leftPad("^", i));
        };

        char[] chars = input.toCharArray();
        int i;
        for (; (i = iterator.get()) < chars.length; iterator.incrementAndGet()) {
            char c = chars[i];
            if (c == '(' && functionNameBuffer.length() == 0) {
                iterator.incrementAndGet();
                Func function = parse(input, iterator, requirements);
                if (function == null) {
                    throw error.apply("Empty brackets");
                }

                if (func != null) {
                    if (operator == null) {
                        throw error.apply("No operator");
                    }

                    func = operator.function.apply(func, function);
                    operator = null;
                } else {
                    func = function;
                }
                continue;
            }

            if (c == ')' && functionNameBuffer.length() == 0) {
                return func;
            }
            if (c == '(' && functionNameBuffer.length() > 0) {
                if (isFunctionValue) {
                    throw error.apply("Opening bracket inside function value");
                }

                isFunctionValue = true;
                continue;
            }
            if (c == ')' && functionNameBuffer.length() > 0) {
                String functionName = functionNameBuffer.toString();
                String value = functionValueBuffer.toString();

                for (Requirement<T> requirement : requirements) {
                    if (requirement.name().equalsIgnoreCase(functionName)) {
                        T requirementValue = requirement.parse(value);
                        if (requirementValue == null) {
                            throw error.apply("Unacceptable function value for " + functionName);
                        }

                        Func function = (player, user) -> requirement.isMet(requirementValue, player, user);
                        if (func != null) {
                            if (operator == null) {
                                throw error.apply("No operator");
                            }

                            func = operator.function.apply(func, function);
                            operator = null;
                        } else {
                            func = function;
                        }

                        functionNameBuffer.setLength(0);
                        functionValueBuffer.setLength(0);
                        isFunctionValue = false;
                        break;
                    }
                }
                if (functionNameBuffer.length() != 0) {
                    throw error.apply("Unknown function: " + functionName);
                }
                continue;
            }

            if (operator != null && !operatorSecond && c == operator.character) {
                operatorSecond = true;
                continue;
            } else if (operator == null && functionNameBuffer.length() == 0) {
                boolean found = false;
                for (Operator value : Operator.values()) {
                    if (value.character == c) {
                        if (func == null) {
                            throw error.apply("No condition");
                        }
                        operator = value;
                        operatorSecond = false;
                        found = true;
                        break;
                    }
                }
                if (found) {
                    continue;
                }
            }

            if (operator != null && !operatorSecond) {
                throw error.apply("Operators must be exactly two of the same character");
            }

            if (!Character.isSpaceChar(c)) {
                if (isFunctionValue) {
                    functionValueBuffer.append(c);
                } else {
                    functionNameBuffer.append(c);
                }
            }
        }

        if (operator != null) {
            throw error.apply("Dangling operator");
        }
        return func;
    }

    @FunctionalInterface
    private interface Func {
        CompletableFuture<Boolean> test(UUID player, long user);
    }

    private enum Operator {

        AND('&', (one, two) -> apply(one, two, (o, t) -> o && t)),
        OR('|', (one, two) -> apply(one, two, (o, t) -> o || t));

        private final char character;
        private final BiFunction<Func, Func, Func> function;

        Operator(char character, BiFunction<Func, Func, Func> function) {
            this.character = character;
            this.function = function;
        }

        private static Func apply(Func one, Func two, BiFunction<Boolean, Boolean, Boolean> function) {
            return (player, user) -> CompletableFutureUtil.combine(one.test(player, user), two.test(player, user))
                    .thenApply(bools -> function.apply(bools.get(0), bools.get(1)));
        }
    }
}
