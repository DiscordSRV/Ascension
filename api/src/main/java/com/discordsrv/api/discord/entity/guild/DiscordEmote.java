package com.discordsrv.api.discord.entity.guild;

import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.Snowflake;
import net.dv8tion.jda.api.entities.Emote;

public interface DiscordEmote extends JDAEntity<Emote>, Snowflake {

    String getName();

}
