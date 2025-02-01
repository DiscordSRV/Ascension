/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.config.main;

import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.config.configurate.annotation.Constants;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ConfigSerializable
public class CustomCommandConfig {

    public static CustomCommandConfig defaultIp() {
        CustomCommandConfig config = new CustomCommandConfig();
        config.command = "ip";
        config.description = "Get the Minecraft server ip";
        config.ephemeral = true;
        config.response = SendableDiscordMessage.builder().setContent("`yourserveripchange.me`");
        return config;
    }

    public static CustomCommandConfig defaultHelloWorld() {
        CustomCommandConfig config = new CustomCommandConfig();
        config.command = "hello";
        config.description = "Greet a user";
        config.options.add(new OptionConfig());
        config.response = SendableDiscordMessage.builder()
                .addEmbed(DiscordMessageEmbed.builder().setDescription("Hello %option_target_user_name%").build());
        return config;
    }

    @Comment("The command in Discord, this can be in up to 3 parts (separated by spaces).\n"
            + "You cannot specify commands on the 2nd and 3rd layer for the same main command at once.\n"
            + "You cannot specify an action for the main command if you specify something for the same main command on the 2nd or 3rd layer")
    public String command = "";

    @Comment("The description of the command, will be shown to the user")
    public String description = "";

    @Comment("If the command output should only be visible to the user who ran the command")
    public boolean ephemeral = false;

    public List<OptionConfig> options = new ArrayList<>();

    @Comment("The Discord server id to register this command in\n"
            + "Use 0 for all Discord servers, or -1 to make the command global")
    public long serverId = 0;

    @Comment("Only one of the constraints has to be true to allow execution")
    public List<ConstraintConfig> constraints = new ArrayList<>(Collections.singletonList(new ConstraintConfig()));

    @Comment("A list of console commands to run upon this commands execution")
    public List<String> consoleCommandsToRun = new ArrayList<>();

    public SendableDiscordMessage.Builder response = SendableDiscordMessage.builder().setContent("test");

    @ConfigSerializable
    public static class OptionConfig {

        @Comment("Acceptable options are: %1")
        @Constants.Comment("STRING, LONG, DOUBLE, BOOLEAN, USER, CHANNEL, ROLE, MENTIONABLE, ATTACHMENT")
        public CommandOption.Type type = CommandOption.Type.USER;

        @Comment("The name of this option, will be shown to the user")
        public String name = "target_user";

        @Comment("The description of this option, will be shown to the user")
        public String description = "The user to greet";

        @Comment("If this option is required to run the command")
        public boolean required = true;

    }

    @ConfigSerializable
    public static class ConstraintConfig {

        @Comment("The role and user ids that should/should not be allowed to run this custom command")
        public List<Long> roleAndUserIds = new ArrayList<>();

        @Comment("true for blacklisting the specified roles and users, false for whitelisting")
        public boolean blacklist = true;
    }
}
