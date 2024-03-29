package com.discordsrv.common.command.discord.commands;

import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.commands.*;
import com.discordsrv.common.command.discord.commands.subcommand.ExecuteCommand;
import com.discordsrv.common.config.main.DiscordCommandConfig;
import com.discordsrv.common.linking.LinkStore;

public class DiscordSRVDiscordCommand {

    private static final ComponentIdentifier IDENTIFIER = ComponentIdentifier.of("DiscordSRV", "discordsrv");

    private static DiscordCommand INSTANCE;

    public static DiscordCommand get(DiscordSRV discordSRV) {
        if (INSTANCE == null) {
            DiscordCommandConfig config = discordSRV.config().discordCommand;

            DiscordCommand.ChatInputBuilder builder = DiscordCommand.chatInput(IDENTIFIER, "discordsrv", "DiscordSRV related commands")
                    .addSubCommand(DebugCommand.getDiscord(discordSRV))
                    .addSubCommand(VersionCommand.getDiscord(discordSRV))
                    .addSubCommand(ResyncCommand.getDiscord(discordSRV))
                    .addSubCommand(LinkedCommand.getDiscord(discordSRV));

            if (config.execute.enabled) {
                builder = builder.addSubCommand(ExecuteCommand.get(discordSRV));
            }
            if (discordSRV.linkProvider() instanceof LinkStore) {
                builder = builder
                        .addSubCommand(LinkInitCommand.getDiscord(discordSRV))
                        .addSubCommand(UnlinkCommand.getDiscord(discordSRV));
            }

            INSTANCE = builder
                    .setGuildOnly(false)
                    .setDefaultPermission(DiscordCommand.DefaultPermission.ADMINISTRATOR)
                    .build();
        }

        return INSTANCE;
    }
}
