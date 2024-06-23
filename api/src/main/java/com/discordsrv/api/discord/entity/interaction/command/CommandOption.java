/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.discord.entity.interaction.command;

import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.channel.DiscordChannelType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

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
    private final boolean required;
    private final boolean autoComplete;
    private final Set<DiscordChannelType> channelTypes;
    private final Number minValue;
    private final Number maxValue;

    public CommandOption(
            Type type,
            String name,
            String description,
            Map<String, Object> choices,
            boolean required,
            boolean autoComplete,
            Set<DiscordChannelType> channelTypes,
            Number minValue,
            Number maxValue
    ) {
        this.type = type;
        this.name = name;
        this.description = description;
        this.choices = choices;
        this.required = required;
        this.autoComplete = autoComplete;
        this.channelTypes = channelTypes;
        this.minValue = minValue;
        this.maxValue = maxValue;
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

    public boolean isRequired() {
        return required;
    }

    public boolean isAutoComplete() {
        return autoComplete;
    }

    @NotNull
    @Unmodifiable
    public Set<DiscordChannelType> getChannelTypes() {
        return channelTypes;
    }

    @Nullable
    public Number getMinValue() {
        return minValue;
    }

    @Nullable
    public Number getMaxValue() {
        return maxValue;
    }

    @Override
    public OptionData asJDA() {
        OptionData data = new OptionData(type.asJDA(), name, description)
                .setRequired(required)
                .setAutoComplete(autoComplete);
        if (type == Type.LONG) {
            Number min = getMinValue();
            if (min != null) {
                data.setMinValue(min.longValue());
            }
            Number max = getMaxValue();
            if (max != null) {
                data.setMinValue(max.longValue());
            }
        }
        if (type == Type.DOUBLE) {
            Number min = getMinValue();
            if (min != null) {
                data.setMinValue(min.doubleValue());
            }
            Number max = getMaxValue();
            if (max != null) {
                data.setMinValue(max.doubleValue());
            }
        }
        for (Map.Entry<String, Object> entry : choices.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                data.addChoice(key, (String) value);
            } else if (value instanceof Number && type == Type.LONG) {
                data.addChoice(key, ((Number) value).longValue());
            } else if (value instanceof Number && type == Type.DOUBLE) {
                data.addChoice(key, ((Number) value).doubleValue());
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
        private boolean required = false;
        private boolean autoComplete = false;
        private final Set<DiscordChannelType> channelTypes = new LinkedHashSet<>();
        private Number minValue;
        private Number maxValue;

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
            if (type != Type.STRING) {
                throw new IllegalStateException("Must be of type STRING to specify a text choice");
            }
            this.choices.put(name, stringValue);
            return this;
        }

        /**
         * Adds a String choice, type must be {@link Type#LONG} or {@link Type#DOUBLE}.
         *
         * @param name the name of the choice, this will be returned via the event
         * @param value the choice
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder addChoice(String name, Number value) {
            if (type != Type.LONG && type != Type.DOUBLE) {
                throw new IllegalStateException("Must be of type INTEGER or DOUBLE to specify a numeric choice");
            }
            this.choices.put(name, value);
            return this;
        }

        /**
         * Sets if this choice is required.
         *
         * @param required is this choice required
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder setRequired(boolean required) {
            this.required = required;
            return this;
        }

        /**
         * Sets if this option is auto completed. See {@link com.discordsrv.api.event.events.discord.interaction.command.DiscordCommandAutoCompleteInteractionEvent}.
         *
         * @param autoComplete is this choice auto completing
         * @return this builder, useful for chaining
         * @see com.discordsrv.api.event.events.discord.interaction.command.DiscordCommandAutoCompleteInteractionEvent
         */
        @NotNull
        public Builder setAutoComplete(boolean autoComplete) {
            this.autoComplete = autoComplete;
            return this;
        }

        /**
         * Sets the accepted channel types for this option. The type must be {@link Type#CHANNEL} or {@link Type#MENTIONABLE}.
         *
         * @param types the channel types
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder setChannelTypes(@NotNull DiscordChannelType... types) {
            if (type != Type.CHANNEL && type != Type.MENTIONABLE) {
                throw new IllegalStateException("Must be of type CHANNEL or MENTIONABLE to specify channel types");
            }
            this.channelTypes.clear();
            this.channelTypes.addAll(Arrays.asList(types));
            return this;
        }

        /**
         * Sets the minimum value for this command option. The type must be {@link Type#LONG} or {@link Type#DOUBLE}.
         * @param minValue the minimum value
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder setMinValue(@Nullable Number minValue) {
            if (type != Type.LONG && type != Type.DOUBLE) {
                throw new IllegalStateException("Must be of type INTEGER or DOUBLE to specify minimum value");
            }
            this.minValue = minValue;
            return this;
        }

        /**
         * Sets the maximum value for this command option. The type must be {@link Type#LONG} or {@link Type#DOUBLE}.
         * @param maxValue the minimum value
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder setMaxValue(@Nullable Number maxValue) {
            if (type != Type.LONG && type != Type.DOUBLE) {
                throw new IllegalStateException("Must be of type INTEGER or DOUBLE to specify maximum value");
            }
            this.maxValue = maxValue;
            return this;
        }

        @NotNull
        public CommandOption build() {
            if (minValue != null && maxValue != null && minValue.doubleValue() >= maxValue.doubleValue()) {
                throw new IllegalStateException("Minimum value cannot be greater than or equal to maximum value");
            }
            if (!choices.isEmpty() && autoComplete) {
                throw new IllegalStateException("Cannot use auto complete and specify choices at the same time");
            }
            return new CommandOption(type, name, description, choices, required, autoComplete, channelTypes, minValue, maxValue);
        }
    }

    public enum Type implements JDAEntity<OptionType> {
        STRING(OptionType.STRING),
        LONG(OptionType.INTEGER),
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
