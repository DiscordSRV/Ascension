package com.discordsrv.common.command.combined.commands;

import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.interaction.command.DiscordCommand;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.abstraction.CombinedCommand;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.combined.abstraction.GameCommandExecution;
import com.discordsrv.common.command.combined.abstraction.Text;
import com.discordsrv.common.command.game.abstraction.GameCommand;
import com.discordsrv.common.command.game.sender.ICommandSender;
import com.discordsrv.common.command.util.CommandUtil;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.linking.LinkProvider;
import com.discordsrv.common.linking.LinkStore;
import com.discordsrv.common.logging.Logger;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.permission.Permission;
import com.discordsrv.common.player.IPlayer;
import com.github.benmanes.caffeine.cache.Cache;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LinkInitCommand extends CombinedCommand {

    private static LinkInitCommand INSTANCE;
    private static GameCommand GAME;
    private static DiscordCommand DISCORD;

    private static LinkInitCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new LinkInitCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            LinkInitCommand command = getInstance(discordSRV);
            GAME = GameCommand.literal("link")
                    .then(
                            GameCommand.stringWord("player")
                                    .then(
                                            GameCommand.stringWord("user")
                                                    .requiredPermission(Permission.COMMAND_LINK_OTHER)
                                                    .executor(command)
                                    )
                    )
                    .requiredPermission(Permission.COMMAND_LINK)
                    .executor(command);
        }

        return GAME;
    }

    public static DiscordCommand getDiscord(DiscordSRV discordSRV) {
        if (DISCORD == null) {
            LinkInitCommand command = getInstance(discordSRV);
            DISCORD = DiscordCommand.chatInput(ComponentIdentifier.of("DiscordSRV", "link"), "link", "Link players")
                    .addOption(
                            CommandOption.builder(CommandOption.Type.USER, "user", "The user to link")
                                    .setRequired(true)
                                    .build()
                    )
                    .addOption(
                            CommandOption.builder(CommandOption.Type.STRING, "player", "The player to link")
                                    .setRequired(true)
                                    .build()
                    )
                    .setAutoCompleteHandler(command)
                    .setEventHandler(command)
                    .build();
        }

        return DISCORD;
    }

    private final DiscordSRV discordSRV;
    private final Logger logger;
    private final Cache<UUID, Boolean> linkCheckRateLimit;

    public LinkInitCommand(DiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "LINK_COMMAND");
        this.linkCheckRateLimit = discordSRV.caffeineBuilder()
                .expireAfterWrite(LinkStore.LINKING_CODE_RATE_LIMIT)
                .build();
    }

    @Override
    public void execute(CommandExecution execution) {
        String playerArgument = execution.getArgument("player");
        String userArgument = execution.getArgument("user");
        if (execution instanceof GameCommandExecution) {
            ICommandSender sender = ((GameCommandExecution) execution).getSender();

            if (StringUtils.isEmpty(playerArgument)) {
                if (sender instanceof IPlayer) {
                    startLinking((IPlayer) sender, ((GameCommandExecution) execution).getLabel());
                } else {
                    sender.sendMessage(discordSRV.messagesConfig(sender).pleaseSpecifyPlayerAndUserToLink.asComponent());
                }
                return;
            }

            if (!sender.hasPermission(Permission.COMMAND_LINK_OTHER)) {
                sender.sendMessage(discordSRV.messagesConfig(sender).noPermission.asComponent());
                return;
            }
        }

        LinkProvider linkProvider = discordSRV.linkProvider();
        if (!(linkProvider instanceof LinkStore)) {
            execution.send(new Text("Cannot create links using this link provider").withGameColor(NamedTextColor.DARK_RED));
            return;
        }

        CompletableFuture<UUID> playerUUIDFuture = CommandUtil.lookupPlayer(discordSRV, logger, execution, false, playerArgument, null);
        CompletableFuture<Long> userIdFuture = CommandUtil.lookupUser(discordSRV, logger, execution, false, userArgument, null);

        playerUUIDFuture.whenComplete((playerUUID, __) -> userIdFuture.whenComplete((userId, ___) -> {
            if (playerUUID == null) {
                execution.messages().playerNotFound(execution);
                return;
            }
            if (userId == null) {
                execution.messages().userNotFound(execution);
                return;
            }

            linkProvider.queryUserId(playerUUID).whenComplete((linkedUser, t) -> {
                if (t != null) {
                    logger.error("Failed to check linking status", t);
                    execution.messages().unableToCheckLinkingStatus(execution);
                    return;
                }
                if (linkedUser.isPresent()) {
                    execution.messages().playerAlreadyLinked3rd(execution);
                    return;
                }

                linkProvider.queryPlayerUUID(userId).whenComplete((linkedPlayer, t2) -> {
                    if (t2 != null) {
                        logger.error("Failed to check linking status", t2);
                        execution.messages().unableToCheckLinkingStatus(execution);
                        return;
                    }
                    if (linkedPlayer.isPresent()) {
                        execution.messages().userAlreadyLinked3rd(execution);
                        return;
                    }

                    ((LinkStore) linkProvider).createLink(playerUUID, userId).whenComplete((v, t3) -> {
                        if (t3 != null) {
                            logger.error("Failed to create link", t3);
                            execution.send(
                                    execution.messages().minecraft.unableToLinkAtThisTime.asComponent(),
                                    execution.messages().discord.unableToCheckLinkingStatus
                            );
                            return;
                        }

                        execution.messages().nowLinked3rd(discordSRV, execution, playerUUID, userId);
                    });
                });
            });
        }));
    }

    private void startLinking(IPlayer player, String label) {
        MessagesConfig.Minecraft messages = discordSRV.messagesConfig(player);

        LinkProvider linkProvider = discordSRV.linkProvider();
        if (linkProvider.getCachedUserId(player.uniqueId()).isPresent()) {
            player.sendMessage(messages.alreadyLinked1st.asComponent());
            return;
        }

        if (linkCheckRateLimit.getIfPresent(player.uniqueId()) != null) {
            player.sendMessage(messages.pleaseWaitBeforeRunningThatCommandAgain.asComponent());
            return;
        }
        linkCheckRateLimit.put(player.uniqueId(), true);

        player.sendMessage(discordSRV.messagesConfig(player).checkingLinkStatus.asComponent());
        linkProvider.queryUserId(player.uniqueId(), true).whenComplete((userId, t1) -> {
            if (t1 != null) {
                logger.error("Failed to check linking status", t1);
                player.sendMessage(messages.unableToLinkAtThisTime.asComponent());
                return;
            }
            if (userId.isPresent()) {
                player.sendMessage(messages.nowLinked1st.asComponent());
                return;
            }

            linkProvider.getLinkingInstructions(player, label).whenComplete((comp, t2) -> {
                if (t2 != null) {
                    logger.error("Failed to link account", t2);
                    player.sendMessage(messages.unableToLinkAtThisTime.asComponent());
                    return;
                }

                player.sendMessage(ComponentUtil.fromAPI(comp));
            });
        });
    }
}
