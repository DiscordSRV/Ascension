package com.discordsrv.common.command.util;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.command.combined.abstraction.DiscordCommandExecution;
import com.discordsrv.common.command.combined.abstraction.GameCommandExecution;
import com.discordsrv.common.command.combined.abstraction.Text;
import com.discordsrv.common.command.game.sender.ICommandSender;
import com.discordsrv.common.permission.util.Permission;
import com.discordsrv.common.player.IPlayer;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class CommandUtil {

    private CommandUtil() {}

    @Nullable
    public static UUID lookupPlayer(
            DiscordSRV discordSRV,
            CommandExecution execution,
            boolean selfPermitted,
            String target,
            @Nullable Permission otherPermission
    ) {
        TargetLookupResult result = lookupTarget(discordSRV, execution, target, selfPermitted, true, false, otherPermission);
        if (result.isValid()) {
            return result.getPlayerUUID();
        }
        return null;
    }

    @Nullable
    public static Long lookupUser(
            DiscordSRV discordSRV,
            CommandExecution execution,
            boolean selfPermitted,
            String target,
            @Nullable Permission otherPermission
    ) {
        TargetLookupResult result = lookupTarget(discordSRV, execution, target, selfPermitted, false, true, otherPermission);
        if (result.isValid()) {
            return result.getUserId();
        }
        return null;
    }

    public static TargetLookupResult lookupTarget(
            DiscordSRV discordSRV,
            CommandExecution execution,
            boolean selfPermitted,
            @Nullable Permission otherPermission
    ) {
        String target = execution.getArgument("target");
        if (target == null) {
            target = execution.getArgument("user");
        }
        if (target == null) {
            target = execution.getArgument("player");
        }
        return lookupTarget(discordSRV, execution, target, selfPermitted, true, true, otherPermission);
    }

    private static TargetLookupResult lookupTarget(
            DiscordSRV discordSRV,
            CommandExecution execution,
            String target,
            boolean selfPermitted,
            boolean lookupPlayer,
            boolean lookupUser,
            @Nullable Permission otherPermission
    ) {
        if (execution instanceof GameCommandExecution) {
            ICommandSender sender = ((GameCommandExecution) execution).getSender();
            if (target != null) {
                if (otherPermission != null && !sender.hasPermission(Permission.COMMAND_LINKED_OTHER)) {
                    sender.sendMessage(discordSRV.messagesConfig(sender).noPermission.asComponent());
                    return TargetLookupResult.INVALID;
                }
            } else if (sender instanceof IPlayer && selfPermitted && lookupPlayer) {
                target = ((IPlayer) sender).uniqueId().toString();
            } else {
                execution.send(new Text(discordSRV.messagesConfig(execution.locale()).both.placeSpecifyTarget).withGameColor(NamedTextColor.RED));
                return TargetLookupResult.INVALID;
            }
        } else if (execution instanceof DiscordCommandExecution) {
            if (target == null) {
                if (selfPermitted && lookupUser) {
                    target = Long.toUnsignedString(((DiscordCommandExecution) execution).getUser().getIdLong());
                } else {
                    execution.send(new Text(discordSRV.messagesConfig(execution.locale()).both.placeSpecifyTarget).withGameColor(NamedTextColor.RED));
                    return TargetLookupResult.INVALID;
                }
            }
        } else {
            throw new IllegalStateException("Unexpected CommandExecution");
        }

        if (lookupUser) {
            if (target.matches("\\d{17,22}")) {
                // Discord user id
                long id;
                try {
                    id = MiscUtil.parseLong(target);
                } catch (IllegalArgumentException ignored) {
                    execution.send(new Text(discordSRV.messagesConfig(execution.locale()).both.invalidTarget)
                                           .withGameColor(NamedTextColor.RED));
                    return TargetLookupResult.INVALID;
                }

                return new TargetLookupResult(true, null, id);
            } else if (target.startsWith("@")) {
                // Discord username
                String username = target.substring(1);
                JDA jda = discordSRV.jda();
                if (jda != null) {
                    List<User> users = jda.getUsersByName(username, true);

                    if (users.size() == 1) {
                        return new TargetLookupResult(true, null, users.get(0).getIdLong());
                    }
                }
            }
        }

        if (lookupPlayer) {
            UUID uuid;
            boolean shortUUID;
            if ((shortUUID = target.length() == 32) || target.length() == 36) {
                // Player UUID
                if (shortUUID) {
                    target = target.substring(0, 8) + "-" + target.substring(8, 12) + "-" + target.substring(12, 16)
                            + "-" + target.substring(16, 20) + "-" + target.substring(20);
                }

                try {
                    uuid = UUID.fromString(target);
                } catch (IllegalArgumentException ignored) {
                    execution.send(new Text(discordSRV.messagesConfig(execution.locale()).both.invalidTarget).withGameColor(NamedTextColor.RED));
                    return TargetLookupResult.INVALID;
                }
            } else {
                // Player name
                IPlayer playerByName = discordSRV.playerProvider().player(target);
                if (playerByName != null) {
                    uuid = playerByName.uniqueId();
                } else {
                    throw new IllegalStateException("lookup offline"); // TODO: lookup offline player
                }
            }

            return new TargetLookupResult(true, uuid, 0L);
        }

        return TargetLookupResult.INVALID;
    }

    public static class TargetLookupResult {

        public static TargetLookupResult INVALID = new TargetLookupResult(false, null, 0L);

        private final boolean valid;
        private final UUID playerUUID;
        private final long userId;

        public TargetLookupResult(boolean valid, UUID playerUUID, long userId) {
            this.valid = valid;
            this.playerUUID = playerUUID;
            this.userId = userId;
        }

        public boolean isValid() {
            return valid;
        }

        public boolean isPlayer() {
            return playerUUID != null;
        }

        public UUID getPlayerUUID() {
            return playerUUID;
        }

        public long getUserId() {
            return userId;
        }
    }
}
