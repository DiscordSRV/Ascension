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

package com.discordsrv.common.core.placeholder.context;

import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.common.DiscordSRV;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.function.Function;

public class MemoryContext {

    private final DiscordSRV discordSRV;

    public MemoryContext(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    private Runtime runtime() {
        return Runtime.getRuntime();
    }

    @Placeholder("memory_free")
    public Bytes memoryFree() {
        return new Bytes(runtime().freeMemory());
    }

    @Placeholder("memory_total")
    public Bytes memoryTotal() {
        return new Bytes(runtime().totalMemory());
    }

    @Placeholder("memory_max")
    public Bytes memoryMax() {
        return new Bytes(runtime().maxMemory());
    }

    @Placeholder("memory_used")
    public Bytes memoryUsed() {
        long used = runtime().totalMemory() - runtime().freeMemory();
        return new Bytes(used);
    }

    @Placeholder("memory_available")
    public Bytes memoryAvailable() {
        long used = runtime().totalMemory() - runtime().freeMemory();
        long available = runtime().maxMemory() - used;
        return new Bytes(available);
    }

    private FileStore fileStore() throws IOException {
        return Files.getFileStore(discordSRV.dataDirectory());
    }

    @Placeholder("disk_usable")
    public Bytes diskFree() throws IOException {
        return new Bytes(fileStore().getUsableSpace());
    }

    @Placeholder("disk_total")
    public Bytes diskTotal() throws IOException {
        return new Bytes(fileStore().getTotalSpace());
    }

    @Placeholder("disk_unallocated")
    public Bytes diskUnallocated() throws IOException {
        return new Bytes(fileStore().getUnallocatedSpace());
    }

    @Placeholder("disk_allocated")
    public Bytes diskAllocated() throws IOException {
        FileStore fileStore = fileStore();
        long allocated = fileStore.getTotalSpace() - fileStore.getUnallocatedSpace();
        return new Bytes(allocated);
    }

    @PlaceholderPrefix("bytes_")
    public static class Bytes {

        private static final double FACTOR = 1024.0;
        private static final Pair<Function<Bytes, Number>, String>[] PREFIXES = getPrefixes();

        @SuppressWarnings("unchecked")
        private static Pair<Function<Bytes, Number>, String>[] getPrefixes() {
            return Arrays.asList(
                    Pair.of((Function<Bytes, Number>) Bytes::bytes, "B"),
                    Pair.of((Function<Bytes, Number>) Bytes::kilobytes, "KB"),
                    Pair.of((Function<Bytes, Number>) Bytes::megabytes, "MB"),
                    Pair.of((Function<Bytes, Number>) Bytes::gigabytes, "GB"),
                    Pair.of((Function<Bytes, Number>) Bytes::terabytes, "TB"),
                    Pair.of((Function<Bytes, Number>) Bytes::petabytes, "PB"),
                    Pair.of((Function<Bytes, Number>) Bytes::zettabytes, "ZB"),
                    Pair.of((Function<Bytes, Number>) Bytes::yottabytes, "YB"),
                    Pair.of((Function<Bytes, Number>) Bytes::ronnabytes, "RB"),
                    Pair.of((Function<Bytes, Number>) Bytes::quettabytes, "QB")
            ).toArray(new Pair[0]);
        }

        private final long bytes;

        public Bytes(long bytes) {
            this.bytes = bytes;
        }

        @Override
        public String toString() {
            int amount = 0;
            double currentBytes = bytes;
            while (amount < PREFIXES.length && currentBytes > FACTOR) {
                currentBytes /= FACTOR;
                amount++;
            }

            return (Math.round(currentBytes * 10.0) / 10.0) + PREFIXES[amount].getValue();
        }

        @Placeholder("bytes")
        public long bytes() {
            return bytes;
        }

        @Placeholder("kilobytes")
        public double kilobytes() {
            return bytes() / FACTOR;
        }

        @Placeholder("megabytes")
        public double megabytes() {
            return kilobytes() / FACTOR;
        }

        @Placeholder("gigabytes")
        public double gigabytes() {
            return megabytes() / FACTOR;
        }

        @Placeholder("terabytes")
        public double terabytes() {
            return gigabytes() / FACTOR;
        }

        @Placeholder("petabytes")
        public double petabytes() {
            return terabytes() / FACTOR;
        }

        @Placeholder("exabytes")
        public double exabytes() {
            return petabytes() / FACTOR;
        }

        @Placeholder("zettabytes")
        public double zettabytes() {
            return exabytes() / FACTOR;
        }

        @Placeholder("yottabytes")
        public double yottabytes() {
            return zettabytes() / FACTOR;
        }

        @Placeholder("ronnabytes")
        public double ronnabytes() {
            return yottabytes() / FACTOR;
        }

        @Placeholder("quettabytes")
        public double quettabytes() {
            return ronnabytes() / FACTOR;
        }
    }
}
