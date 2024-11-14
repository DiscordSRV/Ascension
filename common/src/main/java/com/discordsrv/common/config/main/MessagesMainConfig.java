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

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

@ConfigSerializable
public class MessagesMainConfig {

    @Comment("The language code for the default language, if left blank the system default will be used.\n"
            + "This should be in the ISO 639-1 format (for example \"en\"), or ISO 639-1, a underscore and a ISO 3166-1 country code to specify dialect (for example \"pt_BR\")")
    public String defaultLanguage = "en";

    @Comment("If there should be a messages file per language (based on the player's or user's language), otherwise using the default")
    public boolean multiple = false;

    @Comment("If all languages provided with DiscordSRV should be loaded into the messages directory, only functions when \"multiple\" is set to true")
    public boolean loadAllDefaults = true;
}
