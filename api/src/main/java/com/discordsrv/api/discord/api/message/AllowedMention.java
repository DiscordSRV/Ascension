/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.discord.api.message;

import net.dv8tion.jda.api.entities.Message;

public interface AllowedMention {

    AllowedMention EVERYONE = Standard.EVERYONE;
    AllowedMention ALL_ROLES = Standard.ROLE;
    AllowedMention ALL_USERS = Standard.USER;

    static AllowedMention user(String id) {
        return new Snowflake(id, true);
    }

    static AllowedMention role(String id) {
        return new Snowflake(id, false);
    }

    enum Standard implements AllowedMention {

        EVERYONE(Message.MentionType.EVERYONE),
        ROLE(Message.MentionType.ROLE),
        USER(Message.MentionType.USER),
        ;

        private final Message.MentionType mentionType;

        Standard(Message.MentionType mentionType) {
            this.mentionType = mentionType;
        }

        public Message.MentionType getMentionType() {
            return mentionType;
        }
    }

    class Snowflake implements AllowedMention {

        private final String id;
        private final boolean user;

        public Snowflake(String id, boolean user) {
            this.id = id;
            this.user = user;
        }

        public String getId() {
            return id;
        }

        public boolean isUser() {
            return user;
        }
    }
}
