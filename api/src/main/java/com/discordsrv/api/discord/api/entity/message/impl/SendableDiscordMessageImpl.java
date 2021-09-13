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

package com.discordsrv.api.discord.api.entity.message.impl;

import com.discordsrv.api.discord.api.entity.message.AllowedMention;
import com.discordsrv.api.discord.api.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SendableDiscordMessageImpl implements SendableDiscordMessage {

    private final String content;
    private final List<DiscordMessageEmbed> embeds;
    private final Set<AllowedMention> allowedMentions;
    private final String webhookUsername;
    private final String webhookAvatarUrl;

    protected SendableDiscordMessageImpl(String content,
                                         List<DiscordMessageEmbed> embeds,
                                         Set<AllowedMention> allowedMentions,
                                         String webhookUsername,
                                         String webhookAvatarUrl) {
        this.content = content;
        this.embeds = embeds;
        this.allowedMentions = allowedMentions;
        this.webhookUsername = webhookUsername;
        this.webhookAvatarUrl = webhookAvatarUrl;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public @NotNull List<DiscordMessageEmbed> getEmbeds() {
        return embeds;
    }

    @Override
    public Set<AllowedMention> getAllowedMentions() {
        return allowedMentions;
    }

    @Override
    public String getWebhookUsername() {
        return webhookUsername;
    }

    @Override
    public String getWebhookAvatarUrl() {
        return webhookAvatarUrl;
    }

    public static class BuilderImpl implements SendableDiscordMessage.Builder {

        private String content;
        private final List<DiscordMessageEmbed> embeds = new ArrayList<>();
        private final Set<AllowedMention> allowedMentions = new HashSet<>();
        private String webhookUsername;
        private String webhookAvatarUrl;

        @Override
        public String getContent() {
            return content;
        }

        @Override
        public @NotNull BuilderImpl setContent(String content) {
            this.content = content;
            return this;
        }

        @Override
        public @NotNull List<DiscordMessageEmbed> getEmbeds() {
            return embeds;
        }

        @Override
        public @NotNull Builder addEmbed(DiscordMessageEmbed embed) {
            this.embeds.add(embed);
            return this;
        }

        @Override
        public @NotNull Builder removeEmbed(DiscordMessageEmbed embed) {
            this.embeds.remove(embed);
            return this;
        }

        @Override
        public Set<AllowedMention> getAllowedMentions() {
            return allowedMentions;
        }

        @Override
        public @NotNull Builder addAllowedMention(AllowedMention allowedMention) {
            this.allowedMentions.add(allowedMention);
            return this;
        }

        @Override
        public @NotNull Builder removeAllowedMention(AllowedMention allowedMention) {
            this.allowedMentions.remove(allowedMention);
            return this;
        }

        @Override
        public String getWebhookUsername() {
            return webhookUsername;
        }

        @Override
        public @NotNull BuilderImpl setWebhookUsername(String webhookUsername) {
            this.webhookUsername = webhookUsername;
            return this;
        }

        @Override
        public String getWebhookAvatarUrl() {
            return webhookAvatarUrl;
        }

        @Override
        public @NotNull BuilderImpl setWebhookAvatarUrl(String webhookAvatarUrl) {
            this.webhookAvatarUrl = webhookAvatarUrl;
            return this;
        }

        @Override
        public @NotNull SendableDiscordMessage build() {
            return new SendableDiscordMessageImpl(content, embeds, allowedMentions, webhookUsername, webhookAvatarUrl);
        }

        @SuppressWarnings("MethodDoesntCallSuperMethod")
        @Override
        public Builder clone() {
            BuilderImpl clone = new BuilderImpl();
            clone.setContent(content);
            embeds.forEach(clone::addEmbed);
            allowedMentions.forEach(clone::addAllowedMention);
            clone.setWebhookUsername(webhookUsername);
            clone.setWebhookAvatarUrl(webhookAvatarUrl);
            return clone;
        }
    }
}
