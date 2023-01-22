/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.command.game.command.subcommand;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.command.game.abstraction.GameCommandArguments;
import com.discordsrv.common.command.game.abstraction.GameCommandExecutor;
import com.discordsrv.common.command.game.sender.ICommandSender;
import com.discordsrv.common.debug.DebugReport;
import com.discordsrv.common.paste.Paste;
import com.discordsrv.common.paste.PasteService;
import com.discordsrv.common.paste.service.AESEncryptedPasteService;
import com.discordsrv.common.paste.service.BytebinPasteService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.Locale;

public class DebugCommand implements GameCommandExecutor {

    private static GameCommand INSTANCE;

    public static GameCommand get(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            DebugCommand debugCommand = new DebugCommand(discordSRV);
            INSTANCE = GameCommand.literal("debug")
                    .requiredPermission("discordsrv.admin.debug")
                    .executor(debugCommand)
                    .then(
                            GameCommand.stringWord("zip")
                                    .suggester((sender, previousArguments, currentInput) ->
                                                       "zip".startsWith(currentInput.toLowerCase(Locale.ROOT))
                                                        ? Collections.singletonList("zip") : Collections.emptyList())
                                    .executor(debugCommand)
                    );
        }

        return INSTANCE;
    }

    private static final String URL_FORMAT = "https://discordsrv.vankka.dev/debug/%s#%s";
    private static final Base64.Encoder KEY_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final DiscordSRV discordSRV;
    private final PasteService pasteService;

    public DebugCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.pasteService = new AESEncryptedPasteService(new BytebinPasteService(discordSRV, "https://bytebin.lucko.me") /* TODO: final store tbd */, 128);
    }

    @Override
    public void execute(ICommandSender sender, GameCommandArguments arguments) {
        boolean usePaste = !"zip".equals(arguments.getString("zip"));

        discordSRV.scheduler().run(() -> {
            DebugReport report = new DebugReport(discordSRV);
            report.generate();

            Throwable pasteError = usePaste ? paste(sender, report) : null;
            if (usePaste && pasteError == null) {
                // Success
                return;
            }

            Throwable zipError = zip(sender, report);
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
            sender.sendMessage(Component.text(
                    usePaste
                        ? "Failed to upload debug report to paste & failed to generate zip"
                        : "Failed to create debug zip",
                    NamedTextColor.DARK_RED
            ));
        });
    }

    private Throwable paste(ICommandSender sender, DebugReport report) {
        try {
            Paste paste = report.upload(pasteService);
            String key = new String(KEY_ENCODER.encode(paste.decryptionKey()), StandardCharsets.UTF_8);
            String url = String.format(URL_FORMAT, paste.id(), key);

            sender.sendMessage(Component.text(url).clickEvent(ClickEvent.openUrl(url)));
            return null;
        } catch (Throwable e) {
            return e;
        }
    }

    private Throwable zip(ICommandSender sender, DebugReport report) {
        try {
            Path zip = report.zip();
            Path relative = discordSRV.dataDirectory().resolve("../..").relativize(zip);
            sender.sendMessage(
                    Component.text("Debug generated to zip: ", NamedTextColor.GRAY)
                            .append(Component.text(relative.toString(), NamedTextColor.GREEN))
            );
            return null;
        } catch (Throwable e) {
            return e;
        }
    }

}
