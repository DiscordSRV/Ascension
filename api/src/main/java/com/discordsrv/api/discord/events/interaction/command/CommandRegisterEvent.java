package com.discordsrv.api.discord.events.interaction.command;

import com.discordsrv.api.discord.entity.interaction.command.Command;
import com.discordsrv.api.event.events.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An event for registering {@link com.discordsrv.api.discord.entity.interaction.command.Command}s,
 * an alternative to {@link com.discordsrv.api.discord.DiscordAPI#registerCommand(Command)}.
 */
public class CommandRegisterEvent implements Event {

    private final List<Command> commands = new ArrayList<>();

    /**
     * Add events to be registered.
     * @param commands the commands to be registered, use of the same command instances is recommended
     */
    public void registerCommands(@NotNull Command... commands) {
        this.commands.addAll(Arrays.asList(commands));
    }

    @NotNull
    @Unmodifiable
    public List<Command> getCommands() {
        return Collections.unmodifiableList(commands);
    }
}
