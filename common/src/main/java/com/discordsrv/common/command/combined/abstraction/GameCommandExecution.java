package com.discordsrv.common.command.combined.abstraction;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.command.game.abstraction.GameCommandArguments;
import com.discordsrv.common.command.game.sender.ICommandSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.Collection;

public class GameCommandExecution implements CommandExecution {

    private final DiscordSRV discordSRV;
    private final ICommandSender sender;
    private final GameCommandArguments arguments;

    public GameCommandExecution(DiscordSRV discordSRV, ICommandSender sender, GameCommandArguments arguments) {
        this.discordSRV = discordSRV;
        this.sender = sender;
        this.arguments = arguments;
    }

    @Override
    public void setEphemeral(boolean ephemeral) {
        // NO-OP
    }

    @Override
    public String getArgument(String label) {
        return arguments.getString(label);
    }

    @Override
    public void send(Collection<Text> texts) {
        TextComponent.Builder builder = Component.text();
        for (Text text : texts) {
            builder.append(
                    Component.text(text.content())
                            .color(text.gameColor())
                            .decorations(text.gameFormatting(), true)
            );
        }
        sender.sendMessage(builder);
    }

    @Override
    public void runAsync(Runnable runnable) {
        discordSRV.scheduler().run(runnable);
    }
}
