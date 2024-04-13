package com.discordsrv.common.sync;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.GroupSyncConfig;
import com.discordsrv.common.config.main.generic.AbstractSyncConfig;
import com.discordsrv.common.future.util.CompletableFutureUtil;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.module.type.AbstractModule;
import com.discordsrv.common.profile.Profile;
import com.discordsrv.common.sync.enums.SyncDirection;
import com.discordsrv.common.sync.enums.SyncResults;
import com.discordsrv.common.sync.enums.SyncSide;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public abstract class AbstractSyncModule<DT extends DiscordSRV, G, D, C extends AbstractSyncConfig<C, G, D>, S> extends AbstractModule<DT> {

    protected final Map<C, Future<?>> syncs = new LinkedHashMap<>();
    private final Map<G, List<C>> configsForGame = new ConcurrentHashMap<>();
    private final Map<D, List<C>> configsForDiscord = new ConcurrentHashMap<>();

    public AbstractSyncModule(DT discordSRV, String loggerName) {
        super(discordSRV, new NamedLogger(discordSRV, loggerName));
    }

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

            for (C config : configs()) {
                if (!config.isSet() || !config.validate(discordSRV)) {
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
                    discordSRV.logger().error("Duplicate " + config.describe());
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

    protected CompletableFuture<Long> lookupLinkedAccount(UUID player) {
        return discordSRV.profileManager().lookupProfile(player)
                .thenApply(Profile::userId)
                .thenApply(userId -> {
                    if (userId == null) {
                        throw new SyncFail(SyncResults.NOT_LINKED);
                    }
                    return userId;
                });
    }

    protected CompletableFuture<UUID> lookupLinkedAccount(long userId) {
        return discordSRV.profileManager().lookupProfile(userId)
                .thenApply(Profile::playerUUID)
                .thenApply(playerUUID -> {
                    if (playerUUID == null) {
                        throw new SyncFail(SyncResults.NOT_LINKED);
                    }
                    return playerUUID;
                });
    }

    protected abstract boolean isTrue(S state);

    protected abstract CompletableFuture<S> getDiscord(C config, long userId);
    protected abstract CompletableFuture<S> getGame(C config, UUID playerUUID);

    protected abstract CompletableFuture<ISyncResult> applyDiscord(C config, long userId, S state);
    protected abstract CompletableFuture<ISyncResult> applyGame(C config, UUID playerUUID, S state);

    protected CompletableFuture<ISyncResult> applyDiscordIfNot(C config, long userId, S state) {
        return getDiscord(config, userId).thenCompose(value -> {
            boolean actualValue;
            if ((actualValue = isTrue(state)) == isTrue(value)) {
                return CompletableFuture.completedFuture(actualValue ? SyncResults.BOTH_TRUE : SyncResults.BOTH_FALSE);
            } else {
                return applyDiscord(config, userId, state);
            }
        });
    }

    protected CompletableFuture<ISyncResult> applyGameIfNot(C config, UUID playerUUID, S state) {
        return getGame(config, playerUUID).thenCompose(value -> {
            boolean actualValue;
            if ((actualValue = isTrue(state)) == isTrue(value)) {
                return CompletableFuture.completedFuture(actualValue ? SyncResults.BOTH_TRUE : SyncResults.BOTH_FALSE);
            } else {
                return applyGame(config, playerUUID, state);
            }
        });
    }

    protected Map<C, CompletableFuture<ISyncResult>> discordChanged(long userId, UUID playerUUID, D discordId, S state) {
        List<C> gameConfigs = configsForDiscord.get(discordId);
        if (gameConfigs == null) {
            return Collections.emptyMap();
        }

        Map<C, CompletableFuture<ISyncResult>> futures = new LinkedHashMap<>();
        for (C config : gameConfigs) {
            SyncDirection direction = config.direction;
            if (direction == SyncDirection.MINECRAFT_TO_DISCORD) {
                // Not going Discord -> Minecraft
                futures.put(config, CompletableFuture.completedFuture(SyncResults.WRONG_DIRECTION));
                continue;
            }

            futures.put(config, applyGameIfNot(config, playerUUID, state));

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

                futures.put(gameConfig, applyDiscordIfNot(gameConfig, userId, state));
            }
        }
        return futures;
    }

    protected Map<C, CompletableFuture<ISyncResult>> gameChanged(long userId, UUID playerUUID, G gameId, S state) {
        List<C> discordConfigs = configsForGame.get(gameId);
        if (discordConfigs == null) {
            return Collections.emptyMap();
        }

        Map<C, CompletableFuture<ISyncResult>> futures = new LinkedHashMap<>();
        for (C config : discordConfigs) {
            SyncDirection direction = config.direction;
            if (direction == SyncDirection.DISCORD_TO_MINECRAFT) {
                // Not going Minecraft -> Discord
                futures.put(config, CompletableFuture.completedFuture(SyncResults.WRONG_DIRECTION));
                continue;
            }

            futures.put(config, applyDiscordIfNot(config, userId, state));

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

                futures.put(gameConfig, applyGameIfNot(gameConfig, playerUUID, state));
            }
        }
        return futures;
    }

    protected abstract void resyncTimer(C config);

    protected CompletableFuture<Map<C, CompletableFuture<ISyncResult>>> resyncAll(UUID playerUUID) {
        return lookupLinkedAccount(playerUUID).thenApply(userId -> resyncAll(playerUUID, userId));
    }

    protected CompletableFuture<Map<C, CompletableFuture<ISyncResult>>> resyncAll(long userId) {
        return lookupLinkedAccount(userId).thenApply(playerUUID -> resyncAll(playerUUID, userId));
    }

    protected Map<C, CompletableFuture<ISyncResult>> resyncAll(UUID playerUUID, long userId) {
        List<C> configs = configs();

        Map<C, CompletableFuture<ISyncResult>> results = new HashMap<>(configs.size());
        for (C config : configs) {
            results.put(config, resync(config, playerUUID, userId));
        }
        return results;
    }

    protected CompletableFuture<ISyncResult> resync(C config, UUID playerUUID, long userId) {
        CompletableFuture<S> gameGet = getGame(config, playerUUID);
        CompletableFuture<S> discordGet = getDiscord(config, userId);

        return CompletableFutureUtil.combine(gameGet, discordGet).thenCompose((__) -> {
            S gameState = gameGet.join();
            S discordState = discordGet.join();

            boolean bothState;
            if ((bothState = (gameState != null)) == (discordState != null)) {
                // Already in sync
                return CompletableFuture.completedFuture((ISyncResult) (bothState ? SyncResults.BOTH_TRUE : SyncResults.BOTH_FALSE));
            }

            SyncSide side = config.tieBreaker;
            SyncDirection direction = config.direction;
            if (discordState != null) {
                if (side == SyncSide.DISCORD) {
                    // Has Discord, add game
                    if (direction == SyncDirection.MINECRAFT_TO_DISCORD) {
                        return CompletableFuture.completedFuture(SyncResults.WRONG_DIRECTION);
                    }

                    return applyGame(config, playerUUID, discordState).thenApply(v -> SyncResults.ADD_GAME);
                } else {
                    // Missing game, remove Discord
                    if (direction == SyncDirection.DISCORD_TO_MINECRAFT) {
                        return CompletableFuture.completedFuture(SyncResults.WRONG_DIRECTION);
                    }

                    return applyDiscord(config, userId, null).thenApply(v -> SyncResults.REMOVE_DISCORD);
                }
            } else {
                if (side == SyncSide.DISCORD) {
                    // Missing Discord, remove game
                    if (direction == SyncDirection.MINECRAFT_TO_DISCORD) {
                        return CompletableFuture.completedFuture(SyncResults.WRONG_DIRECTION);
                    }

                    return applyGame(config, playerUUID, null).thenApply(v -> SyncResults.REMOVE_GAME);
                } else {
                    // Has game, add Discord
                    if (direction == SyncDirection.DISCORD_TO_MINECRAFT) {
                        return CompletableFuture.completedFuture(SyncResults.WRONG_DIRECTION);
                    }

                    return applyDiscord(config, userId, gameState).thenApply(v -> SyncResults.ADD_DISCORD);
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


}
