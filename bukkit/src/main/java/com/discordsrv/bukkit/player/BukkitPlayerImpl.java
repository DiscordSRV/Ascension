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

package com.discordsrv.bukkit.player;

import com.discordsrv.api.task.Task;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import com.discordsrv.bukkit.gamerule.GameRule;
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import com.discordsrv.common.util.ComponentUtil;
import com.discordsrv.common.util.ReflectionUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.Locale;

public class BukkitPlayerImpl extends BukkitPlayer {

    private final PaperComponentHandle.Set<Player> SEND_MESSAGE_HANDLE = PaperComponentHandle.setOrNull(Player.class, "sendMessage");

    public BukkitPlayerImpl(BukkitDiscordSRV discordSRV, Player player) {
        super(discordSRV, player);
    }

    @Override
    public void sendMessage(@NotNull Component message) {
        if (SEND_MESSAGE_HANDLE != null) {
            SEND_MESSAGE_HANDLE.call(player, ComponentUtil.toAPI(message));
            return;
        }
        super.sendMessage(message);
    }

    @Override
    public Task<Void> kick(Component component) {
        if (PaperComponentHandle.IS_AVAILABLE) {
            return discordSRV.scheduler().executeOnMainThread(player, () -> PaperPlayerUtil.kick(player, ComponentUtil.toAPI(component)));
        }
        return super.kick(component);
    }

    @Override
    public void addChatSuggestions(Collection<String> suggestions) {
        if (SpigotPlayerUtil.CHAT_SUGGESTIONS_AVAILABLE) {
            SpigotPlayerUtil.addChatSuggestions(player, suggestions);
        }
    }

    @Override
    public void removeChatSuggestions(Collection<String> suggestions) {
        if (SpigotPlayerUtil.CHAT_SUGGESTIONS_AVAILABLE) {
            SpigotPlayerUtil.removeChatSuggestions(player, suggestions);
        }
    }

    @Override
    public @Nullable SkinInfo skinInfo() {
        if (PaperPlayerUtil.SKIN_AVAILABLE_ONLINE) {
            return PaperPlayerUtil.getSkinInfo(player);
        }
        if (SpigotPlayerUtil.SKIN_AVAILABLE) {
            return SpigotPlayerUtil.getSkinInfo(player);
        }
        return null;
    }

    @Override
    public Locale locale() {
        if (PaperPlayerUtil.LOCALE_SUPPORTED) {
            return PaperPlayerUtil.locale(player);
        }
        if (SpigotPlayerUtil.LOCALE_AVAILABLE) {
            return Locale.forLanguageTag(SpigotPlayerUtil.getLocale(player));
        }
        return null;
    }

    @Override
    public @NonNull String worldName() {
        return player.getWorld().getName();
    }

    @Override
    public @Nullable String worldNamespace() {
        if (SpigotWorldUtil.WORLD_NAMESPACE_AVAILABLE) {
            Key key = SpigotWorldUtil.getWorldKey(player.getWorld());
            return key.namespace();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getGameRuleValueForCurrentWorld(GameRule<T> gameRule) {
        World world = player.getWorld();
        if (ReflectionUtil.classExists("org.bukkit.GameRule")) {
            return PaperPlayerUtil.getGameRuleValue(world, gameRule);
        }

        for (String option : gameRule.getOptions()) {
            String value = world.getGameRuleValue(option);
            if (gameRule.getType().equals(Boolean.class)) {
                return (T) Boolean.valueOf(value);
            }
        }

        return null;
    }

    @Override
    public @NotNull Component displayName() {
        if (PaperComponentHandle.IS_AVAILABLE) {
            return ComponentUtil.fromAPI(PaperPlayerUtil.displayName(player));
        }
        return super.displayName();
    }

    @Override
    public @NotNull Component teamDisplayName() {
        if (PaperPlayerUtil.TEAM_DISPLAY_NAME_AVAILABLE) {
            return ComponentUtil.fromAPI(PaperPlayerUtil.teamDisplayName(player));
        }
        return super.teamDisplayName();
    }

    @Override
    public boolean isChatVisible() {
        if (!PaperPlayerUtil.CLIENT_OPTION_SUPPORTED) {
            return true;
        }
        return PaperPlayerUtil.isChatVisible(player);
    }
}
