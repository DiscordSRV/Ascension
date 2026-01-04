/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.command.SubCommandGroup;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.lifecycle.DiscordSRVShuttingDownEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.abstraction.CombinedCommand;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.combined.abstraction.Text;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.core.debug.DebugGenerateEvent;
import com.discordsrv.common.core.debug.DebugObservabilityEvent;
import com.discordsrv.common.core.debug.DebugReport;
import com.discordsrv.common.core.debug.file.KeyValueDebugFile;
import com.discordsrv.common.core.logging.Logger;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.paste.Paste;
import com.discordsrv.common.core.paste.PasteService;
import com.discordsrv.common.core.paste.service.AESEncryptedPasteService;
import com.discordsrv.common.core.paste.service.BytebinPasteService;
import com.discordsrv.common.permission.game.Permissions;
import com.discordsrv.common.util.ExceptionUtil;
import net.kyori.adventure.text.format.NamedTextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class DebugCommand {

    private static final String LABEL = "debug";
    private static final ComponentIdentifier IDENTIFIER_START = ComponentIdentifier.of("DiscordSRV", "debug-start");
    private static final ComponentIdentifier IDENTIFIER_STOP = ComponentIdentifier.of("DiscordSRV", "debug-stop");
    private static final ComponentIdentifier IDENTIFIER_UPLOAD = ComponentIdentifier.of("DiscordSRV", "debug-upload");
    private static final ComponentIdentifier IDENTIFIER_ZIP = ComponentIdentifier.of("DiscordSRV", "debug-zip");
    private static final String START_LABEL = "start";
    private static final String STOP_LABEL = "stop";
    private static final String UPLOAD_LABEL = "upload";
    private static final String ZIP_LABEL = "zip";

    private static DebugCommand INSTANCE;
    private static GameCommand GAME;
    private static SubCommandGroup DISCORD;

    private static DebugCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new DebugCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            DebugCommand command = getInstance(discordSRV);
            GAME = GameCommand.literal(LABEL)
                    .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.debugCommandDescription.minecraft()))
                    .requiredPermission(Permissions.COMMAND_DEBUG)
                    .executor(command.base)
                    .then(GameCommand.literal(START_LABEL)
                                  .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.debugStartCommandDescription.minecraft()))
                                  .executor(command.start))
                    .then(GameCommand.literal(STOP_LABEL)
                                  .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.debugStopCommandDescription.minecraft()))
                                  .executor(command.stop))
                    .then(GameCommand.literal(UPLOAD_LABEL)
                                  .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.debugUploadCommandDescription.minecraft()))
                                  .executor(command.upload))
                    .then(GameCommand.literal(ZIP_LABEL)
                                  .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.debugZipCommandDescription.minecraft()))
                                  .executor(command.zip));
        }

        return GAME;
    }

    public static SubCommandGroup getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            DebugCommand command = getInstance(discordSRV);

            DISCORD = SubCommandGroup.builder(
                            LABEL,
                    ""
            )
                    .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.debugCommandDescription.discord().content()))
                    .addCommand(DiscordCommand.chatInput(IDENTIFIER_START, START_LABEL, "")
                                        .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.debugStartCommandDescription.discord().content()))
                                        .setEventHandler(command.start)
                                        .build())
                    .addCommand(DiscordCommand.chatInput(IDENTIFIER_STOP, STOP_LABEL, "")
                                        .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.debugStopCommandDescription.discord().content()))
                                        .setEventHandler(command.stop)
                                        .build())
                    .addCommand(DiscordCommand.chatInput(IDENTIFIER_UPLOAD, UPLOAD_LABEL, "")
                                        .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.debugUploadCommandDescription.discord().content()))
                                        .setEventHandler(command.upload)
                                        .build())
                    .addCommand(DiscordCommand.chatInput(IDENTIFIER_ZIP, ZIP_LABEL, "")
                                        .addDescriptionTranslations(discordSRV.getAllTranslations(config -> config.debugZipCommandDescription.discord().content()))
                                        .setEventHandler(command.zip)
                                        .build())
                    .build();
        }

        return DISCORD;
    }

    private static final String URL_FORMAT = DiscordSRV.WEBSITE + "/debug/%s#%s";
    public static final Base64.Encoder KEY_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final AtomicBoolean debugObserving = new AtomicBoolean(false);
    private final DiscordSRV discordSRV;
    private final Logger logger;
    private final PasteService pasteService;

    private final BaseCommand base;
    private final StartCommand start;
    private final StopCommand stop;
    private final UploadCommand upload;
    private final ZipCommand zip;

    public DebugCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "DEBUG_COMMAND");
        this.pasteService = new AESEncryptedPasteService(new BytebinPasteService(discordSRV, "https://bytebin.lucko.me") /* TODO: final store tbd */, 128);

        this.base = new BaseCommand(this);
        this.start = new StartCommand(this);
        this.stop = new StopCommand(this);
        this.upload = new UploadCommand(this);
        this.zip = new ZipCommand(this);

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

    public void execute(String label, CommandExecution execution) {
        execution.runAsync(() -> handle(execution, label));
    }

    private void handle(CommandExecution execution, String subCommand) {
        if (START_LABEL.equals(subCommand)) {
            if (!compareAndSetDebugObservability(true)) {
                execution.send(new Text("Debug observing is already enabled").withGameColor(NamedTextColor.RED));
                return;
            }

            execution.send(new Text("Debug observing started").withGameColor(NamedTextColor.GREEN));
            return;
        }

        boolean useUpload = subCommand == null || UPLOAD_LABEL.equals(subCommand);
        boolean useZip = subCommand == null || ZIP_LABEL.equals(subCommand);
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
        } else if (STOP_LABEL.equals(subCommand)) {
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

    public static class BaseCommand extends CombinedCommand {

        private final DebugCommand parent;

        public BaseCommand(DebugCommand parent) {
            super(parent.discordSRV);
            this.parent = parent;
        }

        @Override
        public void execute(CommandExecution execution) {
            parent.execute(null, execution);
        }
    }

    public static class StartCommand extends CombinedCommand {

        private final DebugCommand parent;

        public StartCommand(DebugCommand parent) {
            super(parent.discordSRV);
            this.parent = parent;
        }

        @Override
        public void execute(CommandExecution execution) {
            parent.execute(START_LABEL, execution);
        }
    }

    public static class StopCommand extends CombinedCommand {

        private final DebugCommand parent;

        public StopCommand(DebugCommand parent) {
            super(parent.discordSRV);
            this.parent = parent;
        }

        @Override
        public void execute(CommandExecution execution) {
            parent.execute(STOP_LABEL, execution);
        }
    }

    public static class UploadCommand extends CombinedCommand {

        private final DebugCommand parent;

        public UploadCommand(DebugCommand parent) {
            super(parent.discordSRV);
            this.parent = parent;
        }

        @Override
        public void execute(CommandExecution execution) {
            parent.execute(UPLOAD_LABEL, execution);
        }
    }

    public static class ZipCommand extends CombinedCommand {

        private final DebugCommand parent;

        public ZipCommand(DebugCommand parent) {
            super(parent.discordSRV);
            this.parent = parent;
        }

        @Override
        public void execute(CommandExecution execution) {
            parent.execute(ZIP_LABEL, execution);
        }
    }

}
