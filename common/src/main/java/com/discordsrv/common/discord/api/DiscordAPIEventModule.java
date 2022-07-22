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

package com.discordsrv.common.discord.api;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.interaction.DiscordInteractionHook;
import com.discordsrv.api.discord.events.interaction.DiscordModalInteractionEvent;
import com.discordsrv.api.discord.events.interaction.command.DiscordCommandAutoCompleteInteractionEvent;
import com.discordsrv.api.discord.events.interaction.command.DiscordMessageContextInteractionEvent;
import com.discordsrv.api.discord.events.interaction.command.DiscordUserContextInteractionEvent;
import com.discordsrv.api.discord.events.interaction.command.DiscordChatInputInteractionEvent;
import com.discordsrv.api.discord.events.interaction.component.DiscordButtonInteractionEvent;
import com.discordsrv.api.discord.events.interaction.component.DiscordSelectMenuInteractionEvent;
import com.discordsrv.api.discord.events.member.role.DiscordMemberRoleAddEvent;
import com.discordsrv.api.discord.events.member.role.DiscordMemberRoleRemoveEvent;
import com.discordsrv.api.discord.events.message.DiscordMessageDeleteEvent;
import com.discordsrv.api.discord.events.message.DiscordMessageReceiveEvent;
import com.discordsrv.api.discord.events.message.DiscordMessageUpdateEvent;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.api.event.events.Event;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.component.DiscordInteractionHookImpl;
import com.discordsrv.common.discord.api.entity.message.ReceivedDiscordMessageImpl;
import com.discordsrv.common.module.type.AbstractModule;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IDeferrableCallback;

import java.util.stream.Collectors;

public class DiscordAPIEventModule extends AbstractModule<DiscordSRV> {

    public DiscordAPIEventModule(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    private DiscordAPIImpl api() {
        return discordSRV.discordAPI();
    }

    @Subscribe
    public void onMessageReceived(MessageReceivedEvent event) {
        discordSRV.eventBus().publish(new DiscordMessageReceiveEvent(
                event,
                api().getMessageChannel(event.getChannel()),
                ReceivedDiscordMessageImpl.fromJDA(discordSRV, event.getMessage())
        ));
    }

    @Subscribe
    public void onMessageUpdate(MessageUpdateEvent event) {
        discordSRV.eventBus().publish(new DiscordMessageUpdateEvent(
                event,
                api().getMessageChannel(event.getChannel()),
                ReceivedDiscordMessageImpl.fromJDA(discordSRV, event.getMessage())
        ));
    }

    @Subscribe
    public void onMessageDelete(MessageDeleteEvent event) {
        discordSRV.eventBus().publish(new DiscordMessageDeleteEvent(
                event,
                api().getMessageChannel(event.getChannel()),
                event.getMessageIdLong()
        ));
    }

    @Subscribe
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        discordSRV.eventBus().publish(new DiscordMemberRoleAddEvent(
                event,
                api().getGuildMember(event.getMember()),
                event.getRoles().stream().map(role -> api().getRole(role)).collect(Collectors.toList())
        ));
    }

    @Subscribe
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        discordSRV.eventBus().publish(new DiscordMemberRoleRemoveEvent(
                event,
                api().getGuildMember(event.getMember()),
                event.getRoles().stream().map(role -> api().getRole(role)).collect(Collectors.toList())
        ));
    }

    @Subscribe
    public void onGenericInteractionCreate(GenericInteractionCreateEvent event) {
        if (event.getChannel() == null || !event.getChannel().getType().isMessage()
                || (!(event instanceof CommandAutoCompleteInteractionEvent) && !(event instanceof IDeferrableCallback))) {
            return;
        }

        Member member = event.getMember();

        DiscordUser user = api().getUser(event.getUser());
        DiscordGuildMember guildMember = member != null ? api().getGuildMember(member) : null;
        DiscordMessageChannel channel = api().getMessageChannel(event.getMessageChannel());
        if (event instanceof CommandAutoCompleteInteractionEvent) {
            discordSRV.eventBus().publish(new DiscordCommandAutoCompleteInteractionEvent(
                    (CommandAutoCompleteInteractionEvent) event, user, guildMember, channel));
            return;
        }

        DiscordInteractionHook hook = new DiscordInteractionHookImpl(discordSRV, ((IDeferrableCallback) event).getHook());
        Event newEvent;
        if (event instanceof MessageContextInteractionEvent) {
            newEvent = new DiscordMessageContextInteractionEvent((MessageContextInteractionEvent) event, user, guildMember, channel, hook);
        } else if (event instanceof UserContextInteractionEvent) {
            newEvent = new DiscordUserContextInteractionEvent((UserContextInteractionEvent) event, user, guildMember, channel, hook);
        } else if (event instanceof SlashCommandInteractionEvent) {
            newEvent = new DiscordChatInputInteractionEvent((SlashCommandInteractionEvent) event, user, guildMember, channel, hook);
        } else if (event instanceof ButtonInteractionEvent) {
            newEvent = new DiscordButtonInteractionEvent((ButtonInteractionEvent) event, user, guildMember, channel, hook);
        } else if (event instanceof SelectMenuInteractionEvent) {
            newEvent = new DiscordSelectMenuInteractionEvent((SelectMenuInteractionEvent) event, user, guildMember, channel, hook);
        } else if (event instanceof ModalInteractionEvent) {
            newEvent = new DiscordModalInteractionEvent((ModalInteractionEvent) event, user, guildMember, channel, hook);
        } else {
            return;
        }
        discordSRV.eventBus().publish(newEvent);
    }
}
