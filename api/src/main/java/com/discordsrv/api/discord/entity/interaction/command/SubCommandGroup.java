/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.discordsrv.api.discord.entity.interaction.command;

import com.discordsrv.api.discord.entity.JDAEntity;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.List;

public class SubCommandGroup implements JDAEntity<SubcommandGroupData> {

    /**
     * Creates a sub command group.
     *
     * @param name the sub command group name
     * @param description the sub command group description
     * @param commands the commands within the sub command group
     * @return a new sub command group
     */
    @NotNull
    public static SubCommandGroup of(@NotNull String name, @NotNull String description, @NotNull DiscordCommand... commands) {
        return new SubCommandGroup(name, description, Arrays.asList(commands));
    }

    private final String name;
    private final String description;
    private final List<DiscordCommand> commands;

    private SubCommandGroup(String name, String description, List<DiscordCommand> commands) {
        this.name = name;
        this.description = description;
        this.commands = commands;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    @NotNull
    @Unmodifiable
    public List<DiscordCommand> getCommands() {
        return commands;
    }

    @Override
    public SubcommandGroupData asJDA() {
        return new SubcommandGroupData(name, description)
                .addSubcommands(commands.stream().map(DiscordCommand::asJDASubcommand).toArray(SubcommandData[]::new));
    }
}
