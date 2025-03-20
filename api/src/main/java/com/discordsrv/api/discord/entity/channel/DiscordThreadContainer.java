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

package com.discordsrv.api.discord.entity.channel;

import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.task.Task;
import net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A Discord channel that contains threads.
 */
public interface DiscordThreadContainer extends DiscordGuildChannel {

    @NotNull
    List<DiscordThreadChannel> getActiveThreads();

    Task<List<DiscordThreadChannel>> retrieveArchivedPrivateThreads();
    Task<List<DiscordThreadChannel>> retrieveArchivedJoinedPrivateThreads();
    Task<List<DiscordThreadChannel>> retrieveArchivedPublicThreads();

    Task<DiscordThreadChannel> createThread(String name, boolean privateThread);
    Task<DiscordThreadChannel> createThread(String name, long messageId);

    /**
     * Returns the JDA representation of this object. This should not be used if it can be avoided.
     * @return the JDA representation of this object
     * @see DiscordSRV#jda()
     */
    IThreadContainer getAsJDAThreadContainer();
}
