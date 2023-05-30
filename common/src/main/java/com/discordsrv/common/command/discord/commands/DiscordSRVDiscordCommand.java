package com.discordsrv.common.command.discord.commands;

import com.discordsrv.api.discord.entity.interaction.command.Command;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.commands.DebugCommand;
import com.discordsrv.common.command.combined.commands.ResyncCommand;
import com.discordsrv.common.command.combined.commands.VersionCommand;

public class DiscordSRVDiscordCommand {

    private static final ComponentIdentifier IDENTIFIER = ComponentIdentifier.of("DiscordSRV", "discordsrv");

    private static Command INSTANCE;

    public static Command get(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            INSTANCE = Command.chatInput(IDENTIFIER, "discordsrv", "DiscordSRV related commands")
                    .addSubCommand(DebugCommand.getDiscord(discordSRV))
                    .addSubCommand(VersionCommand.getDiscord(discordSRV))
                    .addSubCommand(ResyncCommand.getDiscord(discordSRV))
                    .setGuildOnly(false)
                    .setDefaultPermission(Command.DefaultPermission.ADMINISTRATOR)
                    .build();
        }

        return INSTANCE;
    }
}
