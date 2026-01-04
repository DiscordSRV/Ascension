/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.interaction.component.component.ActionRowComponent;
import net.dv8tion.jda.api.components.selections.SelectOption;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DiscordOptionSelectMenu implements ActionRowComponent<StringSelectMenu> {

    public Builder builder(ComponentIdentifier identifier) {
        return new Builder(identifier);
    }

    public Option.Builder optionBuilder(ComponentIdentifier identifier, String label) {
        return new Option.Builder(identifier, label);
    }

    private final ComponentIdentifier identifier;
    private final boolean disabled;
    private final boolean required;
    private final String placeholder;
    private final List<Option> options;

    public DiscordOptionSelectMenu(
            ComponentIdentifier identifier,
            boolean disabled,
            boolean required,
            String placeholder,
            List<Option> options
    ) {
        this.identifier = identifier;
        this.disabled = disabled;
        this.required = required;
        this.placeholder = placeholder;
        this.options = Collections.unmodifiableList(options);
    }

    public ComponentIdentifier getIdentifier() {
        return identifier;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isRequired() {
        return required;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    @Unmodifiable
    public List<Option> getOptions() {
        return options;
    }

    @Override
    public StringSelectMenu asJDA() {
        return StringSelectMenu.create(identifier.getDiscordIdentifier())
                .addOptions(options.stream().map(Option::asJDA).collect(Collectors.toList()))
                .setDisabled(disabled)
                .setRequired(required)
                .setPlaceholder(placeholder)
                .build();
    }

    public static class Option implements JDAEntity<SelectOption> {

        private final ComponentIdentifier identifier;
        private final String label;
        private final String description;
        private final boolean isDefault;

        public Option(ComponentIdentifier identifier, String label, String description, boolean isDefault) {
            this.identifier = identifier;
            this.label = label;
            this.description = description;
            this.isDefault = isDefault;
        }

        public ComponentIdentifier getIdentifier() {
            return identifier;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }

        public boolean isDefault() {
            return isDefault;
        }

        @Override
        public SelectOption asJDA() {
            return SelectOption
                    .of(label, identifier.getDiscordIdentifier())
                    .withDescription(description)
                    .withDefault(isDefault);
        }

        @CheckReturnValue
        public static class Builder {

            private final ComponentIdentifier identifier;
            private final String label;
            private String description;
            private boolean isDefault;

            private Builder(ComponentIdentifier identifier, String label) {
                this.identifier = identifier;
                this.label = label;
            }

            public Builder setDescription(String description) {
                this.description = description;
                return this;
            }

            public Builder setDefault(boolean isDefault) {
                this.isDefault = isDefault;
                return this;
            }

            public Option build() {
                return new Option(identifier, label, description, isDefault);
            }
        }
    }

    @CheckReturnValue
    public static class Builder {

        private final ComponentIdentifier identifier;
        private boolean disabled = false;
        private boolean required = false;
        private String placeholder;
        private final List<Option> options = new ArrayList<>();

        private Builder(ComponentIdentifier identifier) {
            this.identifier = identifier;
        }

        public Builder setDisabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public Builder setRequired(boolean required) {
            this.required = required;
            return this;
        }

        public Builder setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
            return this;
        }

        public Builder addOptions(Option... options) {
            this.options.addAll(Arrays.asList(options));
            return this;
        }

        public DiscordOptionSelectMenu build() {
            return new DiscordOptionSelectMenu(identifier, disabled, required, placeholder, options);
        }

    }
}
