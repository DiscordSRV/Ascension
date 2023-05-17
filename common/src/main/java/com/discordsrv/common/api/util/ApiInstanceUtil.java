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

package com.discordsrv.common.api.util;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.common.DiscordSRV;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@ApiStatus.Internal
public final class ApiInstanceUtil {

    private ApiInstanceUtil() {}

    @ApiStatus.Internal
    public static void setInstance(@NotNull DiscordSRV discordSRV) {
        // Avoids illegal access
        try {
            Class<?> apiProviderClass = Class.forName("com.discordsrv.api.DiscordSRVApi$InstanceHolder");
            Method provideMethod = apiProviderClass.getDeclaredMethod("provide", DiscordSRVApi.class);
            provideMethod.setAccessible(true);
            provideMethod.invoke(null, discordSRV);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            discordSRV.logger().error("Failed to set API instance", e);
        }
    }
}
