/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.debug;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.debug.file.DebugFile;
import com.discordsrv.common.debug.file.KeyValueDebugFile;
import com.discordsrv.common.debug.file.TextDebugFile;
import com.discordsrv.common.plugin.Plugin;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DebugReport {

    private final List<DebugFile> files = new ArrayList<>();
    private final DiscordSRV discordSRV;

    public DebugReport(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    public void generate() {
        discordSRV.eventBus().publish(new DebugGenerateEvent(this));

        addFile(environment());
        addFile(plugins());
        for (Path debugLog : discordSRV.logger().getDebugLogs()) {
            addFile(readFile(1, debugLog));
        }
    }

    public void addFile(DebugFile file) {
        files.add(file);
    }

    public List<DebugFile> getFiles() {
        return files;
    }

    private DebugFile environment() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("discordSRV", discordSRV.getClass().getSimpleName());
        values.put("version", discordSRV.version());
        values.put("status", discordSRV.status().name());
        values.put("jdaStatus", discordSRV.jda().map(jda -> jda.getStatus().name()).orElse("JDA null"));
        values.put("platformLogger", discordSRV.platformLogger().getClass().getName());
        values.put("onlineMode", discordSRV.onlineMode().name());

        values.put("javaVersion", System.getProperty("java.version"));
        values.put("javaVendor", System.getProperty("java.vendor")
                + " (" + System.getProperty("java.vendor.url") + ")");

        values.put("operatingSystem", System.getProperty("os.name")
                + " " + System.getProperty("os.version")
                + " (" + System.getProperty("os.arch") + ")");

        return new KeyValueDebugFile(10, "environment.json", values);
    }

    private DebugFile plugins() {
        List<Plugin> plugins = discordSRV.pluginManager().getPlugins();
        StringBuilder builder = new StringBuilder("Plugins (" + plugins.size() + "):\n");

        for (Plugin plugin : plugins) {
            builder.append('\n')
                    .append(plugin.name())
                    .append(" v").append(plugin.version())
                    .append(" ").append(plugin.authors());
        }
        return new TextDebugFile(5, "plugins.txt", builder.toString());
    }

    private DebugFile readFile(int order, Path file) {
        String fileName = file.getFileName().toString();
        if (!Files.exists(file)) {
            return new TextDebugFile(order, fileName, "File does not exist");
        }

        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            return new TextDebugFile(order, fileName, String.join("\n", lines));
        } catch (IOException e) {
            return new TextDebugFile(order, fileName, ExceptionUtils.getStackTrace(e));
        }
    }
}
