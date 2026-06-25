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
import com.discordsrv.api.discord.entity.interaction.component.component.ModalComponent;
import com.discordsrv.api.events.discord.interaction.DiscordModalInteractionEvent;
import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A Discord modal.
 * @see #builder(ComponentIdentifier, String)
 * @see DiscordModalInteractionEvent
 */
public class DiscordModal implements JDAEntity<Modal> {

    /**
     * Creates a new modal builder.
     *
     * @param id a unique identifier for this interaction, used to check if a given event was for this interaction
     * @param title the title of the modal
     * @return a new modal builder
     */
    @NotNull
    public static Builder builder(@NotNull ComponentIdentifier id, @NotNull String title) {
        return new Builder(id, title);
    }

    private final ComponentIdentifier id;
    private final String title;
    private final List<ModalComponent<?>> components;
    private final Consumer<DiscordModalInteractionEvent> eventHandler;

    private DiscordModal(ComponentIdentifier id, String title, List<ModalComponent<?>> components, Consumer<DiscordModalInteractionEvent> eventHandler) {
        this.id = id;
        this.title = title;
        this.components = components;
        this.eventHandler = eventHandler;
    }

    /**
     * The internal discord identifier used when creating the JDA Modal.
     */
    public ComponentIdentifier getId() {
        return id;
    }

    // registry moved to DiscordAPIImpl

    public String getTitle() {
        return title;
    }

    public List<ModalComponent<?>> getComponents() {
        return components;
    }

    public Consumer<DiscordModalInteractionEvent> getEventHandler() {
        return eventHandler;
    }

    @Override
    public Modal asJDA() {
        return Modal.create(id.getDiscordIdentifier(), title)
                .addComponents(components.stream().map(JDAEntity::asJDA).map(entity -> (ModalTopLevelComponent) entity).collect(Collectors.toList()))
                .build();
    }

    public static class Builder {

        private final ComponentIdentifier id;
        private final String title;
        private final List<ModalComponent<?>> components = new ArrayList<>();
        private Consumer<DiscordModalInteractionEvent> eventHandler;

        public Builder(ComponentIdentifier id, String title) {
            this.id = id;
            this.title = title;
        }

        /**
         * Adds a component to this modal.
         * @param component the component to add
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder addComponent(ModalComponent<?> component) {
            return addComponents(component);
        }

        /**
         * Sets the event handler for this modal. This can be used instead of listening to the specific interaction event.
         * @param eventHandler the event handler, only receives events for this modal
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder setEventHandler(Consumer<DiscordModalInteractionEvent> eventHandler) {
            this.eventHandler = eventHandler;
            return this;
        }

        /**
         * Adds multiple components to this modal.
         * @param components the components to add
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder addComponents(ModalComponent<?>... components) {
            this.components.addAll(Arrays.asList(components));
            return this;
        }

        /**
         * Builds the modal.
         * @return a new modal
         */
        public DiscordModal build() {
            return new DiscordModal(id, title, components, eventHandler);
        }
    }
}
