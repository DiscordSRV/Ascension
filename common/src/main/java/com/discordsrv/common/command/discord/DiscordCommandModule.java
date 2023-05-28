package com.discordsrv.common.command.discord;

import com.discordsrv.api.discord.events.interaction.command.CommandRegisterEvent;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.discord.commands.DiscordSRVDiscordCommand;
import com.discordsrv.common.module.type.AbstractModule;

public class DiscordCommandModule extends AbstractModule<DiscordSRV> {

    public DiscordCommandModule(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Subscribe
    public void onCommandRegister(CommandRegisterEvent event) {
        event.registerCommands(DiscordSRVDiscordCommand.get(discordSRV));
    }
}
