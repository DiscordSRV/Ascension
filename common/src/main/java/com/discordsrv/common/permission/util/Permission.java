package com.discordsrv.common.permission.util;

public enum Permission {

    // Commands
    // Admin
    COMMAND_DEBUG("command.admin.debug"),
    COMMAND_RELOAD("command.admin.reload"),
    COMMAND_BROADCAST("command.admin.broadcast"),
    COMMAND_RESYNC("command.admin.resync"),
    COMMAND_VERSION("command.admin.version"),
    // Player
    COMMAND_ROOT("command.player.root"),
    COMMAND_LINK("command.player.link.base"),
    COMMAND_LINK_OTHER("command.player.link.other"),
    COMMAND_LINKED("command.player.linked.base"),
    COMMAND_LINKED_OTHER("command.player.linked.other"),

    // Mentions
    MENTION_USER("mention.user.base"),
    MENTION_USER_LOOKUP("mention.user.lookup"),
    MENTION_ROLE_MENTIONABLE("mention.role.mentionable"),
    MENTION_ROLE_ALL("mention.role.all"),
    MENTION_EVERYONE("mention.everyone"),

    // Misc
    UPDATE_NOTIFICATION("updatenotification"),
    ;

    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }

    public String permission() {
        return "discordsrv." + permission;
    }
}
