/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.channel.DiscordChannelType;
import com.discordsrv.api.events.discord.interaction.command.DiscordCommandAutoCompleteInteractionEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Predicate;

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

    @NotNull
    public static Builder player(Predicate<DiscordSRVPlayer> playerPredicate) {
        return builder(Type.STRING, "player", "Player name or UUID")
                .setMaxLength(36)
                .setAutoCompleteHandler(event -> {
                    DiscordSRV discordSRV = DiscordSRV.get();
                    String input = event.getOption("player").toLowerCase(Locale.ROOT);

                    for (DiscordSRVPlayer player : discordSRV.playerProvider().allPlayers()) {
                        String username = player.username();
                        if (!username.toUpperCase(Locale.ROOT).startsWith(input) || !playerPredicate.test(player)) {
                            continue;
                        }

                        event.addChoice(username, username);
                    }
                });
    }

    private final Type type;
    private final Map<Locale, String> nameTranslations;
    private final Map<Locale, String> descriptionTranslations;
    private final Map<String, Object> choices;
    private final boolean required;
    private final boolean autoComplete;
    private final AutoCompleteHandler autoCompleteHandler;
    private final Set<DiscordChannelType> channelTypes;
    private final Number minValue;
    private final Number maxValue;
    private final Integer minLength;
    private final Integer maxLength;

    public CommandOption(
            Type type,
            Map<Locale, String> nameTranslations,
            Map<Locale, String> descriptionTranslations,
            Map<String, Object> choices,
            boolean required,
            boolean autoComplete,
            AutoCompleteHandler autoCompleteHandler,
            Set<DiscordChannelType> channelTypes,
            Number minValue,
            Number maxValue,
            Integer minLength,
            Integer maxLength
    ) {
        this.type = type;
        this.nameTranslations = nameTranslations;
        this.descriptionTranslations = descriptionTranslations;
        this.choices = choices;
        this.required = required;
        this.autoComplete = autoComplete;
        this.autoCompleteHandler = autoCompleteHandler;
        this.channelTypes = channelTypes;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @NotNull
    public Type getType() {
        return type;
    }

    @NotNull
    public String getName() {
        return nameTranslations.get(Locale.ROOT);
    }

    @NotNull
    @Unmodifiable
    public Map<Locale, String> getNameTranslations() {
        return Collections.unmodifiableMap(nameTranslations);
    }

    @NotNull
    public String getDescription() {
        return descriptionTranslations.get(Locale.ROOT);
    }

    @NotNull
    @Unmodifiable
    public Map<Locale, String> getDescriptionTranslations() {
        return Collections.unmodifiableMap(descriptionTranslations);
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

    @Nullable
    public AutoCompleteHandler getAutoCompleteHandler() {
        return autoCompleteHandler;
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

    @Nullable
    public Integer getMinLength() {
        return minLength;
    }

    @Nullable
    public Integer getMaxLength() {
        return maxLength;
    }

    @Override
    public OptionData asJDA() {
        OptionData data = new OptionData(getType().asJDA(), getName(), getDescription())
                .setRequired(isRequired())
                .setAutoComplete(isAutoComplete());
        if (getType() == Type.LONG) {
            Number min = getMinValue();
            if (min != null) {
                data.setMinValue(min.longValue());
            }
            Number max = getMaxValue();
            if (max != null) {
                data.setMinValue(max.longValue());
            }
        }
        if (getType() == Type.DOUBLE) {
            Number min = getMinValue();
            if (min != null) {
                data.setMinValue(min.doubleValue());
            }
            Number max = getMaxValue();
            if (max != null) {
                data.setMinValue(max.doubleValue());
            }
        }
        if (getType() == Type.STRING) {
            Integer min = getMinLength();
            if (min != null) {
                data.setMinLength(min);
            }
            Integer max = getMaxLength();
            if (max != null) {
                data.setMaxLength(max);
            }
        }
        for (Map.Entry<String, Object> entry : getChoices().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                data.addChoice(key, (String) value);
            } else if (value instanceof Number && getType() == Type.LONG) {
                data.addChoice(key, ((Number) value).longValue());
            } else if (value instanceof Number && getType() == Type.DOUBLE) {
                data.addChoice(key, ((Number) value).doubleValue());
            } else {
                throw new IllegalStateException("Not a String, Integer or Double choice value");
            }
        }

        for (Map.Entry<Locale, String> entry : getNameTranslations().entrySet()) {
            DiscordLocale locale = DiscordCommand.getJDALocale(entry.getKey());
            if (locale != null) {
                data.setNameLocalization(locale, entry.getValue());
            }
        }
        for (Map.Entry<Locale, String> entry : getDescriptionTranslations().entrySet()) {
            DiscordLocale locale = DiscordCommand.getJDALocale(entry.getKey());
            if (locale != null) {
                data.setDescriptionLocalization(locale, entry.getValue());
            }
        }

        return data;
    }

    @FunctionalInterface
    public interface AutoCompleteHandler {

        void autoComplete(DiscordCommandAutoCompleteInteractionEvent event);

    }

    public static class Builder {

        private final Type type;
        protected final Map<Locale, String> nameTranslations = new LinkedHashMap<>();
        protected final Map<Locale, String> descriptionTranslations = new LinkedHashMap<>();
        private final Map<String, Object> choices = new LinkedHashMap<>();
        private boolean required = false;
        private boolean autoComplete = false;
        private AutoCompleteHandler autoCompleteHandler;
        private final Set<DiscordChannelType> channelTypes = new LinkedHashSet<>();
        private Number minValue;
        private Number maxValue;
        private Integer minLength;
        private Integer maxLength;

        private Builder(Type type, String name, String description) {
            this.type = type;
            this.nameTranslations.put(Locale.ROOT, name);
            this.descriptionTranslations.put(Locale.ROOT, description);
        }

        public void setDescription(String description) {
            this.descriptionTranslations.put(Locale.ROOT, description);
        }

        /**
         * Adds a name translation for this command option.
         * @param locale the language
         * @param translation the translation
         * @return this builder, useful for chaining
         */
        @NotNull
        public CommandOption.Builder addNameTranslation(@NotNull Locale locale, @NotNull String translation) {
            this.nameTranslations.put(locale, translation);
            return this;
        }

        /**
         * Adds a description translation for this command option.
         * @param locale the language
         * @param translation the translation
         * @return this builder, useful for chaining
         */
        @NotNull
        public CommandOption.Builder addDescriptionTranslation(@NotNull Locale locale, @NotNull String translation) {
            this.descriptionTranslations.put(locale, translation);
            return this;
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
         * Sets if this option is auto completed. See {@link DiscordCommandAutoCompleteInteractionEvent}.
         *
         * @param autoComplete is this choice auto completing
         * @return this builder, useful for chaining
         * @see DiscordCommandAutoCompleteInteractionEvent
         */
        @NotNull
        public Builder setAutoComplete(boolean autoComplete) {
            this.autoComplete = autoComplete;
            return this;
        }

        /**
         * Sets the auto complete handler, or clears it. This also does the equivalent {@link #setAutoComplete(boolean)} operation.
         * @param autoCompleteHandler the auto complete handler, or {@code null} to clear
         */
        public Builder setAutoCompleteHandler(@Nullable AutoCompleteHandler autoCompleteHandler) {
            this.autoCompleteHandler = autoCompleteHandler;
            this.autoComplete = autoCompleteHandler != null;
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

        public Builder setMinLength(Integer minLength) {
            this.minLength = minLength;
            return this;
        }

        public Builder setMaxLength(Integer maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        @NotNull
        public CommandOption build() {
            if (minValue != null && maxValue != null && minValue.doubleValue() >= maxValue.doubleValue()) {
                throw new IllegalStateException("Minimum value cannot be greater than or equal to maximum value");
            }
            if (minLength != null && maxLength != null && minLength > maxLength) {
                throw new IllegalStateException("Minimum length cannot be greater than to maximum length");
            }
            if (!choices.isEmpty() && autoComplete) {
                throw new IllegalStateException("Cannot use auto complete and specify choices at the same time");
            }
            return new CommandOption(type, nameTranslations, descriptionTranslations, choices, required,
                                     autoComplete, autoCompleteHandler, channelTypes,
                                     minValue, maxValue, minLength, maxLength);
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
