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

package com.discordsrv.api.events.message.render.discord;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.discord.entity.guild.DiscordCustomEmoji;
import com.discordsrv.api.events.Event;
import com.discordsrv.api.events.Processable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Renders a given {@link DiscordCustomEmoji} into a {@link MinecraftComponent} that can be displayed in Minecraft.
 * @see #process(MinecraftComponent)
 */
public class DiscordChatMessageCustomEmojiRenderEvent implements Event, Processable.Argument<MinecraftComponent> {

    private final DiscordCustomEmoji emoji;
    private MinecraftComponent rendered = null;

    public DiscordChatMessageCustomEmojiRenderEvent(@NotNull DiscordCustomEmoji emoji) {
        this.emoji = emoji;
    }

    @NotNull
    public DiscordCustomEmoji getEmoji() {
        return emoji;
    }

    /**
     * Gets the rendered representation of the emoji.
     * @return the rendered representation of the emoji if this event has been processed otherwise {@code null}
     */
    @Nullable
    public MinecraftComponent getRenderedEmojiFromProcessing() {
        return rendered;
    }

    @Override
    public boolean isProcessed() {
        return rendered != null;
    }

    /**
     * Marks this event as processed, with the given {@link MinecraftComponent} being the representation of {@link DiscordCustomEmoji} in game.
     * @param renderedEmote the rendered emote
     */
    @Override
    public void process(@NotNull MinecraftComponent renderedEmote) {
        Processable.Argument.super.process(renderedEmote);
        rendered = renderedEmote;
    }
}
