/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.api.discord.connection.details.DiscordGatewayIntent;
import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.interaction.DiscordInteractionHook;
import com.discordsrv.api.discord.entity.interaction.command.CommandType;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.command.SubCommandGroup;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.Event;
import com.discordsrv.api.events.discord.interaction.DiscordModalInteractionEvent;
import com.discordsrv.api.events.discord.interaction.command.*;
import com.discordsrv.api.events.discord.interaction.component.DiscordSelectMenuInteractionEvent;
import com.discordsrv.api.events.discord.member.role.DiscordMemberRoleAddEvent;
import com.discordsrv.api.events.discord.member.role.DiscordMemberRoleRemoveEvent;
import com.discordsrv.api.events.discord.message.DiscordMessageDeleteEvent;
import com.discordsrv.api.events.discord.message.DiscordMessageReceiveEvent;
import com.discordsrv.api.events.discord.message.DiscordMessageUpdateEvent;
import com.discordsrv.api.module.Module;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.discord.api.entity.component.DiscordInteractionHookImpl;
import com.discordsrv.common.discord.api.entity.message.ReceivedDiscordMessageImpl;
import com.discordsrv.common.events.discord.interaction.command.DiscordChatInputInteractionEventImpl;
import com.discordsrv.common.events.discord.interaction.command.DiscordMessageContextInteractionEventImpl;
import com.discordsrv.common.events.discord.interaction.command.DiscordUserContextInteractionEventImpl;
import com.discordsrv.common.events.discord.interaction.component.DiscordButtonInteractionEventImpl;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.*;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericSelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.interactions.callbacks.IDeferrableCallback;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DiscordAPIEventModule extends AbstractModule<DiscordSRV> {

    public DiscordAPIEventModule(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    private DiscordAPIImpl api() {
        return discordSRV.discordAPI();
    }

    /**
     * See {@link Module#requiredIntents()} implementation.
     * @return no intents as these events might not be listened to
     */
    @Override
    public @NotNull Collection<DiscordGatewayIntent> requiredIntents() {
        return Collections.emptyList();
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
            DiscordCommand command = discordSRV.discordAPI().getActiveCommand(
                    ((CommandAutoCompleteInteractionEvent) event).isGuildCommand() ? event.getGuild() : null,
                    CommandType.CHAT_INPUT,
                    ((CommandAutoCompleteInteractionEvent) event).getName()
            ).orElse(null);
            if (command == null) {
                return;
            }
            command = mapCommand(
                    command,
                    ((CommandAutoCompleteInteractionEvent) event).getSubcommandGroup(),
                    ((CommandAutoCompleteInteractionEvent) event).getSubcommandName()
            );

            DiscordCommandAutoCompleteInteractionEvent autoComplete = new DiscordCommandAutoCompleteInteractionEvent(
                    (CommandAutoCompleteInteractionEvent) event, command.getId(), user, guildMember, channel);
            discordSRV.eventBus().publish(autoComplete);
            DiscordCommand.AutoCompleteHandler autoCompleteHandler = command.getAutoCompleteHandler();
            if (autoCompleteHandler != null) {
                autoCompleteHandler.autoComplete(autoComplete);
            }

            List<Command.Choice> choices = new ArrayList<>();
            for (Map.Entry<String, Object> entry : autoComplete.getChoices().entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof String) {
                    choices.add(new Command.Choice(name, (String) value));
                } else if (value instanceof Double || value instanceof Float) {
                    choices.add(new Command.Choice(name, ((Number) value).doubleValue()));
                } else {
                    choices.add(new Command.Choice(name, ((Number) value).longValue()));
                }
            }
            ((CommandAutoCompleteInteractionEvent) event).replyChoices(choices).queue();
            return;
        }

        DiscordInteractionHook hook = new DiscordInteractionHookImpl(discordSRV, ((IDeferrableCallback) event).getHook());
        Event newEvent = null;
        if (event instanceof GenericCommandInteractionEvent) {
            Guild guild = ((GenericCommandInteractionEvent) event).isGuildCommand() ? event.getGuild() : null;
            String name = ((GenericCommandInteractionEvent) event).getName();
            if (event instanceof MessageContextInteractionEvent) {
                DiscordCommand command = discordSRV.discordAPI()
                        .getActiveCommand(guild, CommandType.MESSAGE, name).orElse(null);
                if (command == null) {
                    return;
                }

                DiscordMessageContextInteractionEvent interactionEvent = new DiscordMessageContextInteractionEventImpl(
                        discordSRV,
                        (MessageContextInteractionEvent) event,
                        command.getId(),
                        user,
                        guildMember,
                        channel,
                        hook
                );

                newEvent = interactionEvent;
                Consumer<AbstractCommandInteractionEvent<?>> eventHandler = command.getEventHandler();
                if (eventHandler != null) {
                    eventHandler.accept(interactionEvent);
                }
            } else if (event instanceof UserContextInteractionEvent) {
                DiscordCommand command = discordSRV.discordAPI()
                        .getActiveCommand(guild, CommandType.USER, name).orElse(null);
                if (command == null) {
                    return;
                }

                DiscordUserContextInteractionEvent interactionEvent = new DiscordUserContextInteractionEventImpl(
                        discordSRV,
                        (UserContextInteractionEvent) event,
                        command.getId(),
                        user,
                        guildMember,
                        channel,
                        hook
                );

                newEvent = interactionEvent;
                Consumer<AbstractCommandInteractionEvent<?>> eventHandler = command.getEventHandler();
                if (eventHandler != null) {
                    eventHandler.accept(interactionEvent);
                }
            } else if (event instanceof SlashCommandInteractionEvent) {
                DiscordCommand command = discordSRV.discordAPI()
                        .getActiveCommand(guild, CommandType.CHAT_INPUT, name).orElse(null);
                if (command == null) {
                    return;
                }
                command = mapCommand(
                        command,
                        ((SlashCommandInteractionEvent) event).getSubcommandGroup(),
                        ((SlashCommandInteractionEvent) event).getSubcommandName()
                );

                DiscordChatInputInteractionEvent interactionEvent = new DiscordChatInputInteractionEventImpl(
                        discordSRV,
                        (SlashCommandInteractionEvent) event,
                        command.getId(),
                        user,
                        guildMember,
                        channel,
                        hook
                );

                newEvent = interactionEvent;
                Consumer<AbstractCommandInteractionEvent<?>> eventHandler = command.getEventHandler();
                if (eventHandler != null) {
                    eventHandler.accept(interactionEvent);
                }
            }
        } else if (event instanceof GenericComponentInteractionCreateEvent) {
            ComponentIdentifier identifier = ComponentIdentifier.parseFromDiscord(
                    ((GenericComponentInteractionCreateEvent) event).getComponentId());
            if (identifier == null) {
                return;
            }

            if (event instanceof ButtonInteractionEvent) {
                newEvent = new DiscordButtonInteractionEventImpl(
                        discordSRV,
                        (ButtonInteractionEvent) event,
                        identifier,
                        user,
                        guildMember,
                        channel,
                        hook
                );
            } else if (event instanceof GenericSelectMenuInteractionEvent) {
                newEvent = new DiscordSelectMenuInteractionEvent(
                        (GenericSelectMenuInteractionEvent<?, ?>) event, identifier, user, guildMember, channel, hook);
            }
        } else if (event instanceof ModalInteractionEvent) {
            ComponentIdentifier identifier = ComponentIdentifier.parseFromDiscord(
                    ((ModalInteractionEvent) event).getModalId());
            if (identifier == null) {
                return;
            }

            newEvent = new DiscordModalInteractionEvent((ModalInteractionEvent) event, identifier, user, guildMember, channel, hook);
        }
        if (newEvent != null) {
            discordSRV.eventBus().publish(newEvent);
        }
    }

    private DiscordCommand mapCommand(DiscordCommand command, String subCommandGroupName, String subCommandName) {
        if (subCommandGroupName != null) {
            for (SubCommandGroup group : command.getSubCommandGroups()) {
                if (group.getName().equals(subCommandGroupName)) {
                    for (DiscordCommand subCommand : group.getCommands()) {
                        if (subCommand.getName().equals(subCommandName)) {
                            command = subCommand;
                            break;
                        }
                    }
                    break;
                }
            }
        } else if (subCommandName != null) {
            for (DiscordCommand subCommand : command.getSubCommands()) {
                if (subCommandName.equals(subCommand.getName())) {
                    command = subCommand;
                    break;
                }
            }
        }
        return command;
    }
}
