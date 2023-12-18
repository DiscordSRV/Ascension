package com.discordsrv.common.command.combined.commands;

import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.abstraction.CombinedCommand;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.combined.abstraction.Text;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.command.util.CommandUtil;
import com.discordsrv.common.permission.util.Permission;

import java.util.UUID;

public class LinkedCommand extends CombinedCommand {

    private static LinkedCommand INSTANCE;
    private static GameCommand GAME;
    private static DiscordCommand DISCORD;

    private static LinkedCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new LinkedCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            LinkedCommand command = getInstance(discordSRV);
            GAME = GameCommand.literal("linked")
                    .then(
                            GameCommand.stringWord("target")
                                    .requiredPermission(Permission.COMMAND_LINKED_OTHER)
                                    .executor(command)
                    )
                    .requiredPermission(Permission.COMMAND_LINKED)
                    .executor(command);
        }

        return GAME;
    }

    public static DiscordCommand getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            LinkedCommand command = getInstance(discordSRV);
            DISCORD = DiscordCommand.chatInput(ComponentIdentifier.of("DiscordSRV", "linked"), "linked", "Get the linking status of a given user")
                    .addOption(CommandOption.builder(
                            CommandOption.Type.USER,
                            "user",
                            "The Discord user to check the linking status of"
                    ).setRequired(false).build())
                    .addOption(CommandOption.builder(
                            CommandOption.Type.STRING,
                            "player",
                            "The Minecraft player username or UUID to check the linking status of"
                    ).setRequired(false).build())
                    .setEventHandler(command)
                    .build();
        }

        return DISCORD;
    }

    public LinkedCommand(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public void execute(CommandExecution execution) {
        execution.setEphemeral(true);

        CommandUtil.TargetLookupResult result = CommandUtil.lookupTarget(discordSRV, execution, true, Permission.COMMAND_LINKED_OTHER);
        if (!result.isValid()) {
            return;
        }

        if (result.isPlayer()) {
            execution.runAsync(() -> {
                discordSRV.linkProvider().getUserId(result.getPlayerUUID()).whenComplete((userId, t) -> {
                    execution.send(new Text(userId.map(Long::toUnsignedString).orElse("Not linked"))); // TODO: username
                });
            });
        } else {
            execution.runAsync(() -> {
                discordSRV.linkProvider().getPlayerUUID(result.getUserId()).whenComplete((playerUUID, t) -> {
                    execution.send(new Text(playerUUID.map(UUID::toString).orElse("Not linked"))); // TODO: player name
                });
            });
        }
    }
}
