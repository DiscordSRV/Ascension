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
import com.discordsrv.common.linking.LinkProvider;
import com.discordsrv.common.linking.LinkStore;
import com.discordsrv.common.permission.util.Permission;
import com.discordsrv.common.player.IPlayer;
import com.github.benmanes.caffeine.cache.Cache;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class LinkInitCommand extends CombinedCommand {

    private static DebugCommand INSTANCE;
    private static GameCommand GAME;
    private static DiscordCommand DISCORD;

    private static DebugCommand getInstance(DiscordSRV discordSRV) {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new DebugCommand(discordSRV));
    }

    public static GameCommand getGame(DiscordSRV discordSRV) {
        if (GAME == null) {
            LinkInitCommand command = new LinkInitCommand(discordSRV);
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
            DebugCommand command = getInstance(discordSRV);
            DISCORD = DiscordCommand.chatInput(ComponentIdentifier.of("DiscordSRV", "link"), "link", "Link players")
                    .addOption(
                            CommandOption.builder(CommandOption.Type.STRING, "player", "The player to link")
                                    .setRequired(true)
                                    .build()
                    )
                    .addOption(
                            CommandOption.builder(CommandOption.Type.USER, "user", "The user to link")
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
    private final Cache<UUID, Boolean> linkCheckRateLimit;

    public LinkInitCommand(DiscordSRV discordSRV) {
        super(discordSRV);
        this.discordSRV = discordSRV;
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
                    sender.sendMessage(execution.messages().minecraft.pleaseSpecifyPlayerAndUserToLink.asComponent());
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

        UUID playerUUID = CommandUtil.lookupPlayer(discordSRV, execution, false, playerArgument, null);
        if (playerUUID == null) {
            execution.send(
                    execution.messages().minecraft.playerNotFound.asComponent(),
                    execution.messages().discord.playerNotFound
            );
            return;
        }

        Long userId = CommandUtil.lookupUser(discordSRV, execution, false, userArgument, null);
        if (userId == null) {
            execution.send(
                    execution.messages().minecraft.userNotFound.asComponent(),
                    execution.messages().discord.userNotFound
            );
            return;
        }

        linkProvider.queryUserId(playerUUID).thenCompose(opt -> {
            if (opt.isPresent()) {
                execution.send(
                        execution.messages().minecraft.playerAlreadyLinked3rd.asComponent(),
                        execution.messages().discord.playerAlreadyLinked3rd
                );
                return null;
            }

            return linkProvider.queryPlayerUUID(userId);
        }).thenCompose(opt -> {
            if (opt.isPresent()) {
                execution.send(
                        execution.messages().minecraft.userAlreadyLinked3rd.asComponent(),
                        execution.messages().discord.userAlreadyLinked3rd
                );
                return null;
            }

            return ((LinkStore) linkProvider).createLink(playerUUID, userId);
        }).whenComplete((v, t) -> {
            if (t != null) {
                // TODO: it did not work
                return;
            }

            execution.send(
                    execution.messages().minecraft.nowLinked3rd.asComponent(),
                    execution.messages().discord.nowLinked3rd
            );
        });
    }

    private void startLinking(IPlayer player, String label) {
        LinkProvider linkProvider = discordSRV.linkProvider();
        if (linkProvider.getCachedUserId(player.uniqueId()).isPresent()) {
            player.sendMessage(discordSRV.messagesConfig(player).alreadyLinked1st.asComponent());
            return;
        }

        if (linkCheckRateLimit.getIfPresent(player.uniqueId()) != null) {
            player.sendMessage(discordSRV.messagesConfig(player).pleaseWaitBeforeRunningThatCommandAgain.asComponent());
            return;
        }
        linkCheckRateLimit.put(player.uniqueId(), true);

        player.sendMessage(discordSRV.messagesConfig(player).checkingLinkStatus.asComponent());
        linkProvider.queryUserId(player.uniqueId(), true).whenComplete((userId, t) -> {
            if (t != null) {
                player.sendMessage(discordSRV.messagesConfig(player).unableToLinkAtThisTime.asComponent());
                return;
            }
            if (userId.isPresent()) {
                player.sendMessage(discordSRV.messagesConfig(player).nowLinked1st.asComponent());
                return;
            }

            linkProvider.getLinkingInstructions(player, label).whenComplete((comp, t2) -> {
                if (t2 != null) {
                    player.sendMessage(discordSRV.messagesConfig(player).unableToLinkAtThisTime.asComponent());
                    return;
                }

                player.sendMessage(ComponentUtil.fromAPI(comp));
            });
        });
    }
}
