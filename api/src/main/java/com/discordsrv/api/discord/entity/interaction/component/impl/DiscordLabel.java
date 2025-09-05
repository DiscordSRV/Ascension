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
import com.discordsrv.api.discord.entity.interaction.component.component.LabelComponent;
import net.dv8tion.jda.api.components.label.Label;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DiscordLabel implements JDAEntity<Label> {

    @NotNull
    public static DiscordLabel of(@NotNull String label, @Nullable String description, @NotNull LabelComponent<?> child) {
        return new DiscordLabel(label, description, child);
    }

    private final String label;
    private final String description;
    private final LabelComponent<?> child;

    private DiscordLabel(String label, String description, LabelComponent<?> child) {
        this.label = label;
        this.description = description;
        this.child = child;
    }

    @Override
    public Label asJDA() {
        return Label.of(label, description, child.asJDA());
    }
}
