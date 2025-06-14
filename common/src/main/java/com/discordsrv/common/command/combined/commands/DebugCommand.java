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

package com.discordsrv.common.command.combined.commands;

import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.abstraction.CombinedCommand;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.combined.abstraction.Text;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.paste.Paste;
import com.discordsrv.common.core.paste.PasteService;
import com.discordsrv.common.core.paste.service.AESEncryptedPasteService;
import com.discordsrv.common.core.paste.service.BytebinPasteService;
import com.discordsrv.common.core.debug.DebugGenerateEvent;
import com.discordsrv.common.core.debug.DebugObservabilityEvent;
import com.discordsrv.common.core.debug.DebugReport;
import com.discordsrv.common.core.debug.file.KeyValueDebugFile;
import com.discordsrv.common.permission.game.Permissions;
import com.discordsrv.common.util.ExceptionUtil;
import net.kyori.adventure.text.format.NamedTextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class DebugCommand extends CombinedCommand {

    private static final List<String> SUBCOMMANDS = Arrays.asList("start", "stop", "upload", "zip");

    private static DebugCommand INSTANCE;
    private static GameCommand GAME;
    private static DiscordCommand DISCORD;

    private static DebugCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new DebugCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            DebugCommand command = getInstance(discordSRV);
            GAME = GameCommand.literal("debug")
                    .requiredPermission(Permissions.COMMAND_DEBUG)
                    .executor(command)
                    .then(
                            GameCommand.stringWord("subcommand")
                                    .suggester((sender, previousArguments, currentInput) ->
                                                       SUBCOMMANDS.stream()
                                                               .filter(cmd -> cmd.startsWith(currentInput.toLowerCase(Locale.ROOT)))
                                                               .collect(Collectors.toList())
                                    )
                                    .executor(command)
                    );
        }

        return GAME;
    }

    public static DiscordCommand getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            DebugCommand command = getInstance(discordSRV);
            CommandOption.Builder optionBuilder = CommandOption.builder(
                    CommandOption.Type.STRING,
                    "subcommand",
                    String.join("/", SUBCOMMANDS)
            ).setRequired(false);
            for (String subCommand : SUBCOMMANDS) {
                optionBuilder = optionBuilder.addChoice(subCommand, subCommand);
            }

            DISCORD = DiscordCommand.chatInput(ComponentIdentifier.of("DiscordSRV", "debug"), "debug", "Create a debug report")
                    .addOption(optionBuilder.build())
                    .setEventHandler(command)
                    .build();
        }

        return DISCORD;
    }

    private static final String URL_FORMAT = DiscordSRV.WEBSITE + "/debug/%s#%s";
    public static final Base64.Encoder KEY_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final AtomicBoolean debugObserving = new AtomicBoolean(false);
    private final Logger logger;
    private final PasteService pasteService;

    public DebugCommand(DiscordSRV discordSRV) {
        super(discordSRV);
        this.logger = new NamedLogger(discordSRV, "DEBUG_COMMAND");
        this.pasteService = new AESEncryptedPasteService(new BytebinPasteService(discordSRV, "https://bytebin.lucko.me") /* TODO: final store tbd */, 128);
        discordSRV.eventBus().subscribe(this);
    }

    @Subscribe(priority = EventPriorities.EARLIEST)
    public void onDiscordSRVShuttingDown(DiscordSRVShuttingDownEvent event) {
        compareAndSetDebugObservability(false);
    }

    @Subscribe
    public void onDebugGenerate(DebugGenerateEvent event) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("observing", debugObserving.get());

        event.addFile(-100, "debug.json", new KeyValueDebugFile(values));
    }

    @Override
    public void execute(CommandExecution execution) {
        String argument = execution.getString("subcommand");
        String subCommand = argument != null ? argument.toLowerCase(Locale.ROOT) : null;
        if (subCommand != null && !SUBCOMMANDS.contains(subCommand)) {
            execution.send(new Text("Unknown subcommand").withGameColor(NamedTextColor.RED));
            return;
        }

        execution.runAsync(() -> handle(execution, subCommand));
    }

    private void handle(CommandExecution execution, String subCommand) {
        if ("start".equals(subCommand)) {
            if (!compareAndSetDebugObservability(true)) {
                execution.send(new Text("Debug observing is already enabled").withGameColor(NamedTextColor.RED));
                return;
            }

            execution.send(new Text("Debug observing started").withGameColor(NamedTextColor.GREEN));
            return;
        }

        boolean useUpload = subCommand == null || "upload".equals(subCommand);
        boolean useZip = subCommand == null || "zip".equals(subCommand);
        if (useUpload || useZip) {
            DebugReport report = new DebugReport(discordSRV);
            report.generate();

            Throwable pasteError = useUpload ? paste(execution, report) : null;
            if (useUpload && pasteError == null) {
                useZip = false;
            }

            Throwable zipError = useZip ? zip(execution, report) : null;
            if (pasteError != null || zipError != null) {
                // Failed
                RuntimeException exception = ExceptionUtil.minifyException(new RuntimeException("Failed to save debug"));
                if (pasteError != null) {
                    exception.addSuppressed(pasteError);
                }
                if (zipError != null) {
                    exception.addSuppressed(zipError);
                }

                logger.error("Failed to save debug", exception);
                execution.send(new Text("Failed to save debug").withGameColor(NamedTextColor.DARK_RED));
                return;
            }
        }

        if (compareAndSetDebugObservability(false)) {
            execution.send(new Text("Debug observing stopped").withGameColor(NamedTextColor.GREEN));
        } else if ("stop".equals(subCommand)) {
            execution.send(new Text("Not debug observing").withGameColor(NamedTextColor.RED));
        }
    }

    private boolean compareAndSetDebugObservability(boolean newValue) {
        boolean changed = debugObserving.compareAndSet(!newValue, newValue);
        if (!changed) {
            return false;
        }

        logger.debug("Debug observability changed to " + newValue);
        discordSRV.eventBus().publish(new DebugObservabilityEvent(newValue));
        return true;
    }

    private Throwable paste(CommandExecution execution, DebugReport report) {
        try {
            Paste paste = report.upload(pasteService);
            String key = new String(KEY_ENCODER.encode(paste.decryptionKey()), StandardCharsets.UTF_8);
            String url = String.format(URL_FORMAT, paste.id(), key);

            execution.send(new Text(url));
            return null;
        } catch (Throwable e) {
            return e;
        }
    }

    private Throwable zip(CommandExecution execution, DebugReport report) {
        try {
            Path zip = report.zip();
            Path relative = discordSRV.dataDirectory().resolve("../..").relativize(zip);
            execution.send(
                    new Text("Debug generated to zip ")
                            .withGameColor(NamedTextColor.GRAY),
                    new Text(relative.toString())
                            .withGameColor(NamedTextColor.GREEN)
                            .withDiscordFormatting(Text.Formatting.BOLD)
            );
            return null;
        } catch (Throwable e) {
            return e;
        }
    }

}
