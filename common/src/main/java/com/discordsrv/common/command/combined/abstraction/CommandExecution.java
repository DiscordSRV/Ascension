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

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.common.config.messages.MessagesConfig;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

public interface CommandExecution {

    Locale locale();
    MessagesConfig messages();

    void setEphemeral(boolean ephemeral);

    String getArgument(String label);

    default void send(Text... texts) {
        send(Arrays.asList(texts));
    }

    default void send(Collection<Text> texts) {
        send(texts, Collections.emptyList());
    }

    void send(Collection<Text> texts, Collection<Text> extra);

    default void send(MinecraftComponent minecraft, String discord) {
        send(ComponentUtil.fromAPI(minecraft), discord);
    }

    void send(Component minecraft, String discord);

    void runAsync(Runnable runnable);
}
