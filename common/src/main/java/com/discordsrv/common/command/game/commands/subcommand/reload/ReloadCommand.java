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

package com.discordsrv.common.command.game.commands.subcommand.reload;

import com.discordsrv.api.reload.ReloadFlag;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.command.game.abstraction.command.GameCommand;
import com.discordsrv.common.command.game.abstraction.command.GameCommandArguments;
import com.discordsrv.common.command.game.abstraction.command.GameCommandExecutor;
import com.discordsrv.common.command.game.abstraction.command.GameCommandSuggester;
import com.discordsrv.common.command.game.abstraction.sender.ICommandSender;
import com.discordsrv.common.permission.game.Permission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ReloadCommand implements GameCommandExecutor, GameCommandSuggester {

    private static GameCommand INSTANCE;

    public static GameCommand get(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            ReloadCommand cmd = new ReloadCommand(discordSRV);
            INSTANCE = GameCommand.literal("reload")
                    .requiredPermission(Permission.COMMAND_RELOAD)
                    .executor(cmd)
                    .then(
                            GameCommand.stringGreedy("flags")
                                    .executor(cmd).suggester(cmd)
                    );
        }

        return INSTANCE;
    }

    private final DiscordSRV discordSRV;

    public ReloadCommand(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Override
    public void execute(ICommandSender sender, GameCommandArguments arguments, String label) {
        AtomicBoolean dangerousFlags = new AtomicBoolean(false);
        Set<ReloadFlag> flags = getFlagsFromArguments(sender, arguments, dangerousFlags);
        if (flags == null) {
            flags = ReloadFlag.DEFAULT_FLAGS;
        }

        if (dangerousFlags.get()) {
            sender.sendMessage(Component.text("You can add -confirm to the end of the command if you wish to proceed anyway", NamedTextColor.DARK_RED));
            return;
        }

        if (flags.isEmpty()) {
            sender.sendMessage(Component.text("Please specify at least one valid flag", NamedTextColor.RED));
            return;
        }

        List<ReloadResult> results = discordSRV.runReload(flags);
        if (results.isEmpty()) {
            sender.sendMessage(Component.text("Reload successful", NamedTextColor.GRAY));
            return;
        }
        for (ReloadResult result : results) {
            switch (result) {
                case ERROR: {
                    sender.sendMessage(
                            Component.text()
                                    .append(Component.text("Reload failed.", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                                    .append(Component.text("Please check the server console/log for more details."))
                    );
                    break;
                }
                case SECURITY_FAILED: {
                    sender.sendMessage(Component.text(
                            "DiscordSRV is disabled due to a security check failure. "
                                    + "Please check console for more details", NamedTextColor.DARK_RED));
                    break;
                }
                case RESTART_REQUIRED: {
                    sender.sendMessage(Component.text("Some changes require a server restart"));
                    break;
                }
                case STORAGE_CONNECTION_FAILED: {
                    sender.sendMessage(Component.text("Storage connection failed, please check console for details.", NamedTextColor.RED));
                    break;
                }
                case DISCORD_CONNECTION_FAILED: {
                    sender.sendMessage(Component.text("Discord connection failed, please check console for details.", NamedTextColor.RED));
                    break;
                }
                case DISCORD_CONNECTION_RELOAD_REQUIRED: {
                    String command = "discordsrv reload " + ReloadFlag.DISCORD_CONNECTION.name().toLowerCase(Locale.ROOT) + " -confirm";
                    Component child;
                    if (sender instanceof IPlayer) {
                        child = Component.text("[Click to reload Discord connection]", NamedTextColor.DARK_RED)
                                .clickEvent(ClickEvent.runCommand("/" + command))
                                .hoverEvent(HoverEvent.showText(Component.text("/" + command)));
                    } else {
                        child = Component.text("Run ", NamedTextColor.DARK_RED)
                                .append(Component.text(command, NamedTextColor.GRAY))
                                .append(Component.text(" to reload the Discord connection"));
                    }

                    sender.sendMessage(
                            Component.text()
                                    .append(Component.text("Some changes require a Discord connection reload. ", NamedTextColor.GRAY))
                                    .append(child)
                    );
                    break;
                }
            }
        }
    }

    private Set<ReloadFlag> getFlagsFromArguments(ICommandSender sender, GameCommandArguments arguments, AtomicBoolean dangerousFlags) {
        String argument = null;
        try {
            argument = arguments.get("flags", String.class);
        } catch (IllegalArgumentException ignored) {}

        if (argument == null) {
            return null;
        }

        List<String> parts = new ArrayList<>(Arrays.asList(argument.split(" ")));
        boolean confirm = parts.remove("-confirm");

        Set<ReloadFlag> flags = new LinkedHashSet<>();
        if (discordSRV.status().isStartupError()) {
            // If startup error, use all flags
            parts.clear();
            flags.addAll(ReloadFlag.ALL);
        }

        for (String part : parts) {
            try {
                ReloadFlag flag = ReloadFlag.valueOf(part.toUpperCase(Locale.ROOT));
                if (flag.requiresConfirm(discordSRV) && !confirm) {
                    dangerousFlags.set(true);
                    sender.sendMessage(
                            Component.text("Reloading ", NamedTextColor.RED)
                                    .append(Component.text(part, NamedTextColor.GRAY))
                                    .append(Component.text(" might cause DiscordSRV to end up in a unrecoverable state", NamedTextColor.RED))
                    );
                    continue;
                }
                flags.add(flag);
            } catch (IllegalArgumentException ignored) {
                sender.sendMessage(Component.text("Flag ", NamedTextColor.RED)
                                           .append(Component.text(part, NamedTextColor.GRAY))
                                           .append(Component.text(" is not known", NamedTextColor.RED)));
            }
        }
        return flags;
    }

    @Override
    public List<String> suggestValues(
            ICommandSender sender,
            GameCommandArguments previousArguments,
            String currentInput
    ) {
        int lastSpace = currentInput.lastIndexOf(' ') + 1;
        String last = currentInput.substring(lastSpace);
        String beforeLastSpace = currentInput.substring(0, lastSpace);

        List<String> options = ReloadFlag.ALL.stream()
                .map(flag -> flag.name().toLowerCase(Locale.ROOT))
                .filter(flag -> flag.startsWith(last))
                .collect(Collectors.toList());
        for (String part : currentInput.split(" ")) {
            options.remove(part);
        }

        return options.stream().map(flag -> beforeLastSpace + flag).collect(Collectors.toList());
    }
}
