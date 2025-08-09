/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.core.debug;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.plugin.Plugin;
import com.discordsrv.common.config.configurate.manager.MessagesConfigSingleManager;
import com.discordsrv.common.config.configurate.manager.abstraction.ConfigurateConfigManager;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.connection.StorageConfig;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.core.paste.Paste;
import com.discordsrv.common.core.paste.PasteService;
import com.discordsrv.common.core.debug.file.DebugFile;
import com.discordsrv.common.core.debug.file.KeyValueDebugFile;
import com.discordsrv.common.core.debug.file.TextDebugFile;
import com.discordsrv.common.core.scheduler.Scheduler;
import com.discordsrv.common.util.function.CheckedSupplier;
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
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.discordsrv.common.command.combined.commands.DebugCommand.KEY_ENCODER;

public class DebugReport {

    private static final int BIG_FILE_SPLIT_SIZE = 100_000;

    private final List<DebugFile.Named> files = new ArrayList<>();
    private final DiscordSRV discordSRV;

    public DebugReport(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    public void generate() {
        discordSRV.eventBus().publish(new DebugGenerateEvent(this));

        addFile("environment.json", 100, this::environment);
        addFile("plugins.json", 90, this::plugins);
        for (Path debugLog : discordSRV.logger().getDebugLogs()) {
            addFile(readFile(80, debugLog, null));
        }
        addFile(config(71, discordSRV.configManager(), null));
        addFile(rawConfig(70, discordSRV.configManager(), null));

        Locale defaultLocale = discordSRV.defaultLocale();
        for (MessagesConfigSingleManager<? extends MessagesConfig> manager : discordSRV.messagesConfigManager().getAllManagers().values()) {
            if (manager.locale() == defaultLocale) {
                addFile(config(61, manager, "parsed_messages.yaml"));
                addFile(rawConfig(60, manager, "messages.yaml"));
            } else {
                addFile(config(51, manager, "parsed_" + manager.locale() + "_messages.yaml"));
                addFile(rawConfig(50, manager, manager.locale() + "_messages.yaml"));
            }
        }

        addFile("connections.json", 40, this::activeLimitedConnectionsConfig);

        addFile("thread-info.txt", -100, this::threadInfo);
        addFile("thread-dumps.txt", -110, this::threadStacks);
    }

    public Paste upload(PasteService service) throws Throwable {
        files.sort(Comparator.comparing(DebugFile.Named::order).reversed());

        ArrayNode files = discordSRV.json().createArrayNode();
        for (DebugFile.Named file : this.files) {
            int length = file.content().length();
            if (length >= BIG_FILE_SPLIT_SIZE) {
                ObjectNode node = discordSRV.json().createObjectNode();
                node.put("name", file.fileName());
                try {
                    Paste paste = service.uploadFile(convertToJson(file).toString().getBytes(StandardCharsets.UTF_8));
                    node.put("url", paste.url());
                    node.put("decryption_key", new String(KEY_ENCODER.encode(paste.decryptionKey()), StandardCharsets.UTF_8));
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
        files.sort(Comparator.comparing(DebugFile.Named::order).reversed());

        Path zipPath = discordSRV.dataDirectory().resolve("debug-" + System.currentTimeMillis() + ".zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            for (DebugFile.Named file : files) {
                zipOutputStream.putNextEntry(new ZipEntry(file.fileName()));

                byte[] data = file.content().getBytes(StandardCharsets.UTF_8);
                zipOutputStream.write(data, 0, data.length);
                zipOutputStream.closeEntry();
            }
        }

        return zipPath;
    }

    private ObjectNode convertToJson(DebugFile.Named file) {
        ObjectNode node = discordSRV.json().createObjectNode();
        node.put("name", file.fileName());
        node.put("content", file.content());
        return node;
    }

    public void addFile(DebugFile.Named debugFile) {
        files.add(debugFile);
    }

    public void addFile(String fileName, int order, CheckedSupplier<DebugFile> fileSupplier) {
        try {
            DebugFile file = fileSupplier.get();
            if (file != null) {
                files.add(file.withName(fileName, order));
            }
        } catch (Throwable t) {
            files.add(exception(order, fileName + (fileName.endsWith(".json") ? ".txt" : ""), t));
        }
    }

    private DebugFile environment() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("discordSRV", discordSRV.getClass().getName());
        values.put("platformVersion", discordSRV.bootstrap().platformVersion());
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

        return new KeyValueDebugFile(values);
    }

    private DebugFile threadInfo() {
        StringBuilder builder = new StringBuilder();

        Scheduler scheduler = discordSRV.scheduler();
        List<ThreadPoolExecutor> executors = new ArrayList<>();

        ExecutorService executorService = scheduler.executorService();
        if (executorService instanceof ThreadPoolExecutor) {
            executors.add((ThreadPoolExecutor) executorService);
        }
        ScheduledExecutorService scheduledExecutorService = scheduler.scheduledExecutorService();
        if (scheduledExecutorService instanceof ScheduledThreadPoolExecutor) {
            executors.add((ScheduledThreadPoolExecutor) scheduledExecutorService);
        }

        for (ThreadPoolExecutor executor : executors) {
            builder.append(executor.getClass().getName()).append(":\n")
                    .append("- Thread Pool: ").append("\n")
                    .append(" * Current: ").append(executor.getPoolSize()).append("\n")
                    .append(" * Largest: ").append(executor.getLargestPoolSize()).append("\n")
                    .append(" * Max: ").append(executor.getMaximumPoolSize()).append("\n")
                    .append("- Queued: ").append(executor.getQueue().size()).append("\n")
                    .append("- Executing: ").append(executor.getActiveCount()).append("\n")
                    .append("- Completed: ").append(executor.getCompletedTaskCount()).append("\n")
                    .append("\n");
        }

        ForkJoinPool forkJoinPool = scheduler.forkJoinPool();
        builder.append(forkJoinPool.getClass().getName()).append(":\n")
                .append("- Thread Pool: ").append("\n")
                .append(" * Active: ").append(forkJoinPool.getActiveThreadCount()).append("\n")
                .append(" * Size: ").append(forkJoinPool.getPoolSize()).append("\n")
                .append(" * Parallelism: ").append(forkJoinPool.getParallelism()).append("\n")
                .append("- Queued Submissions: ").append(forkJoinPool.getQueuedSubmissionCount()).append("\n")
                .append("- Queued Tasks: ").append(forkJoinPool.getQueuedTaskCount()).append("\n")
                .append("- Running: ").append(forkJoinPool.getRunningThreadCount()).append("\n")
                .append("- Stolen: ").append(forkJoinPool.getStealCount()).append("\n")
                .append("\n");

        builder.append("JVM:\n")
                .append("- Active Threads: ").append(Thread.activeCount());

        return new TextDebugFile(builder.toString());
    }

    private DebugFile threadStacks() {
        StringBuilder builder = new StringBuilder(1_000_000);

        Map<Thread, StackTraceElement[]> stacks = Thread.getAllStackTraces();
        builder.append("Thread stack traces for relevant threads (").append(stacks.size()).append(" stack traces including filtered):\n\n");

        for (Map.Entry<Thread, StackTraceElement[]> entry : stacks.entrySet()) {
            Thread thread = entry.getKey();
            String threadName = thread.getName();
            String loweredThreadName = threadName.toLowerCase(Locale.ROOT);
            if (!loweredThreadName.equals("server thread")
                    && !loweredThreadName.contains("jda")
                    && !threadName.startsWith(Scheduler.THREAD_NAME_PREFIX)) {
                continue;
            }

            builder.append(threadName).append(":\n");
            for (StackTraceElement element : entry.getValue()) {
                builder.append("- ").append(element.toString()).append("\n");
            }
            builder.append("\n");
        }
        return new TextDebugFile(builder.toString());
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
            return new TextDebugFile(json);
        } catch (JsonProcessingException e) {
            return exception(order, fileName, e);
        }
    }

    private DebugFile.Named config(int order, ConfigurateConfigManager<?, ?> manager, String overrideFileName) {
        String fileName = overrideFileName != null ? overrideFileName : "parsed_" + manager.fileName();
        try (StringWriter writer = new StringWriter()) {
            AbstractConfigurationLoader<CommentedConfigurationNode> loader = manager
                    .createLoader(manager.filePath(), manager.nodeOptions(true))
                    .sink(() -> new BufferedWriter(writer))
                    .build();
            manager.save(loader);

            return new TextDebugFile(writer.toString()).withName(fileName, order);
        } catch (Exception e) {
            return exception(order - 2, fileName, e);
        }
    }

    private DebugFile.Named rawConfig(int order, ConfigurateConfigManager<?, ?> manager, String overwriteFileName) {
        return readFile(order, manager.filePath(), overwriteFileName);
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
        values.put("storage.remote.pool-options.maximum-lifetime", poolConfig.maximumLifetime);
        values.put("storage.remote.pool-options.maximum-pool-size", poolConfig.maximumPoolSize);
        values.put("storage.remote.pool-options.minimum-pool-size", poolConfig.minimumPoolSize);

        values.put("update.notification-enabled", config.update.notificationEnabled);
        values.put("update.notification-in-game", config.update.notificationInGame);
        values.put("update.first-party-notification", config.update.firstPartyNotification);
        values.put("update.github.enabled", config.update.github.enabled);
        values.put("update.github.api-token_set", StringUtils.isNotBlank(config.update.github.apiToken));
        values.put("update.security.enabled", config.update.security.enabled);
        values.put("update.security.force", config.update.security.force);

        return new KeyValueDebugFile(values, true);
    }

    private DebugFile.Named readFile(int order, Path file, String overwriteFileName) {
        String fileName = overwriteFileName != null ? overwriteFileName : file.getFileName().toString();
        if (!Files.exists(file)) {
            return new TextDebugFile("File does not exist").withName(fileName, order);
        }

        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            return new TextDebugFile(String.join("\n", lines)).withName(fileName, order);
        } catch (IOException e) {
            return exception(order, fileName, e);
        }
    }

    private DebugFile.Named exception(int order, String fileName, Throwable throwable) {
        return new TextDebugFile(ExceptionUtils.getStackTrace(throwable)).withName(fileName, order);
    }
}
