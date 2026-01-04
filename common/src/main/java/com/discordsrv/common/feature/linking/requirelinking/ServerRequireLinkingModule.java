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

package com.discordsrv.common.feature.linking.requirelinking;

import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.lifecycle.DiscordSRVReadyEvent;
import com.discordsrv.api.events.linking.AccountLinkedEvent;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.config.main.linking.ServerRequiredLinkingConfig;
import com.discordsrv.common.feature.linking.LinkingModule;
import com.discordsrv.common.feature.linking.requirelinking.requirement.parser.ParsedRequirements;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class ServerRequireLinkingModule<T extends DiscordSRV> extends RequiredLinkingModule<T> {

    private final List<ParsedRequirements> additionalRequirements = new CopyOnWriteArrayList<>();
    protected final Map<UUID, Component> frozen = new ConcurrentHashMap<>();

    public ServerRequireLinkingModule(T discordSRV) {
        super(discordSRV);
    }

    @Override
    public List<ParsedRequirements> getAllActiveRequirements() {
        return additionalRequirements;
    }

    @Override
    public void reload() {
        synchronized (additionalRequirements) {
            additionalRequirements.clear();
            additionalRequirements.addAll(compile(config().requirements.additionalRequirements));
        }
    }

    @Subscribe
    public void onDiscordSRVReady(DiscordSRVReadyEvent even) {
        // Check players that may have gotten on the server before the module initialized
        for (IPlayer player : discordSRV.playerProvider().allPlayers()) {
            recheck(player);
        }
    }

    @Override
    public abstract ServerRequiredLinkingConfig config();

    public ServerRequiredLinkingConfig.Action action() {
        return config().action;
    }

    @Override
    public boolean isBypassingLinkingByConfig(UUID playerUUID) {
        return config().requirements.bypassUUIDs.contains(playerUUID.toString());
    }

    protected Task<Component> getBlockReason(UUID playerUUID, String playerName, boolean join) {
        List<ParsedRequirements> additionalRequirements;
        synchronized (this.additionalRequirements) {
            additionalRequirements = this.additionalRequirements;
        }

        return getBlockReason(config().requirements, additionalRequirements, playerUUID, playerName, join);
    }

    protected void handleBlock(IPlayer player, Component component) {
        if (component != null) {
            switch (action()) {
                case KICK:
                    player.kick(component);
                    break;
                case SPECTATOR:
                    changeToSpectator(player);
                    // fall through:
                case FREEZE:
                    freeze(player, component);
                    break;
            }
        } else if (action() != ServerRequiredLinkingConfig.Action.KICK) {
            frozen.remove(player.uniqueId());
        }
    }

    //
    // Freeze / Spectator
    //

    protected abstract void changeToSpectator(IPlayer player);
    protected abstract void removeFromSpectator(IPlayer player);

    @Subscribe
    public void onAccountLinked(AccountLinkedEvent event) {
        IPlayer player = discordSRV.playerProvider().player(event.getPlayerUUID());
        if (player == null) {
            return;
        }

        unfreeze(player);
    }

    protected void freeze(IPlayer player, Component blockReason) {
        frozen.put(player.uniqueId(), blockReason);
        player.sendMessage(blockReason);
    }

    protected void unfreeze(IPlayer player) {
        frozen.remove(player.uniqueId());

        if (action() == ServerRequiredLinkingConfig.Action.SPECTATOR) {
            removeFromSpectator(player);
        }
    }

    /**
     * @return callback to call when the player has fully joined
     */
    protected Consumer<IPlayer> handleFreezeLogin(UUID playerUUID, Supplier<Component> getBlockReason) {
        if (discordSRV.isShutdown()) {
            return player -> {};
        } else if (!discordSRV.isReady()) {
            try {
                discordSRV.waitForStatus(DiscordSRV.Status.CONNECTED);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (!config().enabled || action() == ServerRequiredLinkingConfig.Action.KICK) {
            return player -> {};
        }

        Component blockReason = getBlockReason.get();
        if (blockReason == null) {
            return this::unfreeze;
        }

        frozen.put(playerUUID, blockReason);
        return player -> player.sendMessage(blockReason);
    }

    protected void checkCommand(IPlayer player, String command, Supplier<Task<Component>> getBlockReason) {
        if (command.startsWith("/")) command = command.substring(1);
        if (command.equals("discord link") || command.equals("link")) {
            LinkingModule module = discordSRV.getModule(LinkingModule.class);
            if (module == null || module.rateLimit(player.uniqueId())) {
                player.sendMessage(discordSRV.messagesConfig(player).pleaseWaitBeforeRunningThatCommandAgain.minecraft().asComponent());
                return;
            }

            player.sendMessage(discordSRV.messagesConfig(player).checkingLinkStatus.asComponent());

            getBlockReason.get().whenComplete((reason, t) -> {
                if (t != null) {
                    return;
                }

                if (reason == null) {
                    unfreeze(player);
                } else {
                    freeze(player, reason);
                }
            });
        }
    }
}
