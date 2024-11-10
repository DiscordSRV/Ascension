package com.discordsrv.common.helper;

import com.discordsrv.common.DiscordSRV;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Data that may or may not actually ever persist.
 * @see Model
 * @see #get()
 * @see #save()
 */
public class TemporaryLocalData {

    private final DiscordSRV discordSRV;
    private final Path file;
    private Model model;
    private Future<?> saveFuture;

    public TemporaryLocalData(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.file = discordSRV.dataDirectory().resolve(".temporary-local-data.json");
    }

    @Blocking
    public Model get() {
        if (model != null) {
            return model;
        }

        synchronized (this) {
            return model = resolve();
        }
    }

    @NonBlocking
    public void saveLater() {
        synchronized (this) {
            if (saveFuture != null && !saveFuture.isDone()) {
                return;
            }
            saveFuture = discordSRV.scheduler().runLater(this::save, Duration.ofSeconds(30));
        }
    }

    @Blocking
    public void save() {
        synchronized (this) {
            try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(file))) {
                discordSRV.json().writeValue(outputStream, model);
            } catch (IOException e) {
                discordSRV.logger().error("Failed to save temporary local data", e);
            }
        }
    }

    @Blocking
    private Model resolve() {
        synchronized (this) {
            if (model != null) {
                return model;
            }

            if (!Files.exists(file)) {
                return new Model();
            }

            try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(file))) {
                return discordSRV.json().readValue(inputStream, Model.class);
            } catch (IOException e) {
                discordSRV.logger().error("Failed to load temporary local data, resetting", e);
                return new Model();
            }
        }
    }

    /**
     * Saved/loaded via {@link DiscordSRV#json()} ({@link com.fasterxml.jackson.databind.ObjectMapper}).
     */
    public static class Model {

        /**
         * {@link com.discordsrv.common.feature.console.SingleConsoleHandler} thread rotation.
         */
        public Map<String, List<Long>> consoleThreadRotationIds;


    }
}
