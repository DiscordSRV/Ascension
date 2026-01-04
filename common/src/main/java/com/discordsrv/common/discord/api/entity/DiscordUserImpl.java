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

package com.discordsrv.common.discord.api.entity;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.DiscordUserPrimaryGuild;
import com.discordsrv.api.discord.entity.channel.DiscordDMChannel;
import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;
import com.discordsrv.api.placeholder.format.FormattedText;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.discord.api.entity.channel.DiscordDMChannelImpl;
import com.discordsrv.common.core.profile.ProfileImpl;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@PlaceholderPrefix("user_")
public class DiscordUserImpl implements DiscordUser {

    protected final DiscordSRV discordSRV;
    protected final User user;
    protected final boolean self;

    public DiscordUserImpl(DiscordSRV discordSRV, User user) {
        this.discordSRV = discordSRV;
        this.user = user;
        this.self = user.getIdLong() == user.getJDA().getSelfUser().getIdLong();
    }

    @Placeholder("profile")
    public Task<@NotNull ProfileImpl> profile() {
        return discordSRV.profileManager().queryProfile(user.getIdLong());
    }

    @Placeholder("linked_player")
    public Task<@Nullable IPlayer> linkedPlayer() {
        return profile().thenApply(profile -> profile.isLinked() ? profile.playerUUID() : null)
                .thenApply(playerUUID -> discordSRV.playerProvider().player(playerUUID));
    }

    @Placeholder("linked_offline_player")
    public Task<@Nullable IOfflinePlayer> linkedOfflinePlayer() {
        return profile().thenApply(profile -> profile.isLinked() ? profile.playerUUID() : null)
                .then(playerUUID -> discordSRV.playerProvider().lookupOfflinePlayer(playerUUID));
    }

    @Override
    public long getId() {
        return user.getIdLong();
    }

    @Override
    public DiscordUserPrimaryGuild getPrimaryGuild() {
        if (user.getPrimaryGuild() == null) {
            return null;
        }

        return discordSRV.discordAPI().getUserPrimaryGuild(user);
    }

    @Override
    public boolean isSelf() {
        return self;
    }

    @Override
    public boolean isBot() {
        return user.isBot();
    }

    @Override
    public @NotNull String getUsername() {
        return user.getName();
    }

    @Override
    public @NotNull String getEffectiveName() {
        return user.getEffectiveName();
    }

    @Override
    public @NotNull String getDiscriminator() {
        return user.getDiscriminator();
    }

    @Override
    public @Nullable String getAvatarUrl() {
        return user.getAvatarUrl();
    }

    @Override
    public @NotNull String getEffectiveAvatarUrl() {
        return user.getEffectiveAvatarUrl();
    }

    @Override
    public Task<DiscordDMChannel> openPrivateChannel() {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            return discordSRV.discordAPI().notReady();
        }

        return discordSRV.discordAPI().toTask(() -> jda.retrieveUserById(getId()))
                .then(user -> discordSRV.discordAPI().toTask(user::openPrivateChannel))
                .thenApply(privateChannel -> new DiscordDMChannelImpl(discordSRV, privateChannel));
    }

    @Override
    public FormattedText getAsMention() {
        return FormattedText.of(user.getAsMention());
    }

    @Override
    public String toString() {
        return "User:" + getUsername() + "(" + Long.toUnsignedString(getId()) + ")";
    }

    @Override
    public User asJDA() {
        return user;
    }
}
