package com.discordsrv.api.discord.entity.interaction.command;

import com.discordsrv.api.discord.entity.JDAEntity;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public enum CommandType implements JDAEntity<Command.Type> {

    CHAT_INPUT(Command.Type.SLASH, Commands.MAX_SLASH_COMMANDS),
    USER(Command.Type.USER, Commands.MAX_USER_COMMANDS),
    MESSAGE(Command.Type.MESSAGE, Commands.MAX_MESSAGE_COMMANDS);

    private final Command.Type jda;
    private final int maximumCount;

    CommandType(Command.Type jda, int maximumCount) {
        this.jda = jda;
        this.maximumCount = maximumCount;
    }

    @Override
    public Command.Type asJDA() {
        return jda;
    }

    public int getMaximumCount() {
        return maximumCount;
    }
}
