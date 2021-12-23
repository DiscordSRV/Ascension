package com.discordsrv.proxy.config.channels;

import com.discordsrv.api.discord.api.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.api.entity.message.SendableDiscordMessage;
import com.discordsrv.common.config.annotation.Untranslated;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class ServerSwitchMessageConfig {

    public boolean enabled = true;

    @Untranslated(Untranslated.Type.VALUE)
    public SendableDiscordMessage.Builder format = SendableDiscordMessage.builder()
            .addEmbed(
                    DiscordMessageEmbed.builder()
                            .setAuthor(
                                    "%player_display_name% switched from %from_server% to %to_server%",
                                    null,
                                    "%player_avatar_url%"
                            )
                            .setColor(0x5555FF)
                            .build()
            );
}
