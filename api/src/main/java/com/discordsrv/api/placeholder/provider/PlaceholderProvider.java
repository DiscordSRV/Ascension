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

package com.discordsrv.api.placeholder.provider;

import com.discordsrv.api.placeholder.PlaceholderLookupResult;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * A placeholder provider used internally by DiscordSRV for {@link Placeholder}.
 * API users should use the {@link com.discordsrv.api.event.events.placeholder.PlaceholderLookupEvent} instead.
 */
public interface PlaceholderProvider {

    @NotNull
    PlaceholderLookupResult lookup(@NotNull String placeholder, @NotNull Set<Object> context);
}
