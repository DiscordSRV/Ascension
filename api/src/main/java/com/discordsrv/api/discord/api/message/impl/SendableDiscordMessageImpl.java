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

package com.discordsrv.api.discord.api.message.impl;

import com.discordsrv.api.discord.api.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.api.message.SendableDiscordMessage;

import java.util.ArrayList;
import java.util.List;

public class SendableDiscordMessageImpl implements SendableDiscordMessage {

    private final String content;
    private final List<DiscordMessageEmbed> embeds;
    private final String webhookUsername;
    private final String webhookAvatarUrl;

    public SendableDiscordMessageImpl(String content, List<DiscordMessageEmbed> embeds,
                                      String webhookUsername, String webhookAvatarUrl) {
        this.content = content;
        this.embeds = embeds;
        this.webhookUsername = webhookUsername;
        this.webhookAvatarUrl = webhookAvatarUrl;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public List<DiscordMessageEmbed> getEmbeds() {
        return embeds;
    }

    @Override
    public String getWebhookUsername() {
        return webhookUsername;
    }

    @Override
    public String getWebhookAvatarUrl() {
        return webhookAvatarUrl;
    }

    public static class BuilderImpl implements Builder {

        private String content;
        private final List<DiscordMessageEmbed> embeds = new ArrayList<>();
        private String webhookUsername;
        private String webhookAvatarUrl;

        @Override
        public String getContent() {
            return content;
        }

        @Override
        public BuilderImpl setContent(String content) {
            this.content = content;
            return this;
        }

        @Override
        public List<DiscordMessageEmbed> getEmbeds() {
            return embeds;
        }

        @Override
        public Builder addEmbed(DiscordMessageEmbed embed) {
            this.embeds.add(embed);
            return this;
        }

        @Override
        public Builder removeEmbed(DiscordMessageEmbed embed) {
            this.embeds.remove(embed);
            return this;
        }

        @Override
        public String getWebhookUsername() {
            return webhookUsername;
        }

        @Override
        public BuilderImpl setWebhookUsername(String webhookUsername) {
            this.webhookUsername = webhookUsername;
            return this;
        }

        @Override
        public String getWebhookAvatarUrl() {
            return webhookAvatarUrl;
        }

        @Override
        public BuilderImpl setWebhookAvatarUrl(String webhookAvatarUrl) {
            this.webhookAvatarUrl = webhookAvatarUrl;
            return this;
        }

        @Override
        public SendableDiscordMessage build() {
            return new SendableDiscordMessageImpl(content, embeds, webhookUsername, webhookAvatarUrl);
        }
    }
}
