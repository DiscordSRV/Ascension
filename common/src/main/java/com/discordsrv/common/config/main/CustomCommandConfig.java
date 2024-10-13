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

package com.discordsrv.common.config.main;

import com.discordsrv.api.discord.entity.interaction.command.CommandOption;
import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

import java.util.ArrayList;
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

    public String command = "";
    public String description = "";
    public boolean ephemeral = false;

    public List<OptionConfig> options = new ArrayList<>();

    public SendableDiscordMessage.Builder response = SendableDiscordMessage.builder().setContent("test");

    @ConfigSerializable
    public static class OptionConfig {

        public CommandOption.Type type = CommandOption.Type.USER;
        public String name = "target_user";
        public String description = "The user to greet";
        public boolean required = true;

    }
}
