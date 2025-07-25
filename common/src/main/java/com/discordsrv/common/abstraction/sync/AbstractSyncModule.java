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

package com.discordsrv.common.abstraction.sync;

import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.linking.AccountLinkedEvent;
import com.discordsrv.api.events.linking.AccountUnlinkedEvent;
import com.discordsrv.api.reload.ReloadResult;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.abstraction.sync.cause.GenericSyncCauses;
import com.discordsrv.common.abstraction.sync.cause.ISyncCause;
import com.discordsrv.common.abstraction.sync.enums.SyncDirection;
import com.discordsrv.common.abstraction.sync.enums.SyncSide;
import com.discordsrv.common.abstraction.sync.result.GenericSyncResults;
import com.discordsrv.common.abstraction.sync.result.ISyncResult;
import com.discordsrv.common.config.main.generic.AbstractSyncConfig;
import com.discordsrv.common.config.main.generic.SyncConfig;
import com.discordsrv.common.core.logging.NamedLogger;
import com.discordsrv.common.core.module.type.AbstractModule;
import com.discordsrv.common.events.player.PlayerConnectedEvent;
import com.discordsrv.common.helper.Someone;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

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

    protected final Set<C> syncs = new LinkedHashSet<>();
    protected final List<Future<?>> timers = new ArrayList<>();
    protected final Map<G, List<C>> configsForGame = new ConcurrentHashMap<>();
    protected final Map<D, List<C>> configsForDiscord = new ConcurrentHashMap<>();

    public AbstractSyncModule(DT discordSRV, String loggerName) {
        super(discordSRV, new NamedLogger(discordSRV, loggerName));
    }

    protected abstract String syncName();

    @Nullable
    protected abstract String logFileName();

    protected abstract String gameTerm();
    protected abstract String discordTerm();

    /**
     * Returns a list of all in use synchronizables.
     * @return a list of configurations for synchronizables
     */
    protected abstract List<C> configs();

    @Override
    public boolean isEnabled() {
        boolean any = false;
        for (C config : configs()) {
            if (config.isSet()) {
                any = true;
                break;
            }
        }
        if (!any) {
            return false;
        }

        return super.isEnabled();
    }

    @Override
    public void reload(Consumer<ReloadResult> resultConsumer) {
        synchronized (syncs) {
            timers.forEach(future -> future.cancel(false));
            timers.clear();
            syncs.clear();
            configsForGame.clear();
            configsForDiscord.clear();

            Map<G, C> oneWayToGame = new HashMap<>();
            Map<D, C> oneWayToDiscord = new HashMap<>();
            Map<Integer, Set<C>> timerIntervals = new HashMap<>();
            for (C config : configs()) {
                if (!config.isSet() || !config.validate(syncName(), discordSRV)) {
                    continue;
                }

                boolean duplicateFound = false;
                for (C existingConfig : syncs) {
                    if (existingConfig.isSameAs(config)) {
                        duplicateFound = true;
                        break;
                    }
                }
                if (duplicateFound) {
                    discordSRV.logger().error("Duplicate " + syncName() + " (" + config.describe() + ")");
                    continue;
                }

                G gameId = config.gameId();
                D discordId = config.discordId();

                // This implementation does not support multiple one-way synchronizations to the same entity
                // For example A->X, B->X: as there is no logic to deal with multiple states on the left side
                SyncDirection direction = config.direction;
                if (direction == SyncDirection.DISCORD_TO_MINECRAFT && gameId != null) {
                    C conflict = oneWayToGame.get(gameId);
                    if (conflict != null) {
                        discordSRV.logger().error(
                                "Conflicting one-way " + syncName()
                                        + " to the same Minecraft " + gameTerm()
                                        + ": " + conflict + " and " + config);
                        continue;
                    }
                    oneWayToGame.put(gameId, config);
                } else if (direction == SyncDirection.MINECRAFT_TO_DISCORD && discordId != null) {
                    C conflict = oneWayToDiscord.get(discordId);
                    if (conflict != null) {
                        discordSRV.logger().error(
                                "Conflicting one-way " + syncName()
                                        + " to the same Discord " + discordTerm()
                                        + ": " + conflict + " and " + config);
                        continue;
                    }
                    oneWayToDiscord.put(discordId, config);
                }

                AbstractSyncConfig.TimerConfig timer = config.timer;
                if (timer != null && timer.side != SyncSide.DISABLED) {
                    timerIntervals.computeIfAbsent(timer.cycleTime, key -> new LinkedHashSet<>()).add(config);
                }

                syncs.add(config);
                if (gameId != null) {
                    configsForGame.computeIfAbsent(gameId, key -> new ArrayList<>()).add(config);
                }
                if (discordId != null) {
                    configsForDiscord.computeIfAbsent(discordId, key -> new ArrayList<>()).add(config);
                }
            }

            for (Map.Entry<Integer, Set<C>> entry : timerIntervals.entrySet()) {
                int cycleTime = entry.getKey();
                Future<?> future = discordSRV.scheduler().runAtFixedRate(
                        () -> resyncTimer(entry.getValue()),
                        Duration.ofMinutes(cycleTime),
                        Duration.ofMinutes(cycleTime)
                );
                timers.add(future);
            }
        }
    }

    private void resyncTimer(Set<C> configs) {
        for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
            resync(GenericSyncCauses.TIMER, Someone.of(discordSRV, player.uniqueId()), config -> config.timer.side, configs);
        }
    }

    @Subscribe
    public void onPlayerConnected(PlayerConnectedEvent event) {
        resyncAll(GenericSyncCauses.GAME_JOIN, Someone.of(discordSRV, event.player()), config -> config.tieBreakers.join);
    }

    @Subscribe
    public void onAccountLinked(AccountLinkedEvent event) {
        resyncAll(GenericSyncCauses.LINK, Someone.of(discordSRV, event.getPlayerUUID()), config -> config.tieBreakers.link);
    }

    @Subscribe
    public void onAccountUnlinked(AccountUnlinkedEvent event) {
        Someone.Resolved someone = Someone.of(discordSRV, event.getPlayerUUID(), event.getUserId());
        SyncSummary<C> summary = new SyncSummary<>(this, GenericSyncCauses.UNLINK, someone);
        for (C config : configs()) {
            SyncConfig.UnlinkBehaviour unlinkBehaviour = config.unlinkBehaviour;
            if (unlinkBehaviour.isGame()) {
                summary.appendResult(config, removeGame(config, someone));
            }
            if (unlinkBehaviour.isDiscord()) {
                summary.appendResult(config, removeDiscord(config, someone));
            }
        }
        logSummary(summary);
    }

    /**
     * Checks if the given new and current state are the same, basically meaning that no update is necessary.
     * @return the result stating the states are the same, otherwise {@code null} to state they are not
     */
    @Nullable
    protected abstract ISyncResult doesStateMatch(S one, S two);

    public abstract S getRemovedState();

    /**
     * Gets the current state of the provided config for the specified user on Discord.
     *
     * @param config the configuration for the synchronizable
     * @param someone the linked account
     * @return a future for the state on Discord
     */
    protected abstract Task<S> getDiscord(C config, Someone.Resolved someone);

    /**
     * Gets the current state of the provided config for the specified player on Minecraft.
     *
     * @param config  the configuration for the synchronizable
     * @param someone the Minecraft player {@link UUID}
     * @return a future for the state on Minecraft
     */
    protected abstract Task<S> getGame(C config, Someone.Resolved someone);

    /**
     * Applies the provided state for the provided config for the provided Discord user.
     *
     * @param config   the configuration for the synchronizable
     * @param someone  the Discord user id
     * @param newState the newState to apply
     * @return a future with the result of the synchronization
     */
    protected abstract Task<ISyncResult> applyDiscord(C config, Someone.Resolved someone, @Nullable S newState);

    protected Task<ISyncResult> applyDiscordIfDoesNotMatch(C config, Someone.Resolved someone, @Nullable S newState) {
        return getDiscord(config, someone).then(currentState -> {
            ISyncResult result = doesStateMatch(newState, currentState);
            if (result != null) {
                return Task.completed(result);
            } else {
                return applyDiscord(config, someone, newState);
            }
        });
    }

    protected Task<ISyncResult> removeDiscord(C config, Someone.Resolved someone) {
        return applyDiscordIfDoesNotMatch(config, someone, getRemovedState());
    }

    /**
     * Applies the provided state for the provided config for the provided Minecraft player.
     *
     * @param config   the configuration for the synchronizable
     * @param someone  the Minecraft player {@link UUID}
     * @param newState the newState to apply
     * @return a future with the result of the synchronization
     */
    protected abstract Task<ISyncResult> applyGame(C config, Someone.Resolved someone, @Nullable S newState);

    protected Task<ISyncResult> applyGameIfDoesNotMatch(C config, Someone.Resolved someone, @Nullable S newState) {
        return getGame(config, someone).then(currentState -> {
            ISyncResult result = doesStateMatch(currentState, newState);
            if (result != null) {
                return Task.completed(result);
            } else {
                return applyGame(config, someone, newState);
            }
        });
    }

    protected Task<ISyncResult> removeGame(C config, Someone.Resolved someone) {
        return applyGameIfDoesNotMatch(config, someone, getRemovedState());
    }

    protected boolean isApplicableForProactiveSync(C config) {
        return true;
    }

    protected Task<SyncSummary<C>> discordChanged(ISyncCause cause, Someone someone, D discordId, @Nullable S newState) {
        List<C> gameConfigs = configsForDiscord.get(discordId);
        if (gameConfigs == null) {
            return Task.completed(null);
        }

        return someone.resolve().thenApply(resolved -> {
            if (resolved == null) {
                return new SyncSummary<>(this, cause, someone).fail(GenericSyncResults.NOT_LINKED);
            }

            SyncSummary<C> summary = new SyncSummary<>(this, cause, resolved);
            for (C config : gameConfigs) {
                if (!isApplicableForProactiveSync(config)) {
                    continue;
                }

                SyncDirection direction = config.direction;
                if (direction == SyncDirection.MINECRAFT_TO_DISCORD) {
                    // Not going Discord -> Minecraft
                    summary.appendResult(config, GenericSyncResults.WRONG_DIRECTION);
                    continue;
                }

                summary.appendResult(config, applyGameIfDoesNotMatch(config, resolved, newState));

                // If the sync is bidirectional, also sync anything else linked to the same Minecraft id
                if (direction == SyncDirection.DISCORD_TO_MINECRAFT) {
                    continue;
                }

                List<C> discordConfigs = configsForGame.get(config.gameId());
                if (discordConfigs == null) {
                    continue;
                }

                for (C gameConfig : discordConfigs) {
                    if (Objects.equals(gameConfig.discordId(), discordId)) {
                        continue;
                    }

                    summary.appendResult(gameConfig, applyDiscordIfDoesNotMatch(gameConfig, resolved, newState));
                }
            }
            return summary;
        }).whenComplete((summary, t) -> {
            if (summary != null) {
                logSummary(summary);
            }
        });
    }

    protected Task<SyncSummary<C>> gameChanged(ISyncCause cause, Someone someone, @NotNull G gameId, @Nullable S newState) {
        List<C> discordConfigs = configsForGame.get(gameId);
        if (discordConfigs == null) {
            return Task.completed(null);
        }

        return someone.resolve().thenApply(resolved -> {
            if (resolved == null) {
                return new SyncSummary<>(this, cause, someone).fail(GenericSyncResults.NOT_LINKED);
            }

            SyncSummary<C> summary = new SyncSummary<>(this, cause, resolved);
            for (C config : discordConfigs) {
                if (!isApplicableForProactiveSync(config)) {
                    continue;
                }

                SyncDirection direction = config.direction;
                if (direction == SyncDirection.DISCORD_TO_MINECRAFT) {
                    // Not going Minecraft -> Discord
                    summary.appendResult(config, GenericSyncResults.WRONG_DIRECTION);
                    continue;
                }

                summary.appendResult(config, applyDiscordIfDoesNotMatch(config, resolved, newState));

                // If the sync is bidirectional, also sync anything else linked to the same Discord id
                if (direction == SyncDirection.MINECRAFT_TO_DISCORD) {
                    continue;
                }

                List<C> gameConfigs = configsForDiscord.get(config.discordId());
                if (gameConfigs == null) {
                    continue;
                }

                for (C gameConfig : gameConfigs) {
                    if (Objects.equals(gameConfig.gameId(), gameId)) {
                        continue;
                    }

                    summary.appendResult(gameConfig, applyGameIfDoesNotMatch(gameConfig, resolved, newState));
                }
            }
            return summary;
        }).whenComplete((summary, t) -> {
            if (summary != null) {
                logSummary(summary);
            }
        });
    }

    public boolean disabledOnAllConfigs(Function<C, SyncSide> sideDecider) {
        for (C config : configs()) {
            SyncSide side = sideDecider.apply(config);
            if (side != SyncSide.DISABLED) {
                return false;
            }
        }
        return true;
    }

    public Task<SyncSummary<C>> resyncAll(ISyncCause cause, Someone someone, Function<C, SyncSide> sideDecider) {
        return resync(cause, someone, sideDecider, syncs);
    }

    public Task<SyncSummary<C>> resync(ISyncCause cause, Someone someone, Function<C, SyncSide> sideDecider, Set<C> configs) {
        if (disabledOnAllConfigs(sideDecider)) {
            return Task.completed(new SyncSummary<>(this, cause, someone).fail(GenericSyncResults.SIDE_DISABLED));
        }

        return someone.resolve().thenApply(resolved -> {
            if (resolved == null) {
                return new SyncSummary<>(this, cause, someone).fail(GenericSyncResults.NOT_LINKED);
            }

            SyncSummary<C> summary = new SyncSummary<>(this, cause, resolved);
            for (C config : configs) {
                SyncSide side = sideDecider.apply(config);
                if (side == SyncSide.DISABLED) {
                    continue;
                }

                summary.appendResult(config, resync(config, resolved, side));
            }
            return summary;
        }).whenComplete((summary, t) -> {
            if (summary != null) {
                logSummary(summary);
            }
        });
    }

    private Task<ISyncResult> resync(C config, Someone.Resolved resolved, SyncSide side) {
        Task<S> gameGet = getGame(config, resolved);
        Task<S> discordGet = getDiscord(config, resolved);

        return Task.allOf(gameGet, discordGet).then((__) -> {
            S gameState = gameGet.join();
            S discordState = discordGet.join();

            logger().trace(resolved.playerUUID() + " (" + gameState + ") | "
                                   + Long.toUnsignedString(resolved.userId()) + " (" + discordState + ")");

            ISyncResult alreadyInSyncResult = doesStateMatch(gameState, discordState);
            if (alreadyInSyncResult != null) {
                return Task.completed(alreadyInSyncResult);
            }

            SyncDirection direction = config.direction;
            if (side == SyncSide.DISCORD) {
                if (direction == SyncDirection.MINECRAFT_TO_DISCORD) {
                    return Task.completed(GenericSyncResults.WRONG_DIRECTION);
                }

                return applyGame(config, resolved, discordState);
            } else {
                if (direction == SyncDirection.DISCORD_TO_MINECRAFT) {
                    return Task.completed(GenericSyncResults.WRONG_DIRECTION);
                }

                return applyDiscord(config, resolved, gameState);
            }
        }).mapException(SyncFail.class, SyncFail::getResult);
    }

    private String formatResults(SyncSummary<C> summary, List<String> results) {
        int count = results.size();
        return summary.who() + " (sync cause: " + summary.cause() + ")"
                + (count == 1 ? ": " : "\n")
                + String.join("\n", results);
    }

    private void logSummary(SyncSummary<C> summary) {
        summary.resultFuture().whenComplete((results, t) -> {
            Throwable throwableToLog = null;
            if (t != null) {
                if (t instanceof SyncFail) {
                    SyncFail fail = (SyncFail) t;
                    summary.fail(fail.getResult());
                    throwableToLog = fail.getCause();
                } else {
                    logger().error("Failed to " + syncName() + " " + summary.who() + " (sync cause: " + summary.cause() + ")", t);
                    return;
                }
            }

            ISyncResult allFailReason = summary.allFailReason();
            if (allFailReason != null) {
                String reason = allFailReason.format(gameTerm(), discordTerm());
                String message = "Failed to " + syncName() + " " + summary.who() + " (sync cause: " + summary.cause() + "): " + reason;
                if (allFailReason.isError()) {
                    logger().error(message, throwableToLog);
                } else {
                    logger().debug(message, throwableToLog);
                }
                return;
            }

            Map<ISyncResult, List<String>> groupedResults = new LinkedHashMap<>();
            for (Map.Entry<C, ISyncResult> entry : results.entrySet()) {
                C config = entry.getKey();
                ISyncResult result = entry.getValue();

                groupedResults.computeIfAbsent(result, key -> new ArrayList<>()).add(config.describe());
            }

            List<String> allResults = new ArrayList<>();
            List<String> auditResults = new ArrayList<>();
            for (Map.Entry<ISyncResult, List<String>> entry : groupedResults.entrySet()) {
                ISyncResult result = entry.getKey();
                String line = result.format(gameTerm(), discordTerm())
                        + ": [" + String.join(", ", entry.getValue()) + "]";
                allResults.add(line);
                if (result.isUpdate()) {
                    auditResults.add(line);
                }
            }

            logger().debug(syncName() + " performed for " + formatResults(summary, allResults));

            String logFileName = logFileName();
            if (logFileName != null && !auditResults.isEmpty()) {
                discordSRV.logger().writeLogForCurrentDay(logFileName, formatResults(summary, auditResults));
            }
        });
    }

}
