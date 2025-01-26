package com.discordsrv.common.discord.api.entity.channel;

import com.discordsrv.api.discord.entity.channel.DiscordChannelType;
import com.discordsrv.api.discord.entity.channel.DiscordMediaChannel;
import com.discordsrv.api.discord.entity.channel.DiscordThreadChannel;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.message.util.SendableDiscordMessageUtil;
import net.dv8tion.jda.api.entities.channel.concrete.MediaChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumPost;

import java.util.concurrent.CompletableFuture;

public class DiscordMediaChannelImpl extends AbstractDiscordForumChannel<MediaChannel> implements DiscordMediaChannel {

    public DiscordMediaChannelImpl(DiscordSRV discordSRV, MediaChannel channel) {
        super(discordSRV, channel);
    }

    @Override
    public DiscordChannelType getType() {
        return DiscordChannelType.MEDIA;
    }

    @Override
    public MediaChannel asJDA() {
        return channel;
    }

    @Override
    public CompletableFuture<DiscordThreadChannel> createPost(String name, SendableDiscordMessage message) {
        return thread(
                channel -> channel.createForumPost(name, SendableDiscordMessageUtil.toJDASend(message)),
                ForumPost::getThreadChannel
        );
    }

    @Override
    public String toString() {
        return "Media:" + getName() + "(" + Long.toUnsignedString(getId()) + ")";
    }
}
