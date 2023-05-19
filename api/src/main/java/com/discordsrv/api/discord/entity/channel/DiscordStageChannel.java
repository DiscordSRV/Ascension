package com.discordsrv.api.discord.entity.channel;

import com.discordsrv.api.discord.entity.JDAEntity;
import net.dv8tion.jda.api.entities.channel.concrete.StageChannel;

public interface DiscordStageChannel extends DiscordGuildMessageChannel, JDAEntity<StageChannel> {

    @Override
    default DiscordChannelType getType() {
        return DiscordChannelType.STAGE;
    }
}
