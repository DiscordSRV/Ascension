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

package com.discordsrv.api.events.message.preprocess.game;

import com.discordsrv.api.channel.GameChannel;
import com.discordsrv.api.color.Color;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.PlayerEvent;
import com.discordsrv.api.player.DiscordSRVPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An advancement (or achievement) message was received,
 * DiscordSRV will process it (if enabled, not already processed and not cancelled) at priority {@link com.discordsrv.api.eventbus.EventPriorities#DEFAULT}.
 * <p>
 * Order of events:
 * <ul>
 * <li>{@link com.discordsrv.api.events.message.preprocess.game.AdvancementMessagePreProcessEvent} (this event)</li>
 * <li>{@link com.discordsrv.api.events.message.postprocess.game.AdvancementMessagePostProcessEvent}</li>
 * <li>{@link com.discordsrv.api.events.message.post.game.AdvancementMessagePostEvent}</li>
 * </ul>
 */
public class AdvancementMessagePreProcessEvent extends AbstractGameMessagePreProcessEvent implements PlayerEvent {

    private final DiscordSRVPlayer player;
    private MinecraftComponent title;
    private MinecraftComponent description;
    private AdvancementFrame frame;

    public AdvancementMessagePreProcessEvent(
            @Nullable Object triggeringEvent,
            @NotNull DiscordSRVPlayer player,
            @Nullable MinecraftComponent message,
            @Nullable MinecraftComponent title,
            @Nullable MinecraftComponent description,
            @Nullable AdvancementFrame frame,
            @Nullable GameChannel gameChannel
    ) {
        this(triggeringEvent, player, message, title, description, frame, gameChannel, false);
    }

    @ApiStatus.Experimental
    public AdvancementMessagePreProcessEvent(
            @Nullable Object triggeringEvent,
            @NotNull DiscordSRVPlayer player,
            @Nullable MinecraftComponent message,
            @Nullable MinecraftComponent title,
            @Nullable MinecraftComponent description,
            @Nullable AdvancementFrame frame,
            @Nullable GameChannel gameChannel,
            boolean cancelled
    ) {
        super(triggeringEvent, cancelled, gameChannel, message);
        this.player = player;
        this.title = title;
        this.description = description;
        this.frame = frame;
    }

    @Override
    @NotNull
    public DiscordSRVPlayer getPlayer() {
        return player;
    }

    @Nullable
    public MinecraftComponent getTitle() {
        return title;
    }

    public void setTitle(@Nullable MinecraftComponent title) {
        this.title = title;
    }

    @Nullable
    public MinecraftComponent getDescription() {
        return description;
    }

    public void setDescription(@Nullable MinecraftComponent description) {
        this.description = description;
    }

    @Nullable public AdvancementFrame getFrame() {
        return frame;
    }

    public void setFrame(@Nullable AdvancementFrame frame) {
        this.frame = frame;
    }

    @Override
    public String toString() {
        return "AwardMessageReceiveEvent{"
                + "player=" + player + ", "
                + "gameChannel=" + GameChannel.toString(gameChannel)
                + '}';
    }

    public enum AdvancementFrame {
        TASK("task", new Color(0x55FF55)), // Green
        GOAL("goal", new Color(0x55FF55)), // Green
        CHALLENGE("challenge", new Color(0xAA00AA)); // Dark Purple

        private final String name;
        private final Color color;

        AdvancementFrame(String id, Color color) {
            this.name = id;
            this.color = color;
        }

        public String id() {
            return name;
        }

        public Color color() {
            return color;
        }

        public static AdvancementFrame fromId(String id) {
            for (AdvancementFrame frame : values()) {
                if (frame.id().equalsIgnoreCase(id)) {
                    return frame;
                }
            }

            throw new IllegalArgumentException("Unknown AdvancementFrame id: " + id);
        }
    }
}
