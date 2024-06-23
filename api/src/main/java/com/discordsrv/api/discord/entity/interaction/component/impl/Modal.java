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

package com.discordsrv.api.discord.entity.interaction.component.impl;

import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.interaction.component.actionrow.ActionRow;
import com.discordsrv.api.discord.entity.interaction.component.actionrow.ModalActionRow;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A Discord modal.
 * @see #builder(ComponentIdentifier, String)
 * @see com.discordsrv.api.event.events.discord.interaction.DiscordModalInteractionEvent
 */
public class Modal implements JDAEntity<net.dv8tion.jda.api.interactions.modals.Modal> {

    /**
     * Creates a new modal builder.
     *
     * @param id a unique identifier for this interaction, used to check if a given event was for this interaction
     * @param title the title of the modal
     * @return a new modal builder
     */
    @NotNull
    public static Builder builder(@NotNull ComponentIdentifier id, @NotNull String title) {
        return new Builder(id.getDiscordIdentifier(), title);
    }

    private final String id;
    private final String title;
    private final List<ModalActionRow> rows;

    private Modal(String id, String title, List<ModalActionRow> rows) {
        this.id = id;
        this.title = title;
        this.rows = rows;
    }

    public String getTitle() {
        return title;
    }

    public List<ModalActionRow> getRows() {
        return rows;
    }

    public Modal addRow(ModalActionRow row) {
        this.rows.add(row);
        return this;
    }

    @Override
    public net.dv8tion.jda.api.interactions.modals.Modal asJDA() {
        return net.dv8tion.jda.api.interactions.modals.Modal.create(id, title)
                .addComponents(rows.stream().map(ActionRow::asJDA).collect(Collectors.toList()))
                .build();
    }

    private static class Builder {

        private final String id;
        private final String title;
        private final List<ModalActionRow> rows = new ArrayList<>();

        public Builder(String id, String title) {
            this.id = id;
            this.title = title;
        }

        /**
         * Adds an action row to this modal.
         * @param row the row to add
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder addRow(ModalActionRow row) {
            this.rows.add(row);
            return this;
        }

        /**
         * Adds multiple action rows to this modal.
         * @param rows the rows to add
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder addRows(ModalActionRow... rows) {
            this.rows.addAll(Arrays.asList(rows));
            return this;
        }

        /**
         * Builds the modal.
         * @return a new modal
         */
        public Modal build() {
            return new Modal(id, title, rows);
        }
    }
}
