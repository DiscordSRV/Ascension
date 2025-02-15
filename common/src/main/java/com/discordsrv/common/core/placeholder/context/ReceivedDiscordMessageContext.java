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

package com.discordsrv.common.core.placeholder.context;

import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.api.placeholder.annotation.PlaceholderRemainder;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;

import java.util.ArrayList;
import java.util.List;

@PlaceholderPrefix("message_")
public class ReceivedDiscordMessageContext {

    private final DiscordSRV discordSRV;

    public ReceivedDiscordMessageContext(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    @Placeholder("reply")
    public Component reply(ReceivedDiscordMessage message, BaseChannelConfig config) {
        ReceivedDiscordMessage replyingTo = message.getReplyingTo();
        if (replyingTo == null) {
            return null;
        }

        String content = replyingTo.getContent();
        if (content == null) {
            return null;
        }

        Component component = discordSRV.componentFactory().minecraftSerialize(message, config, content);

        String replyFormat = config.discordToMinecraft.replyFormat;
        return ComponentUtil.fromAPI(
                discordSRV.componentFactory().textBuilder(replyFormat)
                        .applyPlaceholderService()
                        .addPlaceholder("message", component)
                        .addContext(replyingTo.getMember(), replyingTo.getAuthor(), replyingTo)
                        .build()
                // TODO: add contentRegexFilters to this
        );
    }

    @Placeholder("attachments")
    public Component attachments(
            ReceivedDiscordMessage message,
            BaseChannelConfig config,
            @PlaceholderRemainder(supportsNoValue = true) String suffix
    ) {
        String attachmentFormat = config.discordToMinecraft.attachmentFormat;
        List<Component> components = new ArrayList<>();
        for (ReceivedDiscordMessage.Attachment attachment : message.getAttachments()) {
            components.add(ComponentUtil.fromAPI(
                    discordSRV.componentFactory().textBuilder(attachmentFormat)
                            .applyPlaceholderService()
                            .addPlaceholder("file_name", attachment.fileName())
                            .addPlaceholder("file_url", attachment.url())
                            .build()
            ));
        }

        return Component.join(JoinConfiguration.separator(Component.text(suffix)), components);
    }
}
