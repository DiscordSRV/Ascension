package com.discordsrv.common.command.discord.commands.subcommand;

import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.events.interaction.command.DiscordChatInputInteractionEvent;
import com.discordsrv.api.discord.events.interaction.command.DiscordCommandAutoCompleteInteractionEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

public class ExecuteCommand implements Consumer<DiscordChatInputInteractionEvent>, DiscordCommand.AutoCompleteHandler {

    private static DiscordCommand INSTANCE;

    public static DiscordCommand get(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            ExecuteCommand command = new ExecuteCommand(discordSRV);
            INSTANCE = DiscordCommand.chatInput(ComponentIdentifier.of("DiscordSRV", "execute"), "execute", "Run a Minecraft console command")
                    .addOption(
                            CommandOption.builder(CommandOption.Type.STRING, "command", "The command to execute")
                                    .setAutoComplete(true)
                                    .setRequired(true)
                                    .build()
                    )
                    .setAutoCompleteHandler(command)
                    .setEventHandler(command)
                    .build();
        }

        return INSTANCE;
    }

    private final DiscordSRV discordSRV;
    private final Logger logger;

    public ExecuteCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "EXECUTE_COMMAND");
    }

    @Override
    public void accept(DiscordChatInputInteractionEvent event) {
        OptionMapping mapping = event.asJDA().getOption("command");
        if (mapping == null) {
            return;
        }

        String command = mapping.getAsString();
        discordSRV.logger().error("> " + command);
    }

    @Override
    public void autoComplete(DiscordCommandAutoCompleteInteractionEvent event) {
        OptionMapping mapping = event.asJDA().getOption("command");
        if (mapping == null) {
            return;
        }

        String command = mapping.getAsString();
        List<String> parts = new ArrayList<>(Arrays.asList(command.split(" ")));

        AutoCompleteHelper helper = discordSRV.autoCompleteHelper();
        if (helper == null) {
            // No suggestions available.
            return;
        }

        List<String> suggestions = getSuggestions(helper, parts);
        if (suggestions == null) {
            return;
        }

        if (suggestions.isEmpty() || suggestions.contains(command)) {
            parts.add("");

            List<String> newSuggestions = getSuggestions(helper, parts);
            if (newSuggestions == null) {
                return;
            }

            suggestions = new ArrayList<>(newSuggestions);
            if (suggestions.isEmpty()) {
                suggestions.add(command);
            }
        }

        for (String suggestion : suggestions) {
            if (event.getChoices().size() >= 25) break;

            event.addChoice(suggestion, suggestion);
        }
    }

    private List<String> getSuggestions(AutoCompleteHelper helper, List<String> parts) {
        try {
            return helper.suggestCommands(new ArrayList<>(parts)).get(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (TimeoutException e) {
            return null;
        } catch (ExecutionException e) {
            logger.error("Failed to suggest commands", e.getCause());
            return null;
        } catch (Throwable t) {
            logger.error("Failed to suggest commands", t);
            return null;
        }
    }

    public interface AutoCompleteHelper {

        CompletableFuture<List<String>> suggestCommands(List<String> parts);
    }
}
