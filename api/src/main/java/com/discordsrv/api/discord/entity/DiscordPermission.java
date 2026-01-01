/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.discordsrv.api.discord.entity;

import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordPermission implements JDAEntity<Permission> {

    public static final DiscordPermission MANAGE_CHANNEL = get(Permission.MANAGE_CHANNEL);
    public static final DiscordPermission MANAGE_SERVER = get(Permission.MANAGE_SERVER);
    public static final DiscordPermission VIEW_AUDIT_LOGS = get(Permission.VIEW_AUDIT_LOGS);
    public static final DiscordPermission VIEW_CHANNEL = get(Permission.VIEW_CHANNEL);
    public static final DiscordPermission VIEW_GUILD_INSIGHTS = get(Permission.VIEW_GUILD_INSIGHTS);
    public static final DiscordPermission MANAGE_ROLES = get(Permission.MANAGE_ROLES);
    public static final DiscordPermission MANAGE_PERMISSIONS = get(Permission.MANAGE_PERMISSIONS);
    public static final DiscordPermission MANAGE_WEBHOOKS = get(Permission.MANAGE_WEBHOOKS);
    public static final DiscordPermission MANAGE_GUILD_EXPRESSIONS = get(Permission.MANAGE_GUILD_EXPRESSIONS);
    public static final DiscordPermission MANAGE_EVENTS = get(Permission.MANAGE_EVENTS);
    public static final DiscordPermission USE_EMBEDDED_ACTIVITIES = get(Permission.USE_EMBEDDED_ACTIVITIES);
    public static final DiscordPermission VIEW_CREATOR_MONETIZATION_ANALYTICS = get(Permission.VIEW_CREATOR_MONETIZATION_ANALYTICS);
    public static final DiscordPermission CREATE_SCHEDULED_EVENTS = get(Permission.CREATE_SCHEDULED_EVENTS);
    public static final DiscordPermission CREATE_INSTANT_INVITE = get(Permission.CREATE_INSTANT_INVITE);
    public static final DiscordPermission KICK_MEMBERS = get(Permission.KICK_MEMBERS);
    public static final DiscordPermission BAN_MEMBERS = get(Permission.BAN_MEMBERS);
    public static final DiscordPermission NICKNAME_CHANGE = get(Permission.NICKNAME_CHANGE);
    public static final DiscordPermission NICKNAME_MANAGE = get(Permission.NICKNAME_MANAGE);
    public static final DiscordPermission MODERATE_MEMBERS = get(Permission.MODERATE_MEMBERS);
    public static final DiscordPermission MESSAGE_ADD_REACTION = get(Permission.MESSAGE_ADD_REACTION);
    public static final DiscordPermission MESSAGE_SEND = get(Permission.MESSAGE_SEND);
    public static final DiscordPermission MESSAGE_TTS = get(Permission.MESSAGE_TTS);
    public static final DiscordPermission MESSAGE_MANAGE = get(Permission.MESSAGE_MANAGE);
    public static final DiscordPermission MESSAGE_EMBED_LINKS = get(Permission.MESSAGE_EMBED_LINKS);
    public static final DiscordPermission MESSAGE_ATTACH_FILES = get(Permission.MESSAGE_ATTACH_FILES);
    public static final DiscordPermission MESSAGE_HISTORY = get(Permission.MESSAGE_HISTORY);
    public static final DiscordPermission MESSAGE_MENTION_EVERYONE = get(Permission.MESSAGE_MENTION_EVERYONE);
    public static final DiscordPermission MESSAGE_EXT_EMOJI = get(Permission.MESSAGE_EXT_EMOJI);
    public static final DiscordPermission USE_APPLICATION_COMMANDS = get(Permission.USE_APPLICATION_COMMANDS);
    public static final DiscordPermission MESSAGE_EXT_STICKER = get(Permission.MESSAGE_EXT_STICKER);
    public static final DiscordPermission MESSAGE_ATTACH_VOICE_MESSAGE = get(Permission.MESSAGE_ATTACH_VOICE_MESSAGE);
    public static final DiscordPermission MESSAGE_SEND_POLLS = get(Permission.MESSAGE_SEND_POLLS);
    public static final DiscordPermission USE_EXTERNAL_APPLICATIONS = get(Permission.USE_EXTERNAL_APPLICATIONS);
    public static final DiscordPermission MANAGE_THREADS = get(Permission.MANAGE_THREADS);
    public static final DiscordPermission CREATE_PUBLIC_THREADS = get(Permission.CREATE_PUBLIC_THREADS);
    public static final DiscordPermission CREATE_PRIVATE_THREADS = get(Permission.CREATE_PRIVATE_THREADS);
    public static final DiscordPermission MESSAGE_SEND_IN_THREADS = get(Permission.MESSAGE_SEND_IN_THREADS);
    public static final DiscordPermission PRIORITY_SPEAKER = get(Permission.PRIORITY_SPEAKER);
    public static final DiscordPermission VOICE_STREAM = get(Permission.VOICE_STREAM);
    public static final DiscordPermission VOICE_CONNECT = get(Permission.VOICE_CONNECT);
    public static final DiscordPermission VOICE_SPEAK = get(Permission.VOICE_SPEAK);
    public static final DiscordPermission VOICE_MUTE_OTHERS = get(Permission.VOICE_MUTE_OTHERS);
    public static final DiscordPermission VOICE_DEAF_OTHERS = get(Permission.VOICE_DEAF_OTHERS);
    public static final DiscordPermission VOICE_MOVE_OTHERS = get(Permission.VOICE_MOVE_OTHERS);
    public static final DiscordPermission VOICE_USE_VAD = get(Permission.VOICE_USE_VAD);
    public static final DiscordPermission VOICE_USE_SOUNDBOARD = get(Permission.VOICE_USE_SOUNDBOARD);
    public static final DiscordPermission VOICE_USE_EXTERNAL_SOUNDS = get(Permission.VOICE_USE_EXTERNAL_SOUNDS);
    public static final DiscordPermission VOICE_SET_STATUS = get(Permission.VOICE_SET_STATUS);
    public static final DiscordPermission REQUEST_TO_SPEAK = get(Permission.REQUEST_TO_SPEAK);
    public static final DiscordPermission ADMINISTRATOR = get(Permission.ADMINISTRATOR);

    private static final Map<Permission, DiscordPermission> PERMISSIONS = new ConcurrentHashMap<>();

    @NotNull
    public static DiscordPermission get(int offset) {
        return get(Permission.getFromOffset(offset));
    }

    private static DiscordPermission get(Permission permission) {
        return PERMISSIONS.computeIfAbsent(permission, DiscordPermission::new);
    }

    private final Permission permission;

    private DiscordPermission(Permission permission) {
        this.permission = permission;
    }

    public int getOffset() {
        return permission.getOffset();
    }

    @Override
    public Permission asJDA() {
        return permission;
    }
}
