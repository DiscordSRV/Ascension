/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.component.renderer;

import com.discordsrv.api.discord.api.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.api.entity.guild.DiscordRole;
import com.discordsrv.common.DiscordSRV;
import dev.vankka.mcdiscordreserializer.renderer.implementation.DefaultMinecraftRenderer;
import lombok.NonNull;
import net.dv8tion.jda.api.entities.AbstractChannel;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

public class DiscordSRVMinecraftRenderer extends DefaultMinecraftRenderer {

    private static final ThreadLocal<Long> GUILD_CONTEXT = ThreadLocal.withInitial(() -> 0L);
    private final DiscordSRV discordSRV;

    public DiscordSRVMinecraftRenderer(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    public static void runInGuildContext(long guildId, Runnable runnable) {
        getWithGuildContext(guildId, () -> {
            runnable.run();
            return null;
        });
    }

    public static <T> T getWithGuildContext(long guildId, Supplier<T> supplier) {
        GUILD_CONTEXT.set(guildId);
        T output = supplier.get();
        GUILD_CONTEXT.set(0L);
        return output;
    }

    @Override
    public @Nullable Component appendChannelMention(@NonNull Component component, @NonNull String id) {
        return component.append(Component.text(
                discordSRV.jda()
                        .map(jda -> jda.getGuildChannelById(id))
                        .map(AbstractChannel::getName)
                        .map(name -> "#" + name)
                        .orElse("<#" + id + ">")
        ));
    }

    @Override
    public @Nullable Component appendUserMention(@NonNull Component component, @NonNull String id) {
        long guildId = GUILD_CONTEXT.get();
        Optional<DiscordGuild> guild = guildId > 0
                ? discordSRV.discordAPI().getGuildById(guildId)
                : Optional.empty();

        long userId = MiscUtil.parseLong(id);
        return component.append(Component.text(
                guild.flatMap(g -> g.getMemberById(userId))
                        .map(member -> "@" + member.getEffectiveName())
                        .orElseGet(() -> discordSRV.discordAPI()
                                .getUserById(userId)
                                .map(user -> "@" + user.getUsername())
                                .orElse("<@" + id + ">"))
        ));
    }

    @Override
    public @Nullable Component appendRoleMention(@NonNull Component component, @NonNull String id) {
        return component.append(Component.text(
                discordSRV.discordAPI()
                        .getRoleById(MiscUtil.parseLong(id))
                        .map(DiscordRole::getName)
                        .map(name -> "@" + name)
                        .orElse("<@" + id + ">")
        ));
    }
}
