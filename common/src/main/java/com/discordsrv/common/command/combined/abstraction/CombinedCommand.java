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

import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.event.events.discord.interaction.command.DiscordChatInputInteractionEvent;
import com.discordsrv.api.event.events.discord.interaction.command.DiscordCommandAutoCompleteInteractionEvent;
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
