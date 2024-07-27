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

package com.discordsrv.common.feature.linking.requirelinking.requirement.parser;

import com.discordsrv.common.feature.linking.requirelinking.requirement.Requirement;
import com.discordsrv.common.feature.linking.requirelinking.requirement.RequirementType;
import com.discordsrv.common.helper.Someone;
import com.discordsrv.common.util.CompletableFutureUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
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
    public <T> ParsedRequirements parse(
            String input,
            List<RequirementType<?>> availableRequirementTypes
    ) {
        List<RequirementType<T>> reqs = new ArrayList<>(availableRequirementTypes.size());
        availableRequirementTypes.forEach(r -> reqs.add((RequirementType<T>) r));

        List<Requirement<?>> usedRequirements = new ArrayList<>();
        Func func = parse(input, new AtomicInteger(0), reqs, usedRequirements);
        return new ParsedRequirements(input, func::test, usedRequirements);
    }

    private <T> Func parse(
            String input,
            AtomicInteger iterator,
            List<RequirementType<T>> availableRequirementTypes,
            List<Requirement<?>> parsedRequirements
    ) {
        StringBuilder functionNameBuffer = new StringBuilder();
        StringBuilder functionValueBuffer = new StringBuilder();
        boolean isFunctionValue = false;

        Func func = null;
        Operator operator = null;
        boolean operatorSecond = false;
        boolean negated = false;

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
                Func function = parse(input, iterator, availableRequirementTypes, parsedRequirements);
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

                for (RequirementType<T> requirementType : availableRequirementTypes) {
                    if (requirementType.name().equalsIgnoreCase(functionName)) {
                        T requirementValue = requirementType.parse(value);
                        if (requirementValue == null) {
                            throw error.apply("Unacceptable function value for " + functionName);
                        }

                        boolean isNegated = negated;
                        negated = false;

                        parsedRequirements.add(new Requirement<>(requirementType, requirementValue, isNegated));

                        Func function = someone -> requirementType.isMet(requirementValue, someone)
                                .thenApply(val -> isNegated != val);
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

            if (Character.isSpaceChar(c)) {
                continue;
            }

            if (isFunctionValue) {
                functionValueBuffer.append(c);
            } else {
                if (c == '!') {
                    if (functionNameBuffer.length() > 0) {
                        throw error.apply("Negation must be before function name");
                    }

                    negated = !negated;
                    continue;
                }

                functionNameBuffer.append(c);
            }
        }

        if (operator != null) {
            throw error.apply("Dangling operator");
        }
        return func;
    }

    @FunctionalInterface
    private interface Func {
        CompletableFuture<Boolean> test(Someone.Resolved someone);
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
            return someone -> CompletableFutureUtil.combine(one.test(someone), two.test(someone))
                    .thenApply(bools -> function.apply(bools.get(0), bools.get(1)));
        }
    }
}
