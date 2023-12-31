/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.discord.entity.interaction.component.impl;

import com.discordsrv.api.discord.entity.guild.DiscordCustomEmoji;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.interaction.component.MessageComponent;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A Discord selection menu.
 * @see #builder(ComponentIdentifier)
 * @see com.discordsrv.api.event.events.discord.interaction.component.DiscordSelectMenuInteractionEvent
 */
// TODO: newest changes
public class SelectMenu implements MessageComponent {

    /**
     * Creates a selection menu builder.
     *
     * @param id a unique identifier for this interaction, used to check if a given event was for this interaction
     * @return a new builder
     */
    public static Builder builder(@NotNull ComponentIdentifier id) {
        return new Builder(id.getDiscordIdentifier());
    }

    private final String id;
    private final List<Option> options;
    private final boolean disabled;
    private final String placeholder;
    private final int minValues;
    private final int maxValues;

    private SelectMenu(String id, List<Option> options, boolean disabled, String placeholder, int minValues, int maxValues) {
        this.id = id;
        this.options = options;
        this.disabled = disabled;
        this.placeholder = placeholder;
        this.minValues = minValues;
        this.maxValues = maxValues;
    }

    @Override
    public ItemComponent asJDA() {
        return null;
//        net.dv8tion.jda.api.interactions.components.selections.SelectMenu.Builder<?, ?> builder =
//                new net.dv8tion.jda.api.interactions.components.selections.SelectMenu.Builder<>(id)
//                        .setDisabled(disabled)
//                        .setPlaceholder(placeholder)
//                        .setMinValues(minValues)
//                        .setMaxValues(maxValues);
//
//        Set<SelectOption> defaultOptions = new HashSet<>();
//        for (Option option : options) {
//            SelectOption selectOption = SelectOption.of(option.getLabel(), option.getValue())
//                    .withEmoji(option.getEmoji())
//                    .withDescription(option.getDescription());
//
//            builder.addOptions(selectOption);
//            if (option.isDefault()) {
//                defaultOptions.add(selectOption);
//            }
//        }
//        builder.setDefaultOptions(defaultOptions);
//
//        return builder.build();
    }

    /**
     * A selection menu option.
     * @see #of(String, String)
     */
    public static class Option {

        /**
         * Creates a new selection menu option.
         * @param label the label for the option
         * @param value the value of the option, this is sent back to you when a selection is made
         * @return a new option
         */
        public static Option of(String label, String value) {
            return new Option(label, value, null, null, false);
        }

        private final String label;
        private final String value;
        private final String description;
        private final Emoji emoji;
        private final boolean defaultSelected;

        private Option(String label, String value, String description, Emoji emoji, boolean defaultSelected) {
            this.label = label;
            this.value = value;
            this.description = description;
            this.emoji = emoji;
            this.defaultSelected = defaultSelected;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public Emoji getEmoji() {
            return emoji;
        }

        public boolean isDefault() {
            return defaultSelected;
        }

        /**
         * Creates a new option with the provided description.
         * @param description the new description
         * @return a new option
         */
        @CheckReturnValue
        public Option withDescription(String description) {
            return new Option(label, value, description, emoji, defaultSelected);
        }

        /**
         * Creates a new option with the provided emoji.
         * @param unicodeEmoji the unicode codepoint for the emoji
         * @return a new option
         */
        @CheckReturnValue
        public Option withEmoji(String unicodeEmoji) {
            return new Option(label, value, description, Emoji.fromUnicode(unicodeEmoji), defaultSelected);
        }

        /**
         * Creates a new option with the provided emote.
         * @param emote the Discord server/guild emote
         * @return a new option
         */
        @CheckReturnValue
        public Option withEmoji(DiscordCustomEmoji emote) {
            return new Option(label, value, description, Emoji.fromCustom(emote.asJDA()), defaultSelected);
        }

        /**
         * Creates a new option with the provided default selection state.
         * @param defaultSelected if the option should be selected by default
         * @return a new option
         */
        @CheckReturnValue
        public Option withDefaultSelected(boolean defaultSelected) {
            return new Option(label, value, description, emoji, defaultSelected);
        }
    }

    private static class Builder {

        private final String id;
        private final List<Option> options = new ArrayList<>();
        private boolean disabled = false;
        private String placeholder;
        private int minValues = 0;
        private int maxValues = 1;

        public Builder(String id) {
            this.id = id;
        }

        /**
         * Adds an option to this selection menu.
         * @param option the option to add
         * @return this builder, useful for chaining
         * @see Option#of(String, String)
         */
        @NotNull
        public Builder addOption(Option option) {
            this.options.add(option);
            return this;
        }

        /**
         * Adds multiple options to this selection menu.
         * @param options the options to add
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder addOptions(Option... options) {
            this.options.addAll(Arrays.asList(options));
            return this;
        }

        /**
         * Sets if this selection menu should be disabled. Default is {@code false}.
         * @param disabled if this selection menu should be disabled
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder setDisabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        /**
         * Sets the placeholder text for this selection menu.
         * @param placeholder the placeholder text
         * @return this builder, useful for chaining
         */
        public Builder setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        /**
         * Sets the minimum amount of values to select. The default is {@code 0}.
         * @param minValues the minimum value amount
         * @return this builder, useful for chaining
         */
        public Builder setMinValues(int minValues) {
            this.minValues = minValues;
            return this;
        }

        /**
         * Sets the maximum amount of values to select. The default is {@code 1}.
         * @param maxValues the maximum value amount
         * @return this builder, useful for chaining
         */
        public Builder setMaxValues(int maxValues) {
            this.maxValues = maxValues;
            return this;
        }

        /**
         * Builds the selection menu.
         * @return a new selection menu
         */
        public SelectMenu build() {
            return new SelectMenu(id, options, disabled, placeholder, minValues, maxValues);
        }
    }
}
