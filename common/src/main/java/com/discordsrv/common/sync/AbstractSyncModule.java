package com.discordsrv.common.sync;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.GroupSyncConfig;
import com.discordsrv.common.config.main.generic.AbstractSyncConfig;
import com.discordsrv.common.event.events.player.PlayerConnectedEvent;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.AbstractModule;
import com.discordsrv.common.player.IPlayer;
import com.discordsrv.common.someone.Someone;
import com.discordsrv.common.sync.cause.GenericSyncCauses;
import com.discordsrv.common.sync.cause.ISyncCause;
import com.discordsrv.common.sync.enums.SyncDirection;
import com.discordsrv.common.sync.result.GenericSyncResults;
import com.discordsrv.common.sync.enums.SyncSide;
import com.discordsrv.common.sync.result.ISyncResult;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * Abstraction for synchronization between Minecraft and Discord.
 *
 * @param <DT> the DiscordSRV type
 * @param <C> the configuration type for a single synchronizable
 * @param <G> the identifier for the game object to be synced
 * @param <D> the identifier for the Discord object to be synced
 * @param <S> state of synchronization on Minecraft/Discord
 */
public abstract class AbstractSyncModule<
        DT extends DiscordSRV,
        C extends AbstractSyncConfig<C, G, D>,
        G,
        D,
        S
> extends AbstractModule<DT> {

    protected final Map<C, Future<?>> syncs = new LinkedHashMap<>();
    private final Map<G, List<C>> configsForGame = new ConcurrentHashMap<>();
    private final Map<D, List<C>> configsForDiscord = new ConcurrentHashMap<>();

    public AbstractSyncModule(DT discordSRV, String loggerName) {
        super(discordSRV, new NamedLogger(discordSRV, loggerName));
    }

    public abstract String syncName();
    public abstract String logName();

    public abstract String gameTerm();
    public abstract String discordTerm();

    /**
     * Returns a list of all in use synchronizables.
     * @return a list of configurations for synchronizables
     */
    public abstract List<C> configs();

    @Override
    public void reload(Consumer<DiscordSRVApi.ReloadResult> resultConsumer) {
        synchronized (syncs) {
            syncs.values().forEach(future -> {
                if (future != null) {
                    future.cancel(false);
                }
            });
            syncs.clear();
            configsForGame.clear();
            configsForDiscord.clear();

            String syncName = syncName();
            for (C config : configs()) {
                if (!config.isSet() || !config.validate(syncName, discordSRV)) {
                    continue;
                }

                boolean failed = false;
                for (C existingConfig : syncs.keySet()) {
                    if (existingConfig.isSameAs(config)) {
                        failed = true;
                        break;
                    }
                }
                if (failed) {
                    discordSRV.logger().error("Duplicate " + syncName + " (" + config.describe() + ")");
                    continue;
                }

                Future<?> future = null;
                GroupSyncConfig.PairConfig.TimerConfig timer = config.timer;
                if (timer != null && timer.enabled) {
                    int cycleTime = timer.cycleTime;
                    future = discordSRV.scheduler().runAtFixedRate(
                            () -> resyncTimer(config),
                            Duration.ofMinutes(cycleTime),
                            Duration.ofMinutes(cycleTime)
                    );
                }

                syncs.put(config, future);

                G game = config.gameId();
                if (game != null) {
                    configsForGame.computeIfAbsent(game, key -> new ArrayList<>()).add(config);
                }

                D discord = config.discordId();
                if (discord != null) {
                    configsForDiscord.computeIfAbsent(discord, key -> new ArrayList<>()).add(config);
                }
            }
        }
    }

    private void resyncTimer(C config) {
        for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
            resync(GenericSyncCauses.TIMER, config, Someone.of(player.uniqueId()));
        }
    }

    @Subscribe
    public void onPlayerConnected(PlayerConnectedEvent event) {
        resyncAll(GenericSyncCauses.GAME_JOIN, Someone.of(event.player()));
    }

    /**
     * Check if the provided state is active or inactive, should this not match for the state of the two sides, synchronization will occur.
     *
     * @param state the state
     * @return {@code true} indicating the provided state is "active"
     */
    protected abstract boolean isActive(S state);

    /**
     * Gets the current state of the provided config for the specified user on Discord.
     *
     * @param config the configuration for the synchronizable
     * @param userId the Discord user id
     * @return a future for the state on Discord
     */
    protected abstract CompletableFuture<S> getDiscord(C config, long userId);

    /**
     * Gets the current state of the provided config for the specified player on Minecraft.
     *
     * @param config the configuration for the synchronizable
     * @param playerUUID the Minecraft player {@link UUID}
     * @return a future for the state on Minecraft
     */
    protected abstract CompletableFuture<S> getGame(C config, UUID playerUUID);

    /**
     * Applies the provided state for the provided config for the provided Discord user.
     *
     * @param config the configuration for the synchronizable
     * @param userId the Discord user id
     * @param state the state to apply
     * @return a future with the result of the synchronization
     */
    protected abstract CompletableFuture<ISyncResult> applyDiscord(C config, long userId, S state);

    protected CompletableFuture<ISyncResult> applyDiscordIfNot(C config, long userId, S state) {
        return getDiscord(config, userId).thenCompose(value -> {
            boolean actualValue;
            if ((actualValue = isActive(state)) == isActive(value)) {
                return CompletableFuture.completedFuture(actualValue ? GenericSyncResults.BOTH_TRUE : GenericSyncResults.BOTH_FALSE);
            } else {
                return applyDiscord(config, userId, state);
            }
        });
    }

    /**
     * Applies the provided state for the provided config for the provided Minecraft player.
     *
     * @param config the configuration for the synchronizable
     * @param playerUUID the Minecraft player {@link UUID}
     * @param state the state to apply
     * @return a future with the result of the synchronization
     */
    protected abstract CompletableFuture<ISyncResult> applyGame(C config, UUID playerUUID, S state);

    protected CompletableFuture<ISyncResult> applyGameIfNot(C config, UUID playerUUID, S state) {
        return getGame(config, playerUUID).thenCompose(value -> {
            boolean active;
            if ((active = isActive(state)) == isActive(value)) {
                return CompletableFuture.completedFuture(active ? GenericSyncResults.BOTH_TRUE : GenericSyncResults.BOTH_FALSE);
            } else {
                return applyGame(config, playerUUID, state);
            }
        });
    }

    protected CompletableFuture<SyncSummary<C>> discordChanged(ISyncCause cause, Someone someone, D discordId, S state) {
        List<C> gameConfigs = configsForDiscord.get(discordId);
        if (gameConfigs == null) {
            return CompletableFuture.completedFuture(null);
        }

        return someone.withLinkedAccounts(discordSRV).thenApply(resolved -> {
            if (resolved == null) {
                return new SyncSummary<C>(cause, someone).fail(GenericSyncResults.NOT_LINKED);
            }

            SyncSummary<C> summary = new SyncSummary<>(cause, resolved);
            for (C config : gameConfigs) {
                SyncDirection direction = config.direction;
                if (direction == SyncDirection.MINECRAFT_TO_DISCORD) {
                    // Not going Discord -> Minecraft
                    summary.appendResult(config, GenericSyncResults.WRONG_DIRECTION);
                    continue;
                }

                summary.appendResult(config, applyGameIfNot(config, resolved.playerUUID(), state));

                // If the sync is bidirectional, also sync anything else linked to the same Minecraft id
                if (direction == SyncDirection.DISCORD_TO_MINECRAFT) {
                    continue;
                }

                List<C> discordConfigs = configsForGame.get(config.gameId());
                if (discordConfigs == null) {
                    continue;
                }

                for (C gameConfig : discordConfigs) {
                    if (gameConfig.discordId() == discordId) {
                        continue;
                    }

                    summary.appendResult(gameConfig, applyDiscordIfNot(gameConfig, resolved.userId(), state));
                }
            }
            return summary;
        }).whenComplete((summary, t) -> {
            if (summary != null) {
                logSummary(summary);
            }
        });
    }

    protected CompletableFuture<SyncSummary<C>> gameChanged(ISyncCause cause, Someone someone, G gameId, S state) {
        List<C> discordConfigs = configsForGame.get(gameId);
        if (discordConfigs == null) {
            return CompletableFuture.completedFuture(null);
        }

        return someone.withLinkedAccounts(discordSRV).thenApply(resolved -> {
            if (resolved == null) {
                return new SyncSummary<C>(cause, someone).fail(GenericSyncResults.NOT_LINKED);
            }

            SyncSummary<C> summary = new SyncSummary<>(cause, resolved);
            for (C config : discordConfigs) {
                SyncDirection direction = config.direction;
                if (direction == SyncDirection.DISCORD_TO_MINECRAFT) {
                    // Not going Minecraft -> Discord
                    summary.appendResult(config, GenericSyncResults.WRONG_DIRECTION);
                    continue;
                }

                summary.appendResult(config, applyDiscordIfNot(config, resolved.userId(), state));

                // If the sync is bidirectional, also sync anything else linked to the same Discord id
                if (direction == SyncDirection.MINECRAFT_TO_DISCORD) {
                    continue;
                }

                List<C> gameConfigs = configsForDiscord.get(config.discordId());
                if (gameConfigs == null) {
                    continue;
                }

                for (C gameConfig : gameConfigs) {
                    if (gameConfig.gameId() == gameId) {
                        continue;
                    }

                    summary.appendResult(gameConfig, applyGameIfNot(gameConfig, resolved.playerUUID(), state));
                }
            }
            return summary;
        }).whenComplete((summary, t) -> {
            if (summary != null) {
                logSummary(summary);
            }
        });
    }

    public CompletableFuture<SyncSummary<C>> resyncAll(ISyncCause cause, Someone someone) {
        return someone.withLinkedAccounts(discordSRV).thenApply(resolved -> {
            if (resolved == null) {
                return new SyncSummary<C>(cause, someone).fail(GenericSyncResults.NOT_LINKED);
            }

            SyncSummary<C> summary = new SyncSummary<>(cause, resolved);
            List<C> configs = configs();

            for (C config : configs) {
                summary.appendResult(config, resync(config, resolved));
            }
            return summary;
        }).whenComplete((summary, t) -> {
            if (summary != null) {
                logSummary(summary);
            }
        });
    }

    protected CompletableFuture<SyncSummary<C>> resync(ISyncCause cause, C config, Someone someone) {
        return someone.withLinkedAccounts(discordSRV).thenApply(resolved -> {
            if (resolved == null) {
                return new SyncSummary<C>(cause, someone).fail(GenericSyncResults.NOT_LINKED);
            }

            return new SyncSummary<C>(cause, resolved)
                    .appendResult(config, resync(config, resolved));
        }).whenComplete((summary, t) -> {
            if (summary != null) {
                logSummary(summary);
            }
        });
    }

    private CompletableFuture<ISyncResult> resync(C config, Someone.Resolved resolved) {
        UUID playerUUID = resolved.playerUUID();
        long userId = resolved.userId();

        CompletableFuture<S> gameGet = getGame(config, playerUUID);
        CompletableFuture<S> discordGet = getDiscord(config, userId);

        return CompletableFutureUtil.combine(gameGet, discordGet).thenCompose((__) -> {
            S gameState = gameGet.join();
            S discordState = discordGet.join();

            boolean bothState;
            if ((bothState = (gameState != null)) == (discordState != null)) {
                // Already in sync
                return CompletableFuture.completedFuture((ISyncResult) (bothState ? GenericSyncResults.BOTH_TRUE : GenericSyncResults.BOTH_FALSE));
            }

            SyncSide side = config.tieBreaker;
            SyncDirection direction = config.direction;
            if (discordState != null) {
                if (side == SyncSide.DISCORD) {
                    // Has Discord, add game
                    if (direction == SyncDirection.MINECRAFT_TO_DISCORD) {
                        return CompletableFuture.completedFuture(GenericSyncResults.WRONG_DIRECTION);
                    }

                    return applyGame(config, playerUUID, discordState).thenApply(v -> GenericSyncResults.ADD_GAME);
                } else {
                    // Missing game, remove Discord
                    if (direction == SyncDirection.DISCORD_TO_MINECRAFT) {
                        return CompletableFuture.completedFuture(GenericSyncResults.WRONG_DIRECTION);
                    }

                    return applyDiscord(config, userId, null).thenApply(v -> GenericSyncResults.REMOVE_DISCORD);
                }
            } else {
                if (side == SyncSide.DISCORD) {
                    // Missing Discord, remove game
                    if (direction == SyncDirection.MINECRAFT_TO_DISCORD) {
                        return CompletableFuture.completedFuture(GenericSyncResults.WRONG_DIRECTION);
                    }

                    return applyGame(config, playerUUID, null).thenApply(v -> GenericSyncResults.REMOVE_GAME);
                } else {
                    // Has game, add Discord
                    if (direction == SyncDirection.DISCORD_TO_MINECRAFT) {
                        return CompletableFuture.completedFuture(GenericSyncResults.WRONG_DIRECTION);
                    }

                    return applyDiscord(config, userId, gameState).thenApply(v -> GenericSyncResults.ADD_DISCORD);
                }
            }
        }).exceptionally(t -> {
            if (t instanceof SyncFail) {
                return ((SyncFail) t).getResult();
            } else {
                throw (RuntimeException) t;
            }
        });
    }

    private String formatResults(SyncSummary<C> summary, List<String> results) {
        int count = results.size();
        return summary.who().toString()
                + (count == 1 ? ": " : "\n")
                + String.join("\n", results);
    }

    private void logSummary(SyncSummary<C> summary) {
        summary.resultFuture().whenComplete((results, t) -> {
            if (t != null) {
                logger().error("Failed to " + syncName() + " " + summary.who(), t);
                return;
            }

            ISyncResult allFailReason = summary.allFailReason();
            if (allFailReason != null) {
                String reason = allFailReason.format(gameTerm(), discordTerm());
                logger().debug("Failed to " + syncName() + " " + summary.who() + ": " + reason);
                return;
            }

            List<String> logResults = new ArrayList<>();
            List<String> auditResults = new ArrayList<>();
            for (Map.Entry<C, ISyncResult> entry : results.entrySet()) {
                C config = entry.getKey();
                ISyncResult result = entry.getValue();

                String log = config.describe();
                if (StringUtils.isEmpty(log)) {
                    log += ": ";
                }
                log += result.format(gameTerm(), discordTerm());

                logResults.add(log);
                if (result.isSuccess()) {
                    auditResults.add(log);
                }
            }

            logger().debug(formatResults(summary, logResults));
            discordSRV.logger().writeLogForCurrentDay(logName(), formatResults(summary, auditResults));
        });
    }

}
