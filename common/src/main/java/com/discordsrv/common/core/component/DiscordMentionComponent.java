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

package com.discordsrv.common.core.component;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEventSource;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * An Adventure {@link Component} that holds a Discord mention whilst being disguised as a {@link TextComponent}
 * for compatibility with serializers that don't know how to deal with custom Component types.
 * <p>
 * Possibly removable after <a href="https://github.com/KyoriPowered/adventure/pull/842">adventure #842</a>
 *
 * @see ComponentFactory#discordSerialize(Component)
 */
public class DiscordMentionComponent implements TextComponent {

    @NotNull
    public static DiscordMentionComponent of(@NotNull String mention) {
        return new DiscordMentionComponent(new ArrayList<>(), Style.empty(), mention);
    }

    @NotNull
    public static Builder builder(@NotNull String mention) {
        return new Builder(new ArrayList<>(), Style.empty(), mention);
    }

    private final List<Component> children;
    private final Style style;
    private final String mention;

    private DiscordMentionComponent(List<? extends ComponentLike> children, Style style, String mention) {
        this.children = ComponentLike.asComponents(children, IS_NOT_EMPTY);
        this.style = style;
        this.mention = mention;
    }

    @Override
    @Deprecated // NOOP
    public @NotNull String content() {
        return "";
    }

    @Override
    @Deprecated // NOOP
    public @NotNull DiscordMentionComponent content(@NotNull String content) {
        return this;
    }

    @Override
    public @NotNull Component asComponent() {
        return TextComponent.super.asComponent();
    }

    @Override
    public @NotNull Builder toBuilder() {
        return new Builder(children, style, mention);
    }

    @Override
    public @Unmodifiable @NotNull List<Component> children() {
        return children;
    }

    @Override
    public @NotNull DiscordMentionComponent children(@NotNull List<? extends ComponentLike> children) {
        return new DiscordMentionComponent(children, style, mention);
    }

    @Override
    public @NotNull Style style() {
        return style;
    }

    @Override
    public @NotNull DiscordMentionComponent style(@NotNull Style style) {
        return new DiscordMentionComponent(children, style, mention);
    }

    @NotNull
    public String mention() {
        return mention;
    }

    public @NotNull DiscordMentionComponent mention(@NotNull String mention) {
        return new DiscordMentionComponent(children, style, mention);
    }

    @Override
    public String toString() {
        return "DiscordMentionComponent{" +
                "children=" + children +
                ", style=" + style +
                ", mention='" + mention + '\'' +
                '}';
    }

    public static class Builder implements TextComponent.Builder {

        private final List<Component> children;
        private Style.Builder styleBuilder;
        private String mention;

        private Builder(List<Component> children, Style style, String mention) {
            this.children = children;
            this.styleBuilder = style.toBuilder();
            this.mention = mention;
        }

        @Override
        @Deprecated // NOOP
        public @NotNull String content() {
            return "";
        }

        @NotNull
        @Override
        @Deprecated // NOOP
        public DiscordMentionComponent.Builder content(@NotNull String content) {
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder append(@NotNull Component component) {
            if (component == Component.empty()) return this;
            this.children.add(component);
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder append(@NotNull Component @NotNull ... components) {
            for (Component component : components) {
                append(component);
            }
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder append(@NotNull ComponentLike @NotNull ... components) {
            for (ComponentLike component : components) {
                append(component);
            }
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder append(@NotNull Iterable<? extends ComponentLike> components) {
            for (Component child : children) {
                append(child);
            }
            return this;
        }

        @Override
        public @NotNull List<Component> children() {
            return children;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder style(@NotNull Style style) {
            this.styleBuilder = style.toBuilder();
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder style(@NotNull Consumer<Style.Builder> consumer) {
            consumer.accept(styleBuilder);
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder font(@Nullable Key font) {
            styleBuilder.font(font);
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder color(@Nullable TextColor color) {
            styleBuilder.color(color);
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder colorIfAbsent(@Nullable TextColor color) {
            styleBuilder.color(color);
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder decoration(@NotNull TextDecoration decoration, TextDecoration.State state) {
            styleBuilder.decoration(decoration, state);
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder decorationIfAbsent(@NotNull TextDecoration decoration, TextDecoration.State state) {
            styleBuilder.decorationIfAbsent(decoration, state);
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder clickEvent(@Nullable ClickEvent event) {
            styleBuilder.clickEvent(event);
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder hoverEvent(@Nullable HoverEventSource<?> source) {
            styleBuilder.hoverEvent(source);
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder insertion(@Nullable String insertion) {
            styleBuilder.insertion(insertion);
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder mergeStyle(@NotNull Component that, @NotNull Set<Style.Merge> merges) {
            styleBuilder.merge(that.style(), merges);
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder resetStyle() {
            styleBuilder = Style.style();
            return this;
        }

        @NotNull
        public String mention() {
            return mention;
        }

        public DiscordMentionComponent.@NotNull Builder mention(@NotNull String mention) {
            this.mention = mention;
            return this;
        }

        @Override
        public @NotNull DiscordMentionComponent build() {
            return new DiscordMentionComponent(children, styleBuilder.build(), mention);
        }

        /*
         * Copyright (c) 2017-2023 KyoriPowered
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
        @Override
        public DiscordMentionComponent.@NotNull Builder applyDeep(@NotNull Consumer<? super ComponentBuilder<?, ?>> action) {
            this.apply(action);
            if (this.children == Collections.<Component>emptyList()) {
                return this;
            }
            ListIterator<Component> it = this.children.listIterator();
            while (it.hasNext()) {
                final Component child = it.next();
                if (!(child instanceof BuildableComponent<?, ?>)) {
                    continue;
                }
                final ComponentBuilder<?, ?> childBuilder = ((BuildableComponent<?, ?>) child).toBuilder();
                childBuilder.applyDeep(action);
                it.set(childBuilder.build());
            }
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder mapChildren(
                @NotNull Function<BuildableComponent<?, ?>, ? extends BuildableComponent<?, ?>> function) {
            if (this.children == Collections.<Component>emptyList()) {
                return this;
            }
            final ListIterator<Component> it = this.children.listIterator();
            while (it.hasNext()) {
                final Component child = it.next();
                if (!(child instanceof BuildableComponent<?, ?>)) {
                    continue;
                }
                final BuildableComponent<?, ?> mappedChild = requireNonNull(function.apply((BuildableComponent<?, ?>) child), "mappedChild");
                if (child == mappedChild) {
                    continue;
                }
                it.set(mappedChild);
            }
            return this;
        }

        @Override
        public DiscordMentionComponent.@NotNull Builder mapChildrenDeep(
                @NotNull Function<BuildableComponent<?, ?>, ? extends BuildableComponent<?, ?>> function) {
            if (this.children == Collections.<Component>emptyList()) {
                return this;
            }
            final ListIterator<Component> it = this.children.listIterator();
            while (it.hasNext()) {
                final Component child = it.next();
                if (!(child instanceof BuildableComponent<?, ?>)) {
                    continue;
                }
                final BuildableComponent<?, ?> mappedChild = requireNonNull(function.apply((BuildableComponent<?, ?>) child), "mappedChild");
                if (mappedChild.children().isEmpty()) {
                    if (child == mappedChild) {
                        continue;
                    }
                    it.set(mappedChild);
                } else {
                    final ComponentBuilder<?, ?> builder = mappedChild.toBuilder();
                    builder.mapChildrenDeep(function);
                    it.set(builder.build());
                }
            }
            return this;
        }
    }
}
