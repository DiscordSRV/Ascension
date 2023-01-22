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

import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.interaction.component.ModalComponent;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.jetbrains.annotations.NotNull;

/**
 * A Discord text input, a modal component.
 * @see #builder(ComponentIdentifier, String, Style)
 */
public class TextInput implements ModalComponent {

    /**
     * Creates a new text input builder with the given label and style.
     *
     * @param id a unique identifier for this component, used to check for the value of the input
     * @param label the label shown above the text input
     * @param style the style of the text input
     * @return a new text input builder
     */
    @NotNull
    public static Builder builder(@NotNull ComponentIdentifier id, @NotNull String label, @NotNull Style style) {
        return new Builder(id.getDiscordIdentifier(), label, style);
    }

    private final String id;
    private final String label;
    private final Style style;
    private final int minLength;
    private final int maxLength;
    private final String placeholder;
    private final boolean required;
    private final String defaultValue;

    public TextInput(
            String id,
            String label,
            Style style,
            int minLength,
            int maxLength,
            String placeholder,
            boolean required,
            String defaultValue
    ) {
        this.id = id;
        this.label = label;
        this.style = style;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.placeholder = placeholder;
        this.required = required;
        this.defaultValue = defaultValue;
    }

    public String getLabel() {
        return label;
    }

    public Style getStyle() {
        return style;
    }

    public int getMinLength() {
        return minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    @Override
    public ItemComponent asJDA() {
        net.dv8tion.jda.api.interactions.components.text.TextInput.Builder builder =
                net.dv8tion.jda.api.interactions.components.text.TextInput.create(id, label, style.getJda())
                        .setMinLength(minLength)
                        .setMaxLength(maxLength)
                        .setPlaceholder(placeholder)
                        .setRequired(required)
                        .setValue(defaultValue);

        return builder.build();
    }

    public static class Builder {

        private final String id;
        private final String label;
        private final Style style;
        private int minLength = 0;
        private int maxLength = Integer.MAX_VALUE;
        private String placeholder;
        private boolean required = true;
        private String defaultValue;

        private Builder(String id, String label, Style style) {
            this.id = id;
            this.label = label;
            this.style = style;
        }

        public Builder setMinLength(int minLength) {
            this.minLength = minLength;
            return this;
        }

        public Builder setMaxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public Builder setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public Builder setRequired(boolean required) {
            this.required = required;
            return this;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public TextInput build() {
            return new TextInput(
                    id,
                    label,
                    style,
                    minLength,
                    maxLength,
                    placeholder,
                    required,
                    defaultValue
            );
        }
    }

    public enum Style {
        PARAGRAPH(TextInputStyle.PARAGRAPH),
        SHORT(TextInputStyle.SHORT);

        private final TextInputStyle jda;

        Style(TextInputStyle jda) {
            this.jda = jda;
        }

        public TextInputStyle getJda() {
            return jda;
        }
    }
}
