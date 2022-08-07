package com.discordsrv.api.discord.events.interaction.command;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.interaction.DiscordInteractionHook;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.events.interaction.AbstractDeferrableInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent;

public class AbstractCommandInteractionEvent<E extends GenericCommandInteractionEvent>
        extends AbstractDeferrableInteractionEvent<E> {

    public AbstractCommandInteractionEvent(
            E jdaEvent,
            ComponentIdentifier identifier,
            DiscordUser user,
            DiscordGuildMember member,
            DiscordMessageChannel channel,
            DiscordInteractionHook interaction
    ) {
        super(jdaEvent, identifier, user, member, channel, interaction);
    }
}
