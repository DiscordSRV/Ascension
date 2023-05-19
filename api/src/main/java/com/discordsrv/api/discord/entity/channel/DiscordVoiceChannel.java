package com.discordsrv.api.discord.entity.channel;

import com.discordsrv.api.discord.entity.JDAEntity;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

public interface DiscordVoiceChannel extends DiscordGuildMessageChannel, JDAEntity<VoiceChannel> {

    @Override
    default DiscordChannelType getType() {
        return DiscordChannelType.VOICE;
    }
}
