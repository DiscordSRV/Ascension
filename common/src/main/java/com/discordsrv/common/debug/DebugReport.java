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

package com.discordsrv.common.debug;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.configurate.manager.MessagesConfigSingleManager;
import com.discordsrv.common.config.configurate.manager.abstraction.ConfigurateConfigManager;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.connection.StorageConfig;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.debug.file.DebugFile;
import com.discordsrv.common.debug.file.KeyValueDebugFile;
import com.discordsrv.common.debug.file.TextDebugFile;
import com.discordsrv.common.exception.ConfigException;
import com.discordsrv.common.paste.Paste;
import com.discordsrv.common.paste.PasteService;
import com.discordsrv.common.plugin.Plugin;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.dv8tion.jda.api.JDA;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
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

    private static final int BIG_FILE_SPLIT_SIZE = 50000;

    private final List<DebugFile> files = new ArrayList<>();
    private final DiscordSRV discordSRV;

    public DebugReport(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    public void generate() {
        discordSRV.eventBus().publish(new DebugGenerateEvent(this));

        addFile(environment()); // 100
        addFile(plugins()); // 90
        for (Path debugLog : discordSRV.logger().getDebugLogs()) {
            addFile(readFile(80, debugLog));
        }
        addFile(config(79, discordSRV.configManager()));
        addFile(rawConfig(79, discordSRV.configManager()));
        for (MessagesConfigSingleManager<? extends MessagesConfig> manager : discordSRV.messagesConfigManager().getAllManagers().values()) {
            addFile(config(78, manager));
            addFile(rawConfig(78, manager));
        }
        addFile(activeLimitedConnectionsConfig()); // 77
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
        if (file != null) {
            files.add(file);
        }
    }

    private DebugFile environment() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("discordSRV", discordSRV.getClass().getName());
        values.put("version", discordSRV.versionInfo().version());
        values.put("gitRevision", discordSRV.versionInfo().gitRevision());
        values.put("gitBranch", discordSRV.versionInfo().gitBranch());
        values.put("buildTime", discordSRV.versionInfo().buildTime());
        values.put("status", discordSRV.status().name());
        JDA jda = discordSRV.jda();
        values.put("jdaStatus", jda != null ? jda.getStatus().name() : "JDA null");
        values.put("platformLogger", discordSRV.platformLogger().getClass().getName());
        values.put("onlineMode", discordSRV.onlineMode().name());
        values.put("offlineModeUuid", discordSRV.playerProvider().isAnyOffline());

        values.put("javaVersion", System.getProperty("java.version"));
        values.put("javaVendor", System.getProperty("java.vendor")
                + " (" + System.getProperty("java.vendor.url") + ")");

        values.put("operatingSystem", System.getProperty("os.name")
                + " (" + System.getProperty("os.arch") + ")");
        values.put("operatingSystemVersion", System.getProperty("os.version"));

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

        return new KeyValueDebugFile(100, "environment.json", values);
    }

    private DebugFile plugins() {
        List<Plugin> plugins = discordSRV.pluginManager().getPlugins()
                .stream()
                .sorted(Comparator.comparing(plugin -> plugin.name().toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());

        int order = 90;
        String fileName = "plugins.json";
        try {
            String json = discordSRV.json().writeValueAsString(plugins);
            return new TextDebugFile(order, fileName, json);
        } catch (JsonProcessingException e) {
            return exception(order, fileName, e);
        }
    }

    private DebugFile config(int order, ConfigurateConfigManager<?, ?> manager) {
        String fileName = "parsed_" + manager.fileName();
        try (StringWriter writer = new StringWriter()) {
            AbstractConfigurationLoader<CommentedConfigurationNode> loader = manager
                    .createLoader(manager.filePath(), manager.nodeOptions(true))
                    .sink(() -> new BufferedWriter(writer))
                    .build();
            manager.save(loader);

            return new TextDebugFile(order, fileName, writer.toString());
        } catch (IOException | ConfigException e) {
            return exception(order, fileName, e);
        }
    }

    private DebugFile rawConfig(int order, ConfigurateConfigManager<?, ?> manager) {
        return readFile(order, manager.filePath());
    }

    private DebugFile activeLimitedConnectionsConfig() {
        ConnectionConfig config = discordSRV.connectionConfig();
        StorageConfig.Pool poolConfig = config.storage.remote.poolOptions;

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("minecraft-auth.allow", config.minecraftAuth.allow);
        values.put("minecraft-auth.token_set", StringUtils.isNotBlank(config.minecraftAuth.token));

        values.put("storage.backend", config.storage.backend);
        values.put("storage.sql-table-prefix", config.storage.sqlTablePrefix);
        values.put("storage.remote.pool-options.keepalive-time", poolConfig.keepaliveTime);
        values.put("storage.remote.pool-options.maxiumum-lifetime", poolConfig.maximumLifetime);
        values.put("storage.remote.pool-options.maximum-pool-size", poolConfig.maximumPoolSize);
        values.put("storage.remote.pool-options.minimum-pool-size", poolConfig.minimumPoolSize);

        values.put("update.notification-enabled", config.update.notificationEnabled);
        values.put("update.notification-in-game", config.update.notificationInGame);
        values.put("update.first-party-notification", config.update.firstPartyNotification);
        values.put("update.github.enabled", config.update.github.enabled);
        values.put("update.github.api-token_set", StringUtils.isNotBlank(config.update.github.apiToken));
        values.put("update.security.enabled", config.update.security.enabled);
        values.put("update.security.force", config.update.security.force);

        return new KeyValueDebugFile(77, "connections.json", values, true);
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
            return exception(order, fileName, e);
        }
    }

    private DebugFile exception(int order, String fileName, Throwable throwable) {
        return new TextDebugFile(order, fileName, ExceptionUtils.getStackTrace(throwable));
    }
}
