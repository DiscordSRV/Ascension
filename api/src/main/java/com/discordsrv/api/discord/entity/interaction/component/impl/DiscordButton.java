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

package com.discordsrv.api.discord.entity.interaction.component.impl;

import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.guild.DiscordCustomEmoji;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.interaction.component.component.ActionRowComponent;
import com.discordsrv.api.events.discord.interaction.component.DiscordButtonInteractionEvent;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A Discord button.
 * @see #builder(ComponentIdentifier, Style)
 * @see #urlBuilder(String)
 * @see DiscordButtonInteractionEvent
 */
public class DiscordButton implements ActionRowComponent<Button> {

    /**
     * Creates a new Button builder.
     *
     * @param id a unique identifier for this interaction, used to check if a given event was for this interaction
     * @param style the style of the button
     * @return a new button builder
     */
    @NotNull
    public static Builder builder(@NotNull ComponentIdentifier id, @NotNull DiscordButton.Style style) {
        return new Builder(id.getDiscordIdentifier(), style);
    }

    /**
     * Creates a new Link button builder.
     *
     * @param url the link the button leads to
     * @return a new button builder
     */
    @NotNull
    public static Builder urlBuilder(@NotNull String url) {
        return new Builder(null, Style.LINK).setUrl(url);
    }

    private final Style buttonStyle;
    private final String idOrUrl;
    private final String label;
    private final Emoji emoji;
    private final boolean disabled;

    private DiscordButton(
            String id,
            Style buttonStyle,
            String url,
            String label,
            Emoji emoji,
            boolean disabled
    ) {
        this.buttonStyle = buttonStyle;
        this.idOrUrl = buttonStyle == Style.LINK ? url : id;
        this.label = label;
        this.emoji = emoji;
        this.disabled = disabled;
    }

    @NotNull
    public Style getButtonStyle() {
        return buttonStyle;
    }

    @Nullable
    public String getUrl() {
        return buttonStyle == Style.LINK ? idOrUrl : null;
    }

    @NotNull
    public String getLabel() {
        return label;
    }

    @Nullable
    public Emoji getJDAEmoji() {
        return emoji;
    }

    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public Button asJDA() {
        return Button.of(
                buttonStyle.asJDA(),
                idOrUrl,
                label,
                emoji
        ).withDisabled(disabled);
    }

    @CheckReturnValue
    public static class Builder {

        private final String id;
        private final Style style;
        private String url;
        private String label;
        private Emoji emoji;
        private boolean disabled;

        private Builder(String id, Style style) {
            this.id = id;
            this.style = style;
        }

        /**
         * Sets the url for this button, only works if the style is {@link Style#LINK}.
         *
         * @param url the url for this button
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder setUrl(@NotNull String url) {
            if (style != Style.LINK) {
                throw new IllegalStateException("Style must be LINK to set a url");
            }
            this.url = url;
            return this;
        }

        /**
         * Sets the text shown on this button.
         * @param label the text
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder setLabel(String label) {
            this.label = label;
            return this;
        }

        /**
         * Sets the emoji to show on this button.
         * @param unicodeEmoji the unicode code point for the emoji
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder setEmoji(String unicodeEmoji) {
            this.emoji = Emoji.fromUnicode(unicodeEmoji);
            return this;
        }

        /**
         * Sets the emoji to show on this button.
         * @param emote the Discord server/guild emote
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder setEmoji(DiscordCustomEmoji emote) {
            this.emoji = Emoji.fromCustom(emote.asJDA());
            return this;
        }

        /**
         * Set if this button is disabled or not. Default is {@code false}.
         * @param disabled if this button should be disabled
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder setDisabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        /**
         * Creates the button.
         * @return a new button
         */
        public DiscordButton build() {
            if (style == null) {
                throw new IllegalStateException("No style set");
            }
            return new DiscordButton(
                    id,
                    style,
                    style == Style.LINK ? url : UUID.randomUUID().toString(),
                    label,
                    emoji,
                    disabled
            );
        }
    }

    public enum Style implements JDAEntity<ButtonStyle> {
        PRIMARY(ButtonStyle.PRIMARY),
        SECONDARY(ButtonStyle.SECONDARY),
        SUCCESS(ButtonStyle.SUCCESS),
        DANGER(ButtonStyle.DANGER),
        LINK(ButtonStyle.LINK);

        private final ButtonStyle style;

        Style(ButtonStyle style) {
            this.style = style;
        }

        @Override
        public ButtonStyle asJDA() {
            return style;
        }
    }
}
