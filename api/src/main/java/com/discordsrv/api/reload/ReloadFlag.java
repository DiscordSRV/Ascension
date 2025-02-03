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

package com.discordsrv.api.reload;

import com.discordsrv.api.DiscordSRVApi;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public enum ReloadFlag {
    CONFIG(false),
    CONFIG_UPGRADE(false),
    LINKED_ACCOUNT_PROVIDER(false),
    STORAGE(true),
    DISCORD_CONNECTION(DiscordSRVApi::isReady),
    DISCORD_COMMANDS(false),
    TRANSLATIONS(false);

    public static final Set<ReloadFlag> LOAD = Collections.unmodifiableSet(
            Arrays.stream(values()).filter(flag -> flag != ReloadFlag.CONFIG_UPGRADE).collect(Collectors.toSet()));
    public static final Set<ReloadFlag> DEFAULT_FLAGS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Collections.singletonList(CONFIG)));

    private final Predicate<DiscordSRVApi> requiresConfirm;

    ReloadFlag(boolean requiresConfirm) {
        this(__ -> requiresConfirm);
    }

    ReloadFlag(Predicate<DiscordSRVApi> requiresConfirm) {
        this.requiresConfirm = requiresConfirm;
    }

    public boolean requiresConfirm(DiscordSRVApi discordSRV) {
        return requiresConfirm.test(discordSRV);
    }
}
