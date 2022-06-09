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

package com.discordsrv.api.discord.entity.component;

import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;

import java.util.UUID;

public class TextInput implements ModalComponent {

    public static Builder builder(String label, Style style) {
        return new Builder(label, style);
    }

    private final String id;
    private final String label;
    private final Style style;
    private final int minLength;
    private final int maxLength;
    private final String placeholder;
    private final boolean required;
    private final String defaultValue;

    public TextInput(String label, Style style, int minLength, int maxLength, String placeholder, boolean required, String defaultValue) {
        this.id = UUID.randomUUID().toString();
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

        private final String label;
        private final Style style;
        private int minLength = 0;
        private int maxLength = Integer.MAX_VALUE;
        private String placeholder;
        private boolean required = true;
        private String defaultValue;

        private Builder(String label, Style style) {
            this.label = label;
            this.style = style;
        }

        public String getLabel() {
            return label;
        }

        public Style getStyle() {
            return style;
        }

        public Builder setMinLength(int minLength) {
            this.minLength = minLength;
            return this;
        }

        public int getMinLength() {
            return minLength;
        }

        public Builder setMaxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public Builder setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public String getPlaceholder() {
            return placeholder;
        }

        public Builder setRequired(boolean required) {
            this.required = required;
            return this;
        }

        public boolean isRequired() {
            return required;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String getDefaultValue() {
            return defaultValue;
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
