package com.discordsrv.common.command.combined.abstraction;

import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.events.interaction.command.DiscordChatInputInteractionEvent;
import com.discordsrv.api.discord.events.interaction.command.DiscordCommandAutoCompleteInteractionEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.abstraction.GameCommandArguments;
import com.discordsrv.common.command.game.abstraction.GameCommandExecutor;
import com.discordsrv.common.command.game.abstraction.GameCommandSuggester;
import com.discordsrv.common.command.game.sender.ICommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public abstract class CombinedCommand
        implements
        GameCommandExecutor,
        GameCommandSuggester,
        Consumer<DiscordChatInputInteractionEvent>,
        DiscordCommand.AutoCompleteHandler
{

    protected final DiscordSRV discordSRV;

    public CombinedCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public void execute(ICommandSender sender, GameCommandArguments arguments, String label) {
        execute(new GameCommandExecution(discordSRV, sender, arguments, label));
    }

    @Override
    public void accept(DiscordChatInputInteractionEvent event) {
        execute(new DiscordCommandExecution(discordSRV, event));
    }

    public abstract void execute(CommandExecution execution);

    @Override
    public List<String> suggestValues(ICommandSender sender, GameCommandArguments previousArguments, String currentInput) {
        return suggest(new GameCommandExecution(discordSRV, sender, previousArguments, null), currentInput);
    }

    @Override
    public void autoComplete(DiscordCommandAutoCompleteInteractionEvent event) {
        List<String> suggestions = suggest(new DiscordCommandExecution(discordSRV, event), null);
        suggestions.forEach(suggestion -> event.addChoice(suggestion, suggestion));
    }

    public List<String> suggest(CommandExecution execution, @Nullable String input) {
        return Collections.emptyList();
    }
}
