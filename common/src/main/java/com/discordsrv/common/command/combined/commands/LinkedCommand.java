package com.discordsrv.common.command.combined.commands;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.placeholder.provider.SinglePlaceholder;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.abstraction.CombinedCommand;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.command.util.CommandUtil;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.permission.Permission;
import com.discordsrv.common.player.IOfflinePlayer;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
                            GameCommand.stringGreedy("target")
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

    private final Logger logger;

    public LinkedCommand(DiscordSRV discordSRV) {
        super(discordSRV);
        this.logger = new NamedLogger(discordSRV, "LINKED_COMMAND");
    }

    @Override
    public void execute(CommandExecution execution) {
        execution.setEphemeral(true);

        execution.runAsync(() -> CommandUtil.lookupTarget(discordSRV, logger, execution, true, Permission.COMMAND_LINKED_OTHER)
                .whenComplete((result, t) -> {
                    if (t != null) {
                        logger.error("Failed to execute linked command", t);
                        return;
                    }
                    if (result.isValid()) {
                        processResult(result, execution);
                    }
                })
        );
    }

    private void processResult(CommandUtil.TargetLookupResult result, CommandExecution execution) {
        if (result.isPlayer()) {
            UUID playerUUID = result.getPlayerUUID();
            CompletableFuture<IOfflinePlayer> playerFuture = CompletableFutureUtil.timeout(
                    discordSRV,
                    discordSRV.playerProvider().lookupOfflinePlayer(playerUUID),
                    Duration.ofSeconds(5)
            );

            discordSRV.linkProvider().getUserId(playerUUID).whenComplete((optUserId, t) -> {
                if (t != null) {
                    logger.error("Failed to check linking status during linked command", t);
                    execution.send(
                            execution.messages().minecraft.unableToCheckLinkingStatus.asComponent(),
                            execution.messages().discord.unableToCheckLinkingStatus
                    );
                    return;
                }
                if (!optUserId.isPresent()) {
                    playerFuture.whenComplete((player, ___) -> execution.send(
                            ComponentUtil.fromAPI(
                                    execution.messages().minecraft.minecraftPlayerUnlinked
                                            .textBuilder()
                                            .applyPlaceholderService()
                                            .addContext(player)
                                            .addPlaceholder("player_uuid", playerUUID)
                                            .build()
                            ),
                            discordSRV.placeholderService().replacePlaceholders(
                                    execution.messages().discord.minecraftPlayerUnlinked,
                                    player,
                                    new SinglePlaceholder("player_uuid", playerUUID)
                            )
                    ));
                    return;
                }

                long userId = optUserId.get();
                CompletableFuture<DiscordUser> userFuture = CompletableFutureUtil.timeout(
                        discordSRV,
                        discordSRV.discordAPI().retrieveUserById(userId),
                        Duration.ofSeconds(5)
                );

                playerFuture.whenComplete((player, __) -> userFuture.whenComplete((user, ___) -> execution.send(
                        ComponentUtil.fromAPI(
                                execution.messages().minecraft.minecraftPlayerLinkedTo
                                        .textBuilder()
                                        .applyPlaceholderService()
                                        .addContext(player, user)
                                        .addPlaceholder("player_uuid", playerUUID)
                                        .addPlaceholder("user_id", userId)
                                        .build()
                        ),
                        discordSRV.placeholderService().replacePlaceholders(
                                execution.messages().discord.minecraftPlayerLinkedTo,
                                player,
                                user,
                                new SinglePlaceholder("player_uuid", playerUUID),
                                new SinglePlaceholder("user_id", userId)
                        )
                )));
            });
        } else {
            long userId = result.getUserId();
            CompletableFuture<DiscordUser> userFuture = CompletableFutureUtil.timeout(
                    discordSRV,
                    discordSRV.discordAPI().retrieveUserById(userId),
                    Duration.ofSeconds(5)
            );

            discordSRV.linkProvider().getPlayerUUID(userId).whenComplete((optPlayerUUID, t) -> {
                if (t != null) {
                    logger.error("Failed to check linking status during linked command", t);
                    execution.send(
                            execution.messages().minecraft.unableToCheckLinkingStatus.asComponent(),
                            execution.messages().discord.unableToCheckLinkingStatus
                    );
                    return;
                }
                if (!optPlayerUUID.isPresent()) {
                    userFuture.whenComplete((user, ___) -> execution.send(
                            ComponentUtil.fromAPI(
                                    execution.messages().minecraft.discordUserUnlinked
                                            .textBuilder()
                                            .applyPlaceholderService()
                                            .addContext(user)
                                            .addPlaceholder("user_id", userId)
                                            .build()
                            ),
                            discordSRV.placeholderService().replacePlaceholders(
                                    execution.messages().discord.discordUserUnlinked,
                                    user,
                                    new SinglePlaceholder("user_id", userId)
                            )
                    ));
                    return;
                }

                UUID playerUUID = optPlayerUUID.get();
                CompletableFuture<IOfflinePlayer> playerFuture = CompletableFutureUtil.timeout(
                        discordSRV,
                        discordSRV.playerProvider().lookupOfflinePlayer(playerUUID),
                        Duration.ofSeconds(5)
                );

                userFuture.whenComplete((user, __) -> playerFuture.whenComplete((player, ___) -> execution.send(
                        ComponentUtil.fromAPI(
                                execution.messages().minecraft.discordUserLinkedTo
                                        .textBuilder()
                                        .applyPlaceholderService()
                                        .addContext(user, player)
                                        .addPlaceholder("user_id", userId)
                                        .addPlaceholder("player_uuid", playerUUID)
                                        .build()
                        ),
                        discordSRV.placeholderService().replacePlaceholders(
                                execution.messages().discord.discordUserLinkedTo,
                                user,
                                player,
                                new SinglePlaceholder("user_id", userId),
                                new SinglePlaceholder("player_uuid", playerUUID)
                        )
                )));
            });
        }
    }
}
