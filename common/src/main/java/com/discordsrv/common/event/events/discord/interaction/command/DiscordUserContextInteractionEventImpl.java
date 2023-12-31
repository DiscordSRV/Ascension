package com.discordsrv.common.event.events.discord.interaction.command;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.interaction.DiscordInteractionHook;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.event.events.discord.interaction.command.DiscordUserContextInteractionEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.discord.api.entity.component.DiscordInteractionHookImpl;
import com.discordsrv.common.discord.api.entity.message.util.SendableDiscordMessageUtil;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;

import java.util.concurrent.CompletableFuture;

public class DiscordUserContextInteractionEventImpl extends DiscordUserContextInteractionEvent {

    private final DiscordSRV discordSRV;

    public DiscordUserContextInteractionEventImpl(
            DiscordSRV discordSRV,
            UserContextInteractionEvent jdaEvent,
            ComponentIdentifier identifier,
            DiscordUser user,
            DiscordGuildMember member,
            DiscordMessageChannel channel, DiscordInteractionHook interaction) {
        super(jdaEvent, identifier, user, member, channel, interaction);
        this.discordSRV = discordSRV;
    }

    @Override
    public CompletableFuture<DiscordInteractionHook> reply(SendableDiscordMessage message, boolean ephemeral) {
        return discordSRV.discordAPI().mapExceptions(
                () -> jdaEvent.reply(SendableDiscordMessageUtil.toJDASend(message)).setEphemeral(ephemeral).submit()
                        .thenApply(ih -> new DiscordInteractionHookImpl(discordSRV, ih))
        );
    }
}
