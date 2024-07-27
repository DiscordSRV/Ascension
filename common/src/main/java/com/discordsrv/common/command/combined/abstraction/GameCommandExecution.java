/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.discordsrv.common.command.combined.abstraction;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.command.game.abstraction.GameCommandArguments;
import com.discordsrv.common.command.game.sender.ICommandSender;
import com.discordsrv.common.config.messages.MessagesConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

public class GameCommandExecution implements CommandExecution {

    private static final TextReplacementConfig URL_REPLACEMENT = TextReplacementConfig.builder()
            .match(Pattern.compile("^https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"))
            .replacement((matchResult, builder) -> {
                String url = matchResult.group();
                return builder.clickEvent(ClickEvent.openUrl(url));
            })
            .build();

    private final DiscordSRV discordSRV;
    private final ICommandSender sender;
    private final GameCommandArguments arguments;
    private final String label;

    public GameCommandExecution(DiscordSRV discordSRV, ICommandSender sender, GameCommandArguments arguments, String label) {
        this.discordSRV = discordSRV;
        this.sender = sender;
        this.arguments = arguments;
        this.label = label;
    }

    @Override
    public Locale locale() {
        return sender instanceof IPlayer ? ((IPlayer) sender).locale() : null;
    }

    @Override
    public MessagesConfig messages() {
        return discordSRV.messagesConfig(locale());
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
    public void send(Collection<Text> texts, Collection<Text> extra) {
        TextComponent.Builder builder = render(texts);
        if (!extra.isEmpty()) {
            builder.hoverEvent(HoverEvent.showText(render(extra)));
        }
        sender.sendMessage(builder.build().replaceText(URL_REPLACEMENT));
    }

    @Override
    public void send(Component minecraft, String discord) {
        sender.sendMessage(minecraft);
    }

    private TextComponent.Builder render(Collection<Text> texts) {
        TextComponent.Builder builder = Component.text();
        for (Text text : texts) {
            builder.append(
                    Component.text(text.content())
                            .color(text.gameColor())
                            .decorations(text.gameFormatting(), true)
            );
        }
        return builder;
    }

    @Override
    public void runAsync(Runnable runnable) {
        discordSRV.scheduler().run(runnable);
    }

    public ICommandSender getSender() {
        return sender;
    }

    public String getLabel() {
        return label;
    }
}
