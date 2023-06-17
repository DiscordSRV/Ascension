package com.discordsrv.api.discord.entity.channel;

import com.discordsrv.api.discord.entity.JDAEntity;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;

public interface DiscordForumChannel extends DiscordThreadContainer, JDAEntity<ForumChannel> {
}
