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

package com.discordsrv.common.feature.mention;

import com.discordsrv.api.discord.entity.message.AllowedMention;
import com.discordsrv.common.permission.game.Permission;
import com.discordsrv.common.permission.game.Permissions;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.util.Objects;
import java.util.regex.Pattern;

public class Mention {

    private final Pattern search;
    private final int searchLength;
    private final String mention;
    private final Type type;
    private final long id;
    private final boolean mentionable;
    private final AllowedMention allowedMention;
    private final Permission permission;

    public Mention(Role role) {
        this(
                "@" + role.getName(),
                role.getAsMention(),
                Type.ROLE,
                role.getIdLong(),
                role.isMentionable(),
                AllowedMention.role(role.getIdLong()),
                Permission.of("mention.role." + Long.toUnsignedString(role.getIdLong()))
        );
    }

    public Mention(Role role, String rawMention) {
        this(
                rawMention,
                rawMention,
                Type.ROLE,
                role.getIdLong(),
                false,
                AllowedMention.EVERYONE,
                Permissions.MENTION_EVERYONE
        );
    }

    public Mention(Member member) {
        this(
                "@" + member.getUser().getName(),
                member.getAsMention(),
                Type.USER,
                member.getIdLong(),
                true,
                AllowedMention.user(member.getIdLong()),
                Permissions.MENTION_USER_ALL
        );
    }

    public Mention(GuildChannel channel) {
        this(
                "#" + channel.getName(),
                channel.getAsMention(),
                Type.CHANNEL,
                channel.getIdLong(),
                true,
                null,
                null
        );
    }

    public Mention(
            String search,
            String mention,
            Type type,
            long id,
            boolean mentionable,
            AllowedMention allowedMention,
            Permission permission
    ) {
        this.search = Pattern.compile(search, Pattern.LITERAL);
        this.searchLength = search.length();
        this.mention = mention;
        this.type = type;
        this.id = id;
        this.mentionable = mentionable;
        this.allowedMention = allowedMention;
        this.permission = permission;
    }

    public String plain() {
        return search.pattern();
    }

    public Pattern search() {
        return search;
    }

    public int searchLength() {
        return searchLength;
    }

    public String discordMention() {
        return mention;
    }

    public Type type() {
        return type;
    }

    public long id() {
        return id;
    }

    /**
     * For roles if the role can be mentioned by anyone.
     */
    public boolean mentionable() {
        return mentionable;
    }

    public AllowedMention allowedMention() {
        return allowedMention;
    }

    public Permission permission() {
        return permission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mention that = (Mention) o;
        return type == that.type && id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    @Override
    public String toString() {
        return "CachedMention{pattern=" + search.pattern() + ",mention=" + mention + "}";
    }

    public enum Type {
        USER(3),
        CHANNEL(2),
        ROLE(2),

        EVERYONE(1),
        HERE(1);

        private final int priority;

        Type(int priority) {
            this.priority = priority;
        }

        public int priority() {
            return priority;
        }
    }
}
