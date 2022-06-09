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

import com.discordsrv.api.discord.entity.guild.DiscordEmote;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * A Discord button.
 * @see #builder(Style)
 */
public class Button implements MessageComponent {

    /**
     * Creates a new Button builder.
     * @param style the style of the button
     * @return a new button builder
     */
    public static Builder builder(@NotNull Button.Style style) {
        return new Builder(style);
    }

    private final Style buttonStyle;
    private final String idOrUrl;
    private final String label;
    private final Emoji emoji;
    private final boolean disabled;
    private final ClickHandler clickHandler;

    private Button(
            Style buttonStyle,
            String url,
            String label,
            Emoji emoji,
            boolean disabled,
            ClickHandler clickHandler
    ) {
        this.buttonStyle = buttonStyle;
        this.idOrUrl = buttonStyle == Style.LINK ? url : UUID.randomUUID().toString();
        this.label = label;
        this.emoji = emoji;
        this.disabled = disabled;
        this.clickHandler = clickHandler;
    }

    @NotNull
    public Style getButtonStyle() {
        return buttonStyle;
    }

    @NotNull
    public Optional<String> getUrl() {
        return buttonStyle == Style.LINK ? Optional.of(idOrUrl) : Optional.empty();
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

    public ClickHandler getClickHandler() {
        return clickHandler;
    }

    @Override
    public ItemComponent asJDA() {
        return net.dv8tion.jda.api.interactions.components.buttons.Button.of(
                buttonStyle.getJDA(),
                idOrUrl,
                label,
                emoji
        ).withDisabled(disabled);
    }

    private static class Builder {

        private final Style style;
        private String url;
        private String label;
        private Emoji emoji;
        private boolean disabled;
        private ClickHandler clickHandler;

        private Builder(Style style) {
            this.style = style;
        }

        @NotNull
        public Style getStyle() {
            return style;
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

        public String getUrl() {
            return url;
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

        public String getLabel() {
            return label;
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
        public Builder setEmoji(DiscordEmote emote) {
            this.emoji = Emoji.fromEmote(emote.asJDA());
            return this;
        }

        public Emoji getEmoji() {
            return emoji;
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

        public boolean isDisabled() {
            return disabled;
        }

        /**
         * Sets the click handler for this button, does not work if the style is {@link Style#LINK}.
         * @param clickHandler the click handler
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder setClickHandler(ClickHandler clickHandler) {
            if (style == Style.LINK) {
                throw new IllegalStateException("Cannot set click handler for LINK type button, use setUrl instead");
            }
            this.clickHandler = clickHandler;
            return this;
        }

        public ClickHandler getClickHandler() {
            return clickHandler;
        }

        /**
         * Creates the button.
         * @return a new button
         */
        public Button build() {
            if (style == null) {
                throw new IllegalStateException("No style set");
            }
            return new Button(
                    style,
                    style == Style.LINK ? url : UUID.randomUUID().toString(),
                    label,
                    emoji,
                    disabled,
                    clickHandler
            );
        }
    }

    public enum Style {
        PRIMARY(ButtonStyle.PRIMARY),
        SECONDARY(ButtonStyle.SECONDARY),
        SUCCESS(ButtonStyle.SUCCESS),
        DANGER(ButtonStyle.DANGER),
        LINK(ButtonStyle.LINK);

        private final ButtonStyle style;

        Style(ButtonStyle style) {
            this.style = style;
        }

        public ButtonStyle getJDA() {
            return style;
        }
    }

    @FunctionalInterface
    public interface ClickHandler {

        void onClick(Interaction interaction);

    }
}
