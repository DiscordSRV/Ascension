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

package com.discordsrv.common.permission.game;

public enum Permission {

    // Commands
    // Admin
    COMMAND_DEBUG("command.admin.debug"),
    COMMAND_RELOAD("command.admin.reload"),
    COMMAND_BROADCAST("command.admin.broadcast"),
    COMMAND_RESYNC("command.admin.resync"),
    COMMAND_VERSION("command.admin.version"),
    COMMAND_LINK_OTHER("command.admin.link.other"),
    COMMAND_LINKED_OTHER("command.admin.linked.other"),
    COMMAND_UNLINK_OTHER("command.admin.linked.other"),
    // Player
    COMMAND_ROOT("command.player.root"),
    COMMAND_LINK("command.player.link"),
    COMMAND_LINKED("command.player.linked"),
    COMMAND_UNLINK("command.player.unlink"),

    // Mentions
    MENTION_USER("mention.user.base"),
    MENTION_USER_LOOKUP("mention.user.lookup"),
    MENTION_ROLE_MENTIONABLE("mention.role.mentionable"),
    MENTION_ROLE_ALL("mention.role.all"),
    MENTION_EVERYONE("mention.everyone"),

    // Misc
    UPDATE_NOTIFICATION("updatenotification"),
    SILENT_JOIN("silentjoin"),
    SILENT_QUIT("silentquit"),
    ;

    private final String permission;

    Permission(String permission) {
        this.permission = permission;
    }

    public String permission() {
        return "discordsrv." + permission;
    }
}
