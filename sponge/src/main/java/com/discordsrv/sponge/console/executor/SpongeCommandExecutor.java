/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.sponge.console.executor;

import com.discordsrv.common.command.executor.AdventureCommandExecutorProxy;
import com.discordsrv.common.command.game.executor.CommandExecutor;
import com.discordsrv.sponge.SpongeDiscordSRV;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.SystemSubject;
import org.spongepowered.api.command.exception.CommandException;

import java.util.function.Consumer;

public class SpongeCommandExecutor implements CommandExecutor {

    private final SpongeDiscordSRV discordSRV;
    private final SystemSubject subject;

    public SpongeCommandExecutor(SpongeDiscordSRV discordSRV, Consumer<Component> componentConsumer) {
        this.discordSRV = discordSRV;
        this.subject = (SystemSubject) new AdventureCommandExecutorProxy(
                discordSRV.game().systemSubject(),
                componentConsumer
        ).getProxy();
    }

    @Override
    public void runCommand(String command) {
        discordSRV.scheduler().runOnMainThread(() -> runOnMain(command));
    }

    private void runOnMain(String command) {
        try {
            discordSRV.game().server().commandManager().process(subject, command);
        } catch (CommandException e) {
            Component message = e.componentMessage();
            if (message != null) {
                subject.sendMessage(message);
            }
        }
    }
}
