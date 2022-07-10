/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.discordsrv.api.discord.entity.command;

import com.discordsrv.api.discord.entity.JDAEntity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class CommandOption implements JDAEntity<OptionData> {

    /**
     * Creates a new command option builder.
     *
     * @param type the type of the command option
     * @param name the name of the command option
     * @param description the description of the command option
     * @return the new command option builder
     */
    @NotNull
    public static Builder builder(@NotNull Type type, @NotNull String name, @NotNull String description) {
        return new Builder(type, name, description);
    }

    private final Type type;
    private final String name;
    private final String description;
    private final Map<String, Object> choices;

    public CommandOption(Type type, String name, String description, Map<String, Object> choices) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.choices = choices;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    @NotNull
    @Unmodifiable
    public Map<String, Object> getChoices() {
        return Collections.unmodifiableMap(choices);
    }

    @Override
    public OptionData asJDA() {
        OptionData data = new OptionData(type.asJDA(), name, description);
        for (Map.Entry<String, Object> entry : choices.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                data.addChoice(key, (String) value);
            } else if (value instanceof Integer) {
                data.addChoice(key, (Integer) value);
            } else if (value instanceof Double) {
                data.addChoice(key, (Double) value);
            } else {
                throw new IllegalStateException("Not a String, Integer or Double choice value");
            }
        }
        return data;
    }

    public static class Builder {

        private final Type type;
        private final String name;
        private final String description;
        private final Map<String, Object> choices = new LinkedHashMap<>();

        private Builder(Type type, String name, String description) {
            this.type = type;
            this.name = name;
            this.description = description;
        }

        /**
         * Adds a String choice, type must be {@link Type#STRING}.
         *
         * @param name the name of the choice, this will be returned via the event
         * @param stringValue the choice
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder addChoice(String name, String stringValue) {
            if (type != Type.DOUBLE) {
                throw new IllegalStateException("Must be of type STRING");
            }
            this.choices.put(name, stringValue);
            return this;
        }

        /**
         * Adds a String choice, type must be {@link Type#INTEGER}.
         *
         * @param name the name of the choice, this will be returned via the event
         * @param integerValue the choice
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder addChoice(String name, int integerValue) {
            if (type != Type.INTEGER) {
                throw new IllegalStateException("Must be of type INTEGER");
            }
            this.choices.put(name, integerValue);
            return this;
        }

        /**
         * Adds a String choice, type must be {@link Type#DOUBLE}.
         *
         * @param name the name of the choice, this will be returned via the event
         * @param doubleValue the choice
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder addChoice(String name, double doubleValue) {
            if (type != Type.DOUBLE) {
                throw new IllegalStateException("Must be of type DOUBLE");
            }
            this.choices.put(name, doubleValue);
            return this;
        }

        @NotNull
        public CommandOption build() {
            return new CommandOption(type, name, description, choices);
        }
    }

    public enum Type implements JDAEntity<OptionType> {
        STRING(OptionType.STRING),
        INTEGER(OptionType.INTEGER),
        DOUBLE(OptionType.NUMBER),
        BOOLEAN(OptionType.BOOLEAN),

        USER(OptionType.USER),
        CHANNEL(OptionType.CHANNEL),
        ROLE(OptionType.ROLE),
        MENTIONABLE(OptionType.MENTIONABLE),

        ATTACHMENT(OptionType.ATTACHMENT);

        private final OptionType optionType;

        Type(OptionType optionType) {
            this.optionType = optionType;
        }

        public boolean isSupportsChoices() {
            return optionType.canSupportChoices();
        }

        @Override
        public OptionType asJDA() {
            return optionType;
        }
    }
}
