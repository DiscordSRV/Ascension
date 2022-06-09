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

import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.component.actionrow.ActionRow;
import com.discordsrv.api.discord.entity.component.actionrow.ModalActionRow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A Discord modal.
 * @see #builder(String)
 */
public class Modal implements JDAEntity<net.dv8tion.jda.api.interactions.components.Modal> {

    /**
     * Creates a new modal builder.
     * @param title the title of the modal
     * @return a new modal builder
     */
    public static Builder builder(String title) {
        return new Builder(title);
    }

    private final String id;
    private final String title;
    private final List<ModalActionRow> rows;

    private Modal(String title, List<ModalActionRow> rows) {
        this.id = UUID.randomUUID().toString();
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
    public net.dv8tion.jda.api.interactions.components.Modal asJDA() {
        return net.dv8tion.jda.api.interactions.components.Modal.create(id, title)
                .addActionRows(rows.stream().map(ActionRow::asJDA).collect(Collectors.toList()))
                .build();
    }

    private static class Builder {

        private final String title;
        private final List<ModalActionRow> rows = new ArrayList<>();

        public Builder(String title) {
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

        @NotNull
        @Unmodifiable
        public List<ModalActionRow> getRows() {
            return Collections.unmodifiableList(rows);
        }

        /**
         * Builds the modal.
         * @return a new modal
         */
        public Modal build() {
            return new Modal(title, rows);
        }
    }
}
