/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.command.combined.abstraction;

import com.discordsrv.api.event.events.discord.interaction.command.DiscordChatInputInteractionEvent;
import com.discordsrv.api.event.events.discord.interaction.command.DiscordCommandAutoCompleteInteractionEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.messages.MessagesConfig;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class DiscordCommandExecution implements CommandExecution {

    private final DiscordSRV discordSRV;

    private final GenericInteractionCreateEvent createEvent;
    private final CommandInteractionPayload interactionPayload;
    private final IReplyCallback replyCallback;

    private final AtomicBoolean isEphemeral = new AtomicBoolean(true);
    private final AtomicReference<InteractionHook> hook = new AtomicReference<>();

    public DiscordCommandExecution(DiscordSRV discordSRV, DiscordChatInputInteractionEvent event) {
        this.discordSRV = discordSRV;
        this.createEvent = event.asJDA();
        this.interactionPayload = event.asJDA();
        this.replyCallback = event.asJDA();
    }

    public DiscordCommandExecution(DiscordSRV discordSRV, DiscordCommandAutoCompleteInteractionEvent event) {
        this.discordSRV = discordSRV;
        this.createEvent = event.asJDA();
        this.interactionPayload = event.asJDA();
        this.replyCallback = null;
    }

    @Override
    public Locale locale() {
        return createEvent.getUserLocale().toLocale();
    }

    @Override
    public MessagesConfig messages() {
        return discordSRV.messagesConfig(locale());
    }

    @Override
    public void setEphemeral(boolean ephemeral) {
        isEphemeral.set(ephemeral);
    }

    @Override
    public String getArgument(String label) {
        OptionMapping mapping = interactionPayload.getOption(label);
        return mapping != null ? mapping.getAsString() : null;
    }

    @Override
    public void send(Collection<Text> texts, Collection<Text> extra) {
        StringBuilder builder = new StringBuilder();
        EnumMap<Text.Formatting, Boolean> formats = new EnumMap<>(Text.Formatting.class);

        for (Text text : texts) {
            render(text, builder, formats);
        }
        verifyStyle(builder, formats, null);

        if (!extra.isEmpty()) {
            builder.append("\n\n");
            for (Text text : extra) {
                render(text, builder, formats);
            }
            verifyStyle(builder, formats, null);
        }

        sendResponse(builder.toString());
    }

    @Override
    public void send(Component minecraft, String discord) {
        sendResponse(discord);
    }

    private void sendResponse(String content) {
        if (replyCallback == null) {
            throw new IllegalStateException("May not be used on auto completions");
        }

        InteractionHook interactionHook = hook.get();
        boolean ephemeral = isEphemeral.get();
        if (interactionHook != null) {
            interactionHook.sendMessage(content).setEphemeral(ephemeral).queue();
        } else {
            replyCallback.reply(content).setEphemeral(ephemeral).queue();
        }
    }

    private void render(Text text, StringBuilder builder, EnumMap<Text.Formatting, Boolean> formats) {
        if (StringUtils.isEmpty(text.content())) return;

        verifyStyle(builder, formats, text);
        builder.append(text.content());
    }

    private void verifyStyle(StringBuilder builder, EnumMap<Text.Formatting, Boolean> formats, Text text) {
        for (Text.Formatting format : Text.Formatting.values()) {
            boolean is = formats.computeIfAbsent(format, key -> false);
            boolean thisIs = text != null && text.discordFormatting().contains(format);

            if (is != thisIs) {
                // should end or start
                builder.append(format.discord());
                formats.put(format, thisIs);
            }
        }
    }

    @Override
    public void runAsync(Runnable runnable) {
        replyCallback.deferReply(isEphemeral.get()).queue(ih -> {
            hook.set(ih);
            discordSRV.scheduler().run(runnable);
        });
    }

    public User getUser() {
        return createEvent.getUser();
    }
}
