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

package com.discordsrv.common.command.combined.commands;

import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.abstraction.CombinedCommand;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.combined.abstraction.Text;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.debug.DebugReport;
import com.discordsrv.common.paste.Paste;
import com.discordsrv.common.paste.PasteService;
import com.discordsrv.common.paste.service.AESEncryptedPasteService;
import com.discordsrv.common.paste.service.BytebinPasteService;
import com.discordsrv.common.permission.Permission;
import net.kyori.adventure.text.format.NamedTextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.Locale;

public class DebugCommand extends CombinedCommand {

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
                    .requiredPermission(Permission.COMMAND_DEBUG)
                    .executor(command)
                    .then(
                            GameCommand.stringWord("format")
                                    .suggester((sender, previousArguments, currentInput) ->
                                                       "zip".startsWith(currentInput.toLowerCase(Locale.ROOT))
                                                        ? Collections.singletonList("zip") : Collections.emptyList())
                                    .executor(command)
                    );
        }

        return GAME;
    }

    public static DiscordCommand getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            DebugCommand command = getInstance(discordSRV);
            DISCORD = DiscordCommand.chatInput(ComponentIdentifier.of("DiscordSRV", "debug"), "debug", "Create a debug report")
                    .addOption(
                            CommandOption.builder(CommandOption.Type.STRING, "format", "The format to generate the debug report")
                                    .addChoice(".zip", "zip")
                                    .setRequired(false)
                                    .build()
                    )
                    .setEventHandler(command)
                    .build();
        }

        return DISCORD;
    }

    private static final String URL_FORMAT = DiscordSRV.WEBSITE + "/debug/%s#%s";
    public static final Base64.Encoder KEY_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final PasteService pasteService;

    public DebugCommand(DiscordSRV discordSRV) {
        super(discordSRV);
        this.pasteService = new AESEncryptedPasteService(new BytebinPasteService(discordSRV, "https://bytebin.lucko.me") /* TODO: final store tbd */, 128);
    }

    @Override
    public void execute(CommandExecution execution) {
        boolean usePaste = !"zip".equals(execution.getArgument("format"));

        execution.runAsync(() -> {
            DebugReport report = new DebugReport(discordSRV);
            report.generate();

            Throwable pasteError = usePaste ? paste(execution, report) : null;
            if (usePaste && pasteError == null) {
                // Success
                return;
            }

            Throwable zipError = zip(execution, report);
            if (zipError == null) {
                // Success
                if (usePaste) {
                    discordSRV.logger().warning("Failed to upload debug, zip generation succeeded", pasteError);
                }
                return;
            }

            if (pasteError != null) {
                zipError.addSuppressed(pasteError);
            }
            discordSRV.logger().error(usePaste ? "Failed to upload & zip debug" : "Failed to zip debug", zipError);
            execution.send(
                    new Text(usePaste
                             ? "Failed to upload debug report to paste & failed to generate zip"
                             : "Failed to create debug zip"
                    ).withGameColor(NamedTextColor.DARK_RED)
            );
        });
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
