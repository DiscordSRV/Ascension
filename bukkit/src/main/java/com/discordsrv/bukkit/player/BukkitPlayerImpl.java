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

package com.discordsrv.bukkit.player;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.component.PaperComponentHandle;
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class BukkitPlayerImpl extends BukkitPlayer {

    public BukkitPlayerImpl(BukkitDiscordSRV discordSRV, Player player) {
        super(discordSRV, player);
    }

    @Override
    public CompletableFuture<Void> kick(Component component) {
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
        if (PaperPlayerUtil.SKIN_AVAILABLE) {
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
    public @NotNull Component displayName() {
        if (PaperComponentHandle.IS_AVAILABLE) {
            MinecraftComponent displayName = PaperPlayerUtil.displayName(player);
            if (displayName != null) {
                return ComponentUtil.fromAPI(displayName);
            }
        }
        return super.displayName();
    }
}
