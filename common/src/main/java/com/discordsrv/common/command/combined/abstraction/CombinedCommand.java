package com.discordsrv.common.command.combined.abstraction;

import com.discordsrv.api.discord.events.interaction.command.DiscordChatInputInteractionEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.abstraction.GameCommandArguments;
import com.discordsrv.common.command.game.abstraction.GameCommandExecutor;
import com.discordsrv.common.command.game.sender.ICommandSender;

import java.util.function.Consumer;

public abstract class CombinedCommand implements GameCommandExecutor, Consumer<DiscordChatInputInteractionEvent> {

    protected final DiscordSRV discordSRV;

    public CombinedCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public void execute(ICommandSender sender, GameCommandArguments arguments) {
        execute(new GameCommandExecution(discordSRV, sender, arguments));
    }

    @Override
    public void accept(DiscordChatInputInteractionEvent event) {
        execute(new DiscordCommandExecution(discordSRV, event));
    }

    public abstract void execute(CommandExecution execution);

}
