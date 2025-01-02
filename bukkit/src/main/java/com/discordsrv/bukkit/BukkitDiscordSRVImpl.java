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

package com.discordsrv.bukkit;

import com.discordsrv.bukkit.command.game.BukkitGameCommandExecutionHelper;
import com.discordsrv.bukkit.command.game.PaperGameCommandExecutionHelper;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import com.discordsrv.bukkit.console.BukkitConsole;
import com.discordsrv.bukkit.listener.*;
import com.discordsrv.bukkit.player.BukkitPlayerImpl;
import com.discordsrv.bukkit.player.BukkitPlayerProvider;
import com.discordsrv.bukkit.requiredlinking.BukkitRequiredLinkingModule;
import com.discordsrv.bukkit.scheduler.BukkitScheduler;
import com.discordsrv.bukkit.scheduler.FoliaScheduler;
import com.discordsrv.common.command.game.abstraction.GameCommandExecutionHelper;
import com.discordsrv.common.feature.messageforwarding.game.MinecraftToDiscordChatModule;
import com.discordsrv.common.util.ReflectionUtil;
import org.bukkit.command.CommandMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BukkitDiscordSRVImpl extends BukkitDiscordSRV {

    private final BukkitScheduler scheduler;
    private final GameCommandExecutionHelper executionHelper;
    private final BukkitPlayerProvider playerProvider;
    private final BukkitConsole console;

    public BukkitDiscordSRVImpl(IBukkitBootstrap bootstrap) {
        super(bootstrap);

        this.scheduler = ReflectionUtil.classExists("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler")
                         ? new FoliaScheduler(this)
                         : new BukkitScheduler(this);
        this.executionHelper = ReflectionUtil.methodExists(CommandMap.class, "getKnownCommands", new Class[0])
                               ? new PaperGameCommandExecutionHelper(this)
                               : new BukkitGameCommandExecutionHelper(this);
        this.playerProvider = new BukkitPlayerProvider(this, player -> new BukkitPlayerImpl(this, player));
        this.console = new BukkitConsole(this);

        load();
    }

    @Override
    protected void enable() throws Throwable {
        super.enable();

        // Modules
        registerModule(MinecraftToDiscordChatModule::new);
        registerModule(BukkitRequiredLinkingModule::new);

        // Listeners

        if (ReflectionUtil.classExists("org.bukkit.event.player.PlayerAdvancementDoneEvent")) {
            // Advancement
            if (ReflectionUtil.classExists("io.papermc.paper.advancement.AdvancementDisplay")) {
                // Paper (Since 1.17.1)
                registerModule(PaperAdvancementListener::new);
            } else if (ReflectionUtil.classExists("org.bukkit.advancement.AdvancementDisplay")) {
                // Spigot (Since 1.19)
                registerModule(SpigotAdvancementListener::new);
            } else {
                // Generic
                registerModule(BukkitLegacyAdvancementListener::new);
            }
        } else {
            // Achievement
            registerModule(BukkitAchievementListener::new);
        }

        if (PaperComponentHandle.IS_AVAILABLE) {
            // Paper (Since 1.16)
            registerModule(PaperChatListener::new);
            registerModule(PaperChatRenderListener::new);
            registerModule(PaperDeathListener::new);
            registerModule(PaperJoinListener::new);
            registerModule(PaperQuitListener::new);
        } else {
            // Legacy
            registerModule(BukkitChatListener::new);
            registerModule(BukkitChatRenderListener::new);
            registerModule(BukkitDeathListener::new);
            registerModule(BukkitJoinListener::new);
            registerModule(BukkitQuitListener::new);
        }
    }

    @Override
    public BukkitScheduler scheduler() {
        return scheduler;
    }

    @Override
    public @Nullable GameCommandExecutionHelper executeHelper() {
        return executionHelper;
    }

    @Override
    public @NotNull BukkitPlayerProvider playerProvider() {
        return playerProvider;
    }

    @Override
    public BukkitConsole console() {
        return console;
    }
}
