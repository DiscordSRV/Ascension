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

import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.interaction.command.CommandType;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.discord.interaction.command.CommandRegisterEvent;
import com.discordsrv.api.events.lifecycle.DiscordSRVReadyEvent;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DiscordCommandRegistry {

    private static final Long GLOBAL_ID = -1L;

    private final Map<Long, Map<CommandType, Registry>> registries = new ConcurrentHashMap<>();
    private final DiscordSRV discordSRV;

    public DiscordCommandRegistry(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        discordSRV.eventBus().subscribe(this);
    }

    @Subscribe
    public void onDiscordSRVReady(DiscordSRVReadyEvent event) {
        reloadCommands();
    }

    public void reloadCommands() {
        registerCommandsFromEvent();
        registerCommandsToDiscord();
    }

    private void registerCommandsFromEvent() {
        CommandRegisterEvent event = new CommandRegisterEvent();
        discordSRV.eventBus().publish(event);

        List<DiscordCommand> commands = event.getCommands();
        for (Map<CommandType, Registry> registryMap : registries.values()) {
            registryMap.values().forEach(registry -> registry.removeIf(reg -> reg.isTemporary() && !commands.contains(reg.getCommand())));
        }

        commands.forEach(cmd -> register(cmd, true));
    }

    public DiscordCommand.RegistrationResult register(DiscordCommand command, boolean temporary) {
        CommandType type = command.getType();
        Long guildId = command.getGuildId();
        Registry registry = registries
                .computeIfAbsent(guildId != null ? guildId : GLOBAL_ID, key -> new EnumMap<>(CommandType.class))
                .computeIfAbsent(type, key -> new Registry());
        if (registry.contains(command)) {
            return DiscordCommand.RegistrationResult.ALREADY_REGISTERED;
        }

        boolean first = registry.register(command, temporary);
        if (!first) {
            return DiscordCommand.RegistrationResult.NAME_ALREADY_IN_USE;
        }
        if (registry.getInTimeOrder().indexOf(command) >= type.getMaximumCount()) {
            return DiscordCommand.RegistrationResult.TOO_MANY_COMMANDS;
        }
        return DiscordCommand.RegistrationResult.REGISTERED;
    }

    public void unregister(DiscordCommand command) {
        Long guildId = command.getGuildId();
        Registry registry = registries
                .computeIfAbsent(guildId != null ? guildId : GLOBAL_ID, key -> Collections.emptyMap())
                .get(command.getType());

        if (registry != null) {
            registry.unregister(command);
        }
    }

    @Nullable
    public DiscordCommand getActive(Long guildId, CommandType type, String name) {
        Registry registry = registries
                .computeIfAbsent(guildId != null ? guildId : GLOBAL_ID, key -> Collections.emptyMap())
                .get(type);
        if (registry == null) {
            return null;
        }
        return registry.getActive(name);
    }

    private void registerCommandsToDiscord() {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            return;
        }

        List<Long> ids = new ArrayList<>();
        ids.add(GLOBAL_ID);
        for (Guild guild : jda.getGuilds()) {
            ids.add(guild.getIdLong());
        }

        for (long guildId : ids) {
            Map<CommandType, Registry> commandsByType = registries.getOrDefault(guildId, Collections.emptyMap());
            Map<CommandType, Set<DiscordCommand>> commandsToRegister = new EnumMap<>(CommandType.class);

            boolean updateNeeded = false;
            for (Map.Entry<CommandType, Registry> entry : commandsByType.entrySet()) {
                Registry registry = entry.getValue();

                List<DiscordCommand> commands = registry.getInTimeOrder();
                Set<DiscordCommand> currentCommands = new LinkedHashSet<>();
                int max = Math.min(commands.size(), entry.getKey().getMaximumCount());
                for (int i = 0; i < max; i++) {
                    DiscordCommand command = commands.get(i);
                    currentCommands.add(command);
                }

                commandsToRegister.put(entry.getKey(), currentCommands);

                Collection<DiscordCommand> activeCommands = registry.activeCommands.values();
                if (activeCommands.size() != currentCommands.size() || !currentCommands.containsAll(activeCommands)) {
                    updateNeeded = true;
                }
            }

            if (updateNeeded) {
                CommandListUpdateAction action;
                if (Objects.equals(guildId, GLOBAL_ID)) {
                    action = jda.updateCommands();
                } else {
                    Guild guild = jda.getGuildById(guildId);
                    if (guild == null) {
                        continue;
                    }
                    action = guild.updateCommands();
                }

                List<DiscordCommand> allCommands = new ArrayList<>();
                commandsToRegister.values().forEach(allCommands::addAll);
                action.addCommands(allCommands.stream().map(JDAEntity::asJDA).collect(Collectors.toList()))
                        .queue(v -> {
                            for (CommandType value : CommandType.values()) {
                                Registry registry = commandsByType.get(value);
                                if (registry != null) {
                                    registry.putActiveCommands(commandsToRegister.get(value));
                                }
                            }
                        });
            }
        }
    }

    private static class Registry {

        private final Map<String, List<Registration>> registry = new ConcurrentHashMap<>();
        private final Map<String, DiscordCommand> activeCommands = new HashMap<>();

        public void removeIf(Predicate<Registration> commandPredicate) {
            List<String> removeKeys = new ArrayList<>();
            for (Map.Entry<String, List<Registration>> entry : registry.entrySet()) {
                List<Registration> registrations = entry.getValue();
                registrations.removeIf(commandPredicate);
                if (registrations.isEmpty()) {
                    removeKeys.add(entry.getKey());
                }
            }
            removeKeys.forEach(registry::remove);
        }

        public boolean contains(@NotNull DiscordCommand command) {
            List<Registration> commands = registry.get(command.getName());
            if (commands == null) {
                return false;
            }

            return commands.stream().anyMatch(reg -> reg.getCommand() == command);
        }

        public boolean register(@NotNull DiscordCommand command, boolean temporary) {
            List<Registration> commands = registry.computeIfAbsent(command.getName(), key -> new CopyOnWriteArrayList<>());
            boolean empty = commands.isEmpty();
            commands.add(new Registration(command, temporary));
            return empty;
        }

        public void unregister(@NotNull DiscordCommand command) {
            List<Registration> commands = registry.get(command.getName());
            if (commands == null) {
                return;
            }

            commands.removeIf(reg -> reg.command == command);
            if (commands.isEmpty()) {
                registry.remove(command.getName());
            }
        }

        public void putActiveCommands(Set<DiscordCommand> commands) {
            synchronized (activeCommands) {
                activeCommands.clear();
                for (DiscordCommand command : commands) {
                    activeCommands.put(command.getName(), command);
                }
            }
        }

        public List<DiscordCommand> getInTimeOrder() {
            List<Registration> registrations = registry.values().stream()
                    .map(list -> list.get(0))
                    .collect(Collectors.toList());

            return registrations.stream()
                    .sorted(Comparator.comparingLong(Registration::getTime))
                    .map(Registration::getCommand)
                    .collect(Collectors.toList());
        }

        @Nullable
        public DiscordCommand getActive(String name) {
            synchronized (activeCommands) {
                return activeCommands.get(name);
            }
        }
    }

    private static class Registration {

        private final DiscordCommand command;
        private final long time;
        private final boolean temporary;

        public Registration(DiscordCommand command, boolean temporary) {
            this.command = command;
            this.time = System.currentTimeMillis();
            this.temporary = temporary;
        }

        public DiscordCommand getCommand() {
            return command;
        }

        public long getTime() {
            return time;
        }

        public boolean isTemporary() {
            return temporary;
        }
    }
}
