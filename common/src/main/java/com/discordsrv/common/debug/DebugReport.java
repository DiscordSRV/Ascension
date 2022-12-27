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
import com.discordsrv.common.paste.Paste;
import com.discordsrv.common.paste.PasteService;
import com.discordsrv.common.plugin.Plugin;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.dv8tion.jda.api.JDA;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class DebugReport {

    private static final int BIG_FILE_SPLIT_SIZE = 20000;

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

    public Paste upload(PasteService service) throws Throwable {
        files.sort(Comparator.comparing(DebugFile::order).reversed());

        ArrayNode files = discordSRV.json().createArrayNode();
        for (DebugFile file : this.files) {
            int length = file.content().length();
            if (length >= BIG_FILE_SPLIT_SIZE) {
                ObjectNode node = discordSRV.json().createObjectNode();
                node.put("name", file.name());
                try {
                    Paste paste = service.uploadFile(convertToJson(file).toString().getBytes(StandardCharsets.UTF_8));
                    node.put("url", paste.url());
                    node.put("decryption_key", new String(Base64.getUrlEncoder().encode(paste.decryptionKey()), StandardCharsets.UTF_8));
                    node.put("length", length);
                } catch (Throwable e) {
                    node.put("content", "Failed to upload file\n\n" + ExceptionUtils.getStackTrace(e));
                }
                files.add(node);
                continue;
            }

            files.add(convertToJson(file));
        }

        return service.uploadFile(files.toString().getBytes(StandardCharsets.UTF_8));
    }

    public Path zip() throws Throwable {
        Path zipPath = discordSRV.dataDirectory().resolve("debug-" + System.currentTimeMillis() + ".zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (DebugFile file : files) {
                zipOutputStream.putNextEntry(new ZipEntry(file.name()));

                byte[] data = file.content().getBytes(StandardCharsets.UTF_8);
                zipOutputStream.write(data, 0, data.length);
                zipOutputStream.closeEntry();
            }
        }

        return zipPath;
    }

    private ObjectNode convertToJson(DebugFile file) {
        ObjectNode node = discordSRV.json().createObjectNode();
        node.put("name", file.name());
        node.put("content", file.content());
        return node;
    }

    public void addFile(DebugFile file) {
        files.add(file);
    }

    private DebugFile environment() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("discordSRV", discordSRV.getClass().getName());
        values.put("version", discordSRV.version());
        values.put("gitRevision", discordSRV.gitRevision());
        values.put("gitBranch", discordSRV.gitBranch());
        values.put("status", discordSRV.status().name());
        JDA jda = discordSRV.jda();
        values.put("jdaStatus", jda != null ? jda.getStatus().name() : "JDA null");
        values.put("platformLogger", discordSRV.platformLogger().getClass().getName());
        values.put("onlineMode", discordSRV.onlineMode().name());

        values.put("javaVersion", System.getProperty("java.version"));
        values.put("javaVendor", System.getProperty("java.vendor")
                + " (" + System.getProperty("java.vendor.url") + ")");

        values.put("operatingSystem", System.getProperty("os.name")
                + " " + System.getProperty("os.version")
                + " (" + System.getProperty("os.arch") + ")");

        Runtime runtime = Runtime.getRuntime();
        values.put("cores", runtime.availableProcessors());
        values.put("freeMemory", runtime.freeMemory());
        values.put("totalMemory", runtime.totalMemory());
        long maxMemory = runtime.maxMemory();
        values.put("maxMemory", maxMemory == Long.MAX_VALUE ? -1 : maxMemory);

        try {
            FileStore store = Files.getFileStore(discordSRV.dataDirectory());
            values.put("usableSpace", store.getUsableSpace());
            values.put("totalSpace", store.getTotalSpace());
        } catch (IOException ignored) {}

        boolean docker = false;
        try {
            docker = Files.readAllLines(Paths.get("/proc/1/cgroup"))
                    .stream().anyMatch(str -> str.contains("/docker/"));
        } catch (IOException ignored) {}
        values.put("docker", docker);

        return new KeyValueDebugFile(10, "environment.json", values);
    }

    private DebugFile plugins() {
        List<Plugin> plugins = discordSRV.pluginManager().getPlugins()
                .stream()
                .sorted(Comparator.comparing(plugin -> plugin.name().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());

        int longestName = 0;
        int longestVersion = 0;
        for (Plugin plugin : plugins) {
            longestName = Math.max(longestName, plugin.name().length());
            longestVersion = Math.max(longestVersion, plugin.version().length());
        }

        longestName++;
        longestVersion++;
        StringBuilder builder = new StringBuilder("Plugins (" + plugins.size() + "):\n");

        for (Plugin plugin : plugins) {
            builder.append('\n')
                    .append(StringUtils.rightPad(plugin.name(), longestName))
                    .append(" v").append(StringUtils.rightPad(plugin.version(), longestVersion))
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
