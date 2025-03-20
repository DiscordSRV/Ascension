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

package com.discordsrv.api.events.discord.interaction;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.events.discord.AbstractDiscordEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public abstract class AbstractInteractionEvent<T extends GenericInteractionCreateEvent> extends AbstractDiscordEvent<T> {

    protected final ComponentIdentifier identifier;
    protected final DiscordUser user;
    protected final DiscordGuildMember member;
    protected final DiscordMessageChannel channel;

    public AbstractInteractionEvent(
            T jdaEvent,
            ComponentIdentifier identifier,
            DiscordUser user,
            DiscordGuildMember member,
            DiscordMessageChannel channel
    ) {
        super(jdaEvent);
        this.identifier = identifier;
        this.user = user;
        this.member = member;
        this.channel = channel;
    }

    public ComponentIdentifier getIdentifier() {
        return identifier;
    }

    public boolean isFor(ComponentIdentifier identifier) {
        return this.identifier.equals(identifier);
    }

    @NotNull
    public Locale getGuildLocale() {
        return jdaEvent.getGuildLocale().toLocale();
    }

    @NotNull
    public Locale getUserLocale() {
        return jdaEvent.getUserLocale().toLocale();
    }

    @NotNull
    public DiscordUser getUser() {
        return user;
    }

    @Nullable
    public DiscordGuildMember getMember() {
        return member;
    }

    public DiscordGuild getGuild() {
        return member != null ? member.getGuild() : null;
    }

    @NotNull
    public DiscordMessageChannel getChannel() {
        return channel;
    }
}
