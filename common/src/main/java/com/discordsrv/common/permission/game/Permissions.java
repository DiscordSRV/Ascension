/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Permissions {

    private Permissions() {}

    private static final List<PermissionTemplate> ALL_PERMISSION_TEMPLATES = new ArrayList<>();

    public static List<PermissionTemplate> getAllTemplates() {
        return Collections.unmodifiableList(ALL_PERMISSION_TEMPLATES);
    }

    // Commands
    // Admin
    public static final Static COMMAND_BYPASS = new Static("command.bypass", true);
    public static final Static COMMAND_DEBUG = new Static("command.debug", true);
    public static final Static COMMAND_PARSE = new Static("command.parse", true);
    public static final Static COMMAND_RELOAD = new Static("command.reload", true);
    public static final Static COMMAND_BROADCAST = new Static("command.broadcast", true);
    public static final Static COMMAND_RESYNC = new Static("command.resync", true);
    public static final Static COMMAND_VERSION = new Static("command.version", true);
    public static final Static COMMAND_LINK_OTHER = new Static("command.link.other", true);
    public static final Static COMMAND_LINKED_OTHER = new Static("command.linked.other", true);
    public static final Static COMMAND_UNLINK_OTHER = new Static("command.unlink.other", true);

    // Player
    public static final Static COMMAND_ROOT = new Static("command.root", false);
    public static final Static COMMAND_HELP = new Static("command.help", false);
    public static final Static COMMAND_LINK = new Static("command.link.self", false);
    public static final Static COMMAND_LINKED = new Static("command.linked.self", false);
    public static final Static COMMAND_UNLINK = new Static("command.unlink.self", false);

    // Mentions
    public static final Static MENTION_USER_ALL = new Static("mention.user.all", true);
    public static final Static MENTION_USER_LOOKUP = new Static("mention.user.lookup", true);
    public static final Static MENTION_ROLE_MENTIONABLE = new Static("mention.role.mentionable", true);
    public static final Static MENTION_ROLE_ALL = new Static("mention.role.all", true);
    public static final Static MENTION_EVERYONE = new Static("mention.everyone", true);
    public static final DynamicPattern MENTION_ROLE = new DynamicPattern("mention.role", "id", true);

    // Misc
    public static final Static UPDATE_NOTIFICATION = new Static("updatenotification", true);
    public static final Static SILENT_JOIN = new Static("silentjoin", true);
    public static final Static SILENT_QUIT = new Static("silentquit", true);

    public static class Static implements Permission {

        private final String permission;
        private final boolean requiresOpByDefault;

        protected Static(String permission, boolean requiresOpByDefault) {
            this.permission = permission;
            this.requiresOpByDefault = requiresOpByDefault;

            ALL_PERMISSION_TEMPLATES.add(this);
        }

        @Override
        public String permissionNode() {
            return permission;
        }

        @Override
        public boolean requiresOpByDefault() {
            return requiresOpByDefault;
        }

        @Override
        public PermissionTemplate template() {
            return this;
        }
    }

    public static class DynamicPattern implements PermissionTemplate.Parameterized {

        private final String permissionPrefix;
        private final String parameterName;
        private final boolean requiresOpByDefault;

        protected DynamicPattern(String permissionPrefix, String parameterName, boolean requiresOpByDefault) {
            this.permissionPrefix = permissionPrefix;
            this.parameterName = parameterName;
            this.requiresOpByDefault = requiresOpByDefault;

            ALL_PERMISSION_TEMPLATES.add(this);
        }

        public Dynamic with(String parameter) {
            return new Dynamic(this, parameter);
        }

        @Override
        public String permissionNode() {
            return permissionPrefix;
        }

        @Override
        public String parameterName() {
            return parameterName;
        }

        @Override
        public boolean requiresOpByDefault() {
            return requiresOpByDefault;
        }
    }

    public static class Dynamic implements Permission.Parameterized {

        private final DynamicPattern pattern;
        private final String parameter;

        protected Dynamic(DynamicPattern pattern, String parameter) {
            this.pattern = pattern;
            this.parameter = parameter;
        }

        @Override
        public String permissionNodePrefix() {
            return pattern.permissionPrefix;
        }

        @Override
        public String parameterName() {
            return pattern.parameterName;
        }

        @Override
        public String parameter() {
            return parameter;
        }

        @Override
        public boolean requiresOpByDefault() {
            return pattern.requiresOpByDefault;
        }

        @Override
        public PermissionTemplate.Parameterized template() {
            return pattern;
        }
    }
}
