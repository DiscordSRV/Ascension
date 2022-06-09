/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.discord.api.entity.component;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.component.Interaction;
import com.discordsrv.api.discord.entity.component.Modal;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.message.ReceivedDiscordMessageImpl;
import com.discordsrv.common.discord.api.entity.message.util.SendableDiscordMessageUtil;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;

import java.util.concurrent.CompletableFuture;

public class InteractionImpl implements Interaction {

    private final DiscordSRV discordSRV;
    private final InteractionHook hook;
    private final DiscordUser user;

    public InteractionImpl(DiscordSRV discordSRV, InteractionHook hook, DiscordUser user) {
        this.discordSRV = discordSRV;
        this.hook = hook;
        this.user = user;
    }

    @Override
    public InteractionHook asJDA() {
        return hook;
    }

    @Override
    public long getExpiryTime() {
        return hook.getExpirationTimestamp();
    }

    @Override
    public boolean isExpired() {
        return hook.isExpired();
    }

    @Override
    public DiscordUser getUser() {
        return user;
    }

    @Override
    public CompletableFuture<Interaction> replyLater(boolean ephemeral) {
        if (!(hook instanceof IReplyCallback)) {
            throw new IllegalStateException("This interaction cannot be replied to");
        }
        return ((IReplyCallback) hook).deferReply(ephemeral).submit()
                .thenApply(hook -> new InteractionImpl(discordSRV, hook, user));
    }

    @Override
    public CompletableFuture<ReceivedDiscordMessage> editOriginal(SendableDiscordMessage message) {
        return hook.editOriginal(SendableDiscordMessageUtil.toJDA(message)).submit()
                .thenApply(msg -> ReceivedDiscordMessageImpl.fromJDA(discordSRV, msg));
    }

    @Override
    public CompletableFuture<Interaction> reply(SendableDiscordMessage message) {
        if (!(hook instanceof IReplyCallback)) {
            throw new IllegalStateException("This interaction cannot be replied to");
        }
        return ((IReplyCallback) hook).reply(SendableDiscordMessageUtil.toJDA(message)).submit()
                .thenApply(hook -> new InteractionImpl(discordSRV, hook, user));
    }

    @Override
    public CompletableFuture<Interaction> replyEphemeral(SendableDiscordMessage message) {
        if (!(hook instanceof IReplyCallback)) {
            throw new IllegalStateException("This interaction cannot be replied to");
        }
        return ((IReplyCallback) hook).reply(SendableDiscordMessageUtil.toJDA(message)).setEphemeral(true).submit()
                .thenApply(hook -> new InteractionImpl(discordSRV, hook, user));
    }

    @Override
    public CompletableFuture<Void> replyModal(Modal modal) {
        if (!(hook instanceof IModalCallback)) {
            throw new IllegalStateException("This interaction cannot be replied to");
        }
        return ((IModalCallback) hook).replyModal(modal.asJDA()).submit();
    }
}
