/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.discord.api.channel;

import com.discordsrv.api.discord.api.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.api.exception.RestErrorResponseException;
import com.discordsrv.api.discord.api.exception.UnknownChannelException;
import com.discordsrv.api.discord.api.exception.UnknownMessageException;
import com.discordsrv.common.DiscordSRV;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public abstract class DiscordMessageChannelImpl implements DiscordMessageChannel {

    public static DiscordMessageChannelImpl get(DiscordSRV discordSRV, MessageChannel messageChannel) {
        if (messageChannel instanceof TextChannel) {
            return new DiscordTextChannelImpl(discordSRV, (TextChannel) messageChannel);
        } else if (messageChannel instanceof PrivateChannel) {
            return new DiscordDMChannelImpl(discordSRV, (PrivateChannel) messageChannel);
        } else {
            throw new IllegalArgumentException("Unknown MessageChannel type");
        }
    }

    @SuppressWarnings("Convert2Lambda") // SneakyThrows
    protected final <T> CompletableFuture<T> mapExceptions(CompletableFuture<T> future) {
        return future.handle(new BiFunction<T, Throwable, T>() {

            @SneakyThrows
            @Override
            public T apply(T msg, Throwable t) {
                if (t instanceof ErrorResponseException) {
                    ErrorResponse errorResponse = ((ErrorResponseException) t).getErrorResponse();
                    if (errorResponse != null) {
                        if (errorResponse == ErrorResponse.UNKNOWN_MESSAGE) {
                            throw new UnknownMessageException(t);
                        } else if (errorResponse == ErrorResponse.UNKNOWN_CHANNEL) {
                            throw new UnknownChannelException(t);
                        }
                    }
                    throw new RestErrorResponseException(((ErrorResponseException) t).getErrorCode(), t);
                } else if (t != null) {
                    throw t;
                }
                return msg;
            }
        });
    }
}
