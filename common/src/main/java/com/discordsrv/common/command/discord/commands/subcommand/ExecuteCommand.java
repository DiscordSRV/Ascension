package com.discordsrv.common.command.discord.commands.subcommand;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.events.interaction.command.DiscordChatInputInteractionEvent;
import com.discordsrv.api.discord.events.interaction.command.DiscordCommandAutoCompleteInteractionEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.GameCommandExecutionHelper;
import com.discordsrv.common.config.main.DiscordCommandConfig;
import com.discordsrv.common.config.main.generic.GameCommandFilterConfig;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.kyori.adventure.text.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class ExecuteCommand implements Consumer<DiscordChatInputInteractionEvent>, DiscordCommand.AutoCompleteHandler {

    private static DiscordCommand INSTANCE;

    public static DiscordCommand get(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            DiscordCommandConfig.ExecuteConfig config = discordSRV.config().discordCommand.execute;

            ExecuteCommand command = new ExecuteCommand(discordSRV);
            INSTANCE = DiscordCommand.chatInput(ComponentIdentifier.of("DiscordSRV", "execute"), "execute", "Run a Minecraft console command")
                    .addOption(
                            CommandOption.builder(CommandOption.Type.STRING, "command", "The command to execute")
                                    .setAutoComplete(config.suggest)
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
    private final GameCommandExecutionHelper helper;
    private final Logger logger;

    public ExecuteCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.helper = discordSRV.executeHelper();
        this.logger = new NamedLogger(discordSRV, "EXECUTE_COMMAND");
    }

    public boolean isNotAcceptableCommand(DiscordGuildMember member, DiscordUser user, String command, boolean suggestions) {
        DiscordCommandConfig.ExecuteConfig config = discordSRV.config().discordCommand.execute;

        for (GameCommandFilterConfig filter : config.filters) {
            if (!filter.isAcceptableCommand(member, user, command, suggestions, helper)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void accept(DiscordChatInputInteractionEvent event) {
        DiscordCommandConfig.ExecuteConfig config = discordSRV.config().discordCommand.execute;
        if (!config.enabled) {
            event.asJDA().reply("The execute command is disabled").setEphemeral(true).queue();
            return;
        }

        OptionMapping mapping = event.asJDA().getOption("command");
        if (mapping == null) {
            return;
        }

        String command = mapping.getAsString();
        if (isNotAcceptableCommand(event.getMember(), event.getUser(), command, false)) {
            event.asJDA().reply("You do not have permission to run that command").setEphemeral(true).queue();
            return;
        }

        boolean ephemeral = config.ephemeral;
        event.asJDA().reply("Executing command `" + command + "`")
                .setEphemeral(ephemeral)
                .queue(ih -> new ExecutionContext(discordSRV, ih, config.getOutputMode(), ephemeral).run(command));
    }

    @Override
    public void autoComplete(DiscordCommandAutoCompleteInteractionEvent event) {
        if (helper == null) {
            // No suggestions available.
            return;
        }

        DiscordCommandConfig.ExecuteConfig config = discordSRV.config().discordCommand.execute;
        if (!config.suggest) {
            return;
        }

        OptionMapping mapping = event.asJDA().getOption("command");
        if (mapping == null) {
            return;
        }

        String command = mapping.getAsString();
        List<String> parts = new ArrayList<>(Arrays.asList(command.split(" ")));

        List<String> suggestions = getSuggestions(parts);
        if (suggestions == null) {
            return;
        }

        if (suggestions.isEmpty() || suggestions.contains(command)) {
            parts.add("");

            List<String> newSuggestions = getSuggestions(parts);
            if (newSuggestions == null) {
                return;
            }

            suggestions = new ArrayList<>(newSuggestions);
            if (suggestions.isEmpty()) {
                suggestions.add(command);
            }
        }

        suggestions.sort((s1, s2) -> {
            // Options with semicolons (eg. plugin:command) are at the bottom
            int semi1 = s1.indexOf(':');
            int semi2 = s2.indexOf(':');
            if (semi1 > semi2) {
                return 1;
            } else if (semi2 > semi1) {
                return -1;
            }

            // Otherwise alphabetically sorted
            return s1.toLowerCase(Locale.ROOT).compareTo(s2.toLowerCase(Locale.ROOT));
        });
        for (String suggestion : suggestions) {
            if (event.getChoices().size() >= 25) {
                break;
            }
            if (config.filterSuggestions && isNotAcceptableCommand(event.getMember(), event.getUser(), suggestion, true)) {
                continue;
            }

            event.addChoice(suggestion, suggestion);
        }
    }

    private List<String> getSuggestions(List<String> parts) {
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

    private static class ExecutionContext {

        private final DiscordSRV discordSRV;
        private final InteractionHook hook;
        private final DiscordCommandConfig.OutputMode outputMode;
        private final boolean ephemeral;
        private ScheduledFuture<?> future;
        private final Queue<Component> queued = new LinkedBlockingQueue<>();

        public ExecutionContext(
                DiscordSRV discordSRV,
                InteractionHook hook,
                DiscordCommandConfig.OutputMode outputMode,
                boolean ephemeral
        ) {
            this.discordSRV = discordSRV;
            this.hook = hook;
            this.outputMode = outputMode;
            this.ephemeral = ephemeral;
        }

        public void run(String command) {
            discordSRV.console().commandExecutorProvider()
                    .getConsoleExecutor(this::consumeComponent)
                    .runCommand(command);
        }

        private void consumeComponent(Component component) {
            if (outputMode == DiscordCommandConfig.OutputMode.OFF) {
                return;
            }
            synchronized (queued) {
                queued.offer(component);
                if (future == null) {
                    future = discordSRV.scheduler().runLater(this::send, 500);
                }
            }
        }

        private void send() {
            boolean ansi = outputMode == DiscordCommandConfig.OutputMode.ANSI;
            boolean plainBlock = outputMode == DiscordCommandConfig.OutputMode.PLAIN_BLOCK;
            String prefix = ansi ? "```ansi\n" : (plainBlock ? "```\n" : "");
            String suffix = ansi ? "```" : (plainBlock ? "```" : "");

            String delimiter = "\n";
            StringJoiner joiner = new StringJoiner(delimiter);

            Component component;
            synchronized (queued) {
                while ((component = queued.poll()) != null) {
                    String discord;
                    switch (outputMode) {
                        default:
                        case MARKDOWN:
                            discord = discordSRV.componentFactory().discordSerializer().serialize(component);
                            break;
                        case ANSI:
                            discord = discordSRV.componentFactory().ansiSerializer().serialize(component);
                            break;
                        case PLAIN:
                        case PLAIN_BLOCK:
                            discord = discordSRV.componentFactory().plainSerializer().serialize(component);
                            break;
                    }

                    if (prefix.length() + suffix.length() + discord.length() + joiner.length() + delimiter.length() > Message.MAX_CONTENT_LENGTH) {
                        hook.sendMessage(prefix + joiner + suffix).setEphemeral(ephemeral).queue();
                        joiner = new StringJoiner(delimiter);
                    }

                    joiner.add(discord);
                }
                future = null;
            }
            hook.sendMessage(prefix + joiner + suffix).setEphemeral(ephemeral).queue();
        }

    }
}
