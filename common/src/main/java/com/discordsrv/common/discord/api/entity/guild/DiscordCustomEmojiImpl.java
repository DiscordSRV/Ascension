package com.discordsrv.common.discord.api.entity.guild;

import com.discordsrv.api.discord.entity.guild.DiscordCustomEmoji;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;

public class DiscordCustomEmojiImpl implements DiscordCustomEmoji {

    private final CustomEmoji jda;

    public DiscordCustomEmojiImpl(CustomEmoji jda) {
        this.jda = jda;
    }

    @Override
    public CustomEmoji asJDA() {
        return jda;
    }

    @Override
    public long getId() {
        return jda.getIdLong();
    }

    @Override
    public String getName() {
        return jda.getName();
    }
}
