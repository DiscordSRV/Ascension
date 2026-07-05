/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.config;

import com.discordsrv.common.config.main.generic.DiscordUserFilterConfig;
import com.discordsrv.common.config.main.generic.FilterMode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DiscordUserFilterConfigTest {

    @Test
    public void emptyTest() {
        DiscordUserFilterConfig config = new DiscordUserFilterConfig();
        config.filters.clear();

        assertFalse(config.included(false, false, 0, Collections.emptyList()));
        assertFalse(config.included(true, false, 0, Collections.emptyList()));
        assertFalse(config.included(true, true, 0, Collections.emptyList()));
    }

    @Test
    public void whitelistTest() {
        DiscordUserFilterConfig config = new DiscordUserFilterConfig();
        config.filters.clear();
        config.filters.add(new DiscordUserFilterConfig.SingleFilter(1, FilterMode.WHITELIST));
        config.filters.add(new DiscordUserFilterConfig.SingleFilter(2, FilterMode.WHITELIST));

        assertTrue(config.included(false, false, 1, Collections.emptyList()));
        assertTrue(config.included(false, false, 2, Collections.emptyList()));
        assertFalse(config.included(false, false, 3, Collections.emptyList()));
        assertTrue(config.included(false, false, 0, Collections.singletonList(1L)));
        assertTrue(config.included(false, false, 0, Collections.singletonList(2L)));
        assertFalse(config.included(false, false, 0, Collections.singletonList(3L)));
    }

    @Test
    public void blacklistTest() {
        DiscordUserFilterConfig config = new DiscordUserFilterConfig();
        config.filters.clear();
        config.filters.add(new DiscordUserFilterConfig.SingleFilter(2, FilterMode.BLACKLIST));
        config.filters.add(new DiscordUserFilterConfig.SingleFilter(3, FilterMode.BLACKLIST));

        assertTrue(config.included(false, false, 1, Collections.emptyList()));
        assertFalse(config.included(false, false, 2, Collections.emptyList()));
        assertFalse(config.included(false, false, 3, Collections.emptyList()));
        assertTrue(config.included(false, false, 0, Collections.singletonList(1L)));
        assertFalse(config.included(false, false, 0, Collections.singletonList(2L)));
        assertFalse(config.included(false, false, 0, Collections.singletonList(3L)));
    }

    @Test
    public void whiteAndBlacklistTest() {
        DiscordUserFilterConfig config = new DiscordUserFilterConfig();
        config.filters.clear();
        config.filters.add(new DiscordUserFilterConfig.SingleFilter(1, FilterMode.WHITELIST));
        config.filters.add(new DiscordUserFilterConfig.SingleFilter(2, FilterMode.BLACKLIST));
        config.filters.add(new DiscordUserFilterConfig.SingleFilter(3, FilterMode.BLACKLIST));

        assertTrue(config.included(false, false, 0, Collections.singletonList(1L)));
        assertFalse(config.included(false, false, 0, Arrays.asList(1L, 2L)));
        assertFalse(config.included(false, false, 0, Arrays.asList(1L, 3L)));
        assertFalse(config.included(false, false, 0, Arrays.asList(2L, 3L)));
        assertFalse(config.included(false, false, 0, Collections.singletonList(2L)));
    }

    @Test
    public void strictTest() {
        DiscordUserFilterConfig.Strict strictConfig = new DiscordUserFilterConfig.Strict();
        strictConfig.filters.clear();
        strictConfig.filters.add(new DiscordUserFilterConfig.SingleFilter(0, FilterMode.BLACKLIST));
        strictConfig.filters.add(new DiscordUserFilterConfig.SingleFilter(1, FilterMode.BLACKLIST));

        assertFalse(strictConfig.included(false, false, 0, Collections.emptyList()));
        assertFalse(strictConfig.included(false, false, 1, Collections.emptyList()));
        assertFalse(strictConfig.included(false, false, 2, Collections.emptyList()));
        assertFalse(strictConfig.included(false, false, 3, Collections.emptyList()));
        assertFalse(strictConfig.included(false, false, 4, Collections.singletonList(0L)));
        assertFalse(strictConfig.included(false, false, 4, Collections.singletonList(1L)));
        assertFalse(strictConfig.included(false, false, 4, Collections.singletonList(2L)));
        assertFalse(strictConfig.included(false, false, 4, Collections.singletonList(3L)));
        assertFalse(strictConfig.included(false, false, 4, Collections.singletonList(4L)));
    }

    @Test
    public void strictFunctionsTest() {
        DiscordUserFilterConfig.Strict strictConfig = new DiscordUserFilterConfig.Strict();
        strictConfig.filters.clear();
        strictConfig.filters.add(new DiscordUserFilterConfig.SingleFilter(0, FilterMode.WHITELIST));
        strictConfig.filters.add(new DiscordUserFilterConfig.SingleFilter(1, FilterMode.BLACKLIST));

        assertTrue(strictConfig.included(false, false, 0, Collections.emptyList()));
        assertFalse(strictConfig.included(false, false, 1, Collections.emptyList()));
        assertFalse(strictConfig.included(false, false, 2, Collections.emptyList()));
        assertFalse(strictConfig.included(false, false, 3, Collections.emptyList()));
        assertTrue(strictConfig.included(false, false, 4, Collections.singletonList(0L)));
        assertFalse(strictConfig.included(false, false, 0, Collections.singletonList(1L)));
        assertFalse(strictConfig.included(false, false, 4, Collections.singletonList(1L)));
        assertFalse(strictConfig.included(false, false, 4, Collections.singletonList(2L)));
        assertFalse(strictConfig.included(false, false, 4, Collections.singletonList(3L)));
        assertFalse(strictConfig.included(false, false, 4, Collections.singletonList(4L)));
    }

    @Test
    public void multiTest() {
        DiscordUserFilterConfig config = new DiscordUserFilterConfig();
        config.filters.clear();
        config.filters.add(new DiscordUserFilterConfig.SingleFilter(Arrays.asList(1L, 2L), FilterMode.WHITELIST));
        config.filters.add(new DiscordUserFilterConfig.SingleFilter(Arrays.asList(2L, 3L), FilterMode.BLACKLIST));

        assertTrue(config.included(false, false, 0, Arrays.asList(1L, 2L)));
        assertTrue(config.included(false, false, 0, Arrays.asList(1L, 2L, 9L)));
        assertFalse(config.included(false, false, 0, Arrays.asList(1L, 3L)));
        assertFalse(config.included(false, false, 0, Arrays.asList(1L, 3L, 9L)));
        assertFalse(config.included(false, false, 0, Arrays.asList(1L, 2L, 3L)));
        assertFalse(config.included(false, false, 0, Arrays.asList(1L, 2L, 3L, 9L)));
    }

    @Test
    public void botTest() {
        DiscordUserFilterConfig.WithBots config = new DiscordUserFilterConfig.WithBots();
        config.filters.clear();
        config.filters.add(new DiscordUserFilterConfig.SingleFilter(1L, FilterMode.WHITELIST));

        config.webhooks = FilterMode.BLACKLIST;

        config.bots = FilterMode.BLACKLIST;
        assertFalse(config.included(false, true, 1L, Collections.emptyList()));
        config.bots = FilterMode.WHITELIST;
        assertTrue(config.included(false, true, 1L, Collections.emptyList()));

        assertFalse(config.included(true, true, 1L, Collections.emptyList()));
        config.webhooks = FilterMode.WHITELIST;
        assertTrue(config.included(true, true, 1L, Collections.emptyList()));
    }
}
