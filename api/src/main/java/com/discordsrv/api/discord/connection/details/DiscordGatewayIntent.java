/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.discord.connection.details;

import com.discordsrv.api.discord.entity.JDAEntity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.util.EnumSet;

public enum DiscordGatewayIntent implements JDAEntity<GatewayIntent> {

    GUILD_MEMBERS(GatewayIntent.GUILD_MEMBERS, "Server Members Intent"),
    GUILD_MODERATION(GatewayIntent.GUILD_MODERATION),
    GUILD_EXPRESSIONS(GatewayIntent.GUILD_EXPRESSIONS),
    GUILD_WEBHOOKS(GatewayIntent.GUILD_WEBHOOKS),
    GUILD_INVITES(GatewayIntent.GUILD_INVITES),
    GUILD_VOICE_STATES(GatewayIntent.GUILD_VOICE_STATES),
    GUILD_PRESENCES(GatewayIntent.GUILD_PRESENCES, "Presence Intent"),
    GUILD_MESSAGES(GatewayIntent.GUILD_MESSAGES),
    GUILD_MESSAGE_REACTIONS(GatewayIntent.GUILD_MESSAGE_REACTIONS),
    GUILD_MESSAGE_TYPING(GatewayIntent.GUILD_MESSAGE_TYPING),
    DIRECT_MESSAGES(GatewayIntent.DIRECT_MESSAGES),
    DIRECT_MESSAGE_REACTIONS(GatewayIntent.DIRECT_MESSAGE_REACTIONS),
    DIRECT_MESSAGE_TYPING(GatewayIntent.DIRECT_MESSAGE_TYPING),
    MESSAGE_CONTENT(GatewayIntent.MESSAGE_CONTENT, "Message Content Intent"),
    SCHEDULED_EVENTS(GatewayIntent.SCHEDULED_EVENTS),
    AUTO_MODERATION_CONFIGURATION(GatewayIntent.AUTO_MODERATION_CONFIGURATION),
    AUTO_MODERATION_EXECUTION(GatewayIntent.AUTO_MODERATION_EXECUTION),

    ;

    public static final EnumSet<DiscordGatewayIntent> PRIVILEGED;

    static {
        EnumSet<DiscordGatewayIntent> privileged = EnumSet.noneOf(DiscordGatewayIntent.class);
        for (DiscordGatewayIntent intent : values()) {
            if (intent.privileged()) {
                privileged.add(intent);
            }
        }

        PRIVILEGED = privileged;
    }

    public static DiscordGatewayIntent getByJda(GatewayIntent jda) {
        for (DiscordGatewayIntent value : values()) {
            if (value.asJDA() == jda) {
                return value;
            }
        }
        throw new IllegalArgumentException("This intent does not have a DiscordGatewayIntent");
    }

    private final GatewayIntent jda;
    private final String portalName;
    private final boolean privileged;

    DiscordGatewayIntent(GatewayIntent jda) {
        this(jda, null, false);
    }

    DiscordGatewayIntent(GatewayIntent jda, String portalName) {
        this(jda, portalName, true);
    }

    DiscordGatewayIntent(GatewayIntent jda, String portalName, boolean privileged) {
        this.jda = jda;
        this.portalName = portalName;
        this.privileged = privileged;
    }

    public String portalName() {
        return portalName;
    }

    public boolean privileged() {
        return privileged;
    }

    @Override
    public GatewayIntent asJDA() {
        return jda;
    }
}
