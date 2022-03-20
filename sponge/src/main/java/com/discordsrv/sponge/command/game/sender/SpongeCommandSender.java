/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.sponge.command.game.sender;

import com.discordsrv.common.command.game.sender.ICommandSender;
import com.discordsrv.sponge.SpongeDiscordSRV;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.service.permission.Subject;

import java.util.function.Supplier;

public class SpongeCommandSender implements ICommandSender {

    protected final SpongeDiscordSRV discordSRV;
    protected final Supplier<Subject> subjectSupplier;
    protected final Supplier<Audience> audienceSupplier;

    public SpongeCommandSender(SpongeDiscordSRV discordSRV, Supplier<Subject> subjectSupplier, Supplier<Audience> audienceSupplier) {
        this.discordSRV = discordSRV;
        this.subjectSupplier = subjectSupplier;
        this.audienceSupplier = audienceSupplier;
    }

    @Override
    public boolean hasPermission(String permission) {
        return subjectSupplier.get().hasPermission(permission);
    }

    @Override
    public void runCommand(String command) {
        try {
            Subject subject = subjectSupplier.get();
            discordSRV.game().server().commandManager().process((Subject & Audience) subject, command);
        } catch (CommandException ignored) {}
    }

    @Override
    public @NotNull Audience audience() {
        return audienceSupplier.get();
    }
}
