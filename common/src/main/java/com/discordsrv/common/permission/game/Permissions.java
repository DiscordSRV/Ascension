/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

public enum Permissions implements Permission {

    // Commands
    // Admin
    COMMAND_DEBUG("command.debug", true),
    COMMAND_RELOAD("command.reload", true),
    COMMAND_BROADCAST("command.broadcast", true),
    COMMAND_RESYNC("command.resync", true),
    COMMAND_VERSION("command.version", true),
    COMMAND_LINK_OTHER("command.link.other", true),
    COMMAND_LINKED_OTHER("command.linked.other", true),
    COMMAND_UNLINK_OTHER("command.unlink.other", true),
    // Player
    COMMAND_ROOT("command.root", false),
    COMMAND_LINK("command.link.self", false),
    COMMAND_LINKED("command.linked.self", false),
    COMMAND_UNLINK("command.unlink.self", false),

    // Mentions
    MENTION_USER("mention.user.base", true),
    MENTION_USER_LOOKUP("mention.user.lookup", true),
    MENTION_ROLE_MENTIONABLE("mention.role.mentionable", true),
    MENTION_ROLE_ALL("mention.role.all", true),
    MENTION_EVERYONE("mention.everyone", true),

    // Misc
    UPDATE_NOTIFICATION("updatenotification", true),
    SILENT_JOIN("silentjoin", true),
    SILENT_QUIT("silentquit", true),
    ;

    private final String permission;
    private final boolean requiresOpByDefault;

    Permissions(String permission, boolean requiresOpByDefault) {
        this.permission = permission;
        this.requiresOpByDefault = requiresOpByDefault;
    }

    public String permission() {
        return "discordsrv." + permission;
    }

    /**
     * If a given permission's default should be OP, rather than being granted by default.
     * @return {@code true} if the permission should be restricted to, at least OPs
     */
    public boolean requiresOpByDefault() {
        return requiresOpByDefault;
    }
}
