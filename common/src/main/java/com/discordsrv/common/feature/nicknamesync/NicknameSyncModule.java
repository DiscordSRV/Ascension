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

package com.discordsrv.common.feature.nicknamesync;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.module.type.NicknameModule;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.sync.AbstractSyncModule;
import com.discordsrv.common.abstraction.sync.SyncFail;
import com.discordsrv.common.abstraction.sync.result.DiscordPermissionResult;
import com.discordsrv.common.abstraction.sync.result.GenericSyncResults;
import com.discordsrv.common.abstraction.sync.result.ISyncResult;
import com.discordsrv.common.config.main.sync.NicknameSyncConfig;
import com.discordsrv.common.feature.nicknamesync.enums.NicknameSyncCause;
import com.discordsrv.common.feature.nicknamesync.enums.NicknameSyncResult;
import com.discordsrv.common.helper.Someone;
import com.discordsrv.common.util.Game;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * {@code long} is used for the guild id being synced to in Discord.
 * The state is the current
 */
public class NicknameSyncModule extends AbstractSyncModule<DiscordSRV, NicknameSyncConfig, Game, Long, String> {

    public NicknameSyncModule(DiscordSRV discordSRV) {
        super(discordSRV, "NICKNAME_SYNC");
    }

    @Override
    protected String syncName() {
        return "Nickname Sync";
    }

    @Override
    protected String logFileName() {
        return "nicknamesync";
    }

    @Override
    protected String gameTerm() {
        return "nickname";
    }

    @Override
    protected String discordTerm() {
        return "nickname";
    }

    @Override
    protected List<NicknameSyncConfig> configs() {
        return Collections.singletonList(discordSRV.config().nicknameSync);
    }

    @Override
    protected @Nullable ISyncResult doesStateMatch(String one, String two) {
        return StringUtils.equals(one, two) ? NicknameSyncResult.MATCH : null;
    }

    @Override
    public String getRemovedState() {
        return null;
    }

    @Subscribe
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        DiscordUser user = discordSRV.discordAPI().getUser(event.getUser());
        discordChanged(
                NicknameSyncCause.DISCORD_NICKNAME_CHANGED,
                Someone.of(discordSRV, user),
                event.getGuild().getIdLong(),
                event.getNewNickname()
        );
    }

    public void newGameNickname(UUID playerUUID, String newNickname) {
        gameChanged(
                NicknameSyncCause.GAME_NICKNAME_CHANGED,
                Someone.of(discordSRV, playerUUID),
                Game.INSTANCE,
                newNickname
        );
    }

    protected NicknameModule getModule() {
        return discordSRV.getModule(NicknameModule.class);
    }

    @Nullable
    protected String cleanNickname(NicknameSyncConfig config, @Nullable String nickname) {
        if (nickname == null) {
            return nickname;
        }
        for (Map.Entry<Pattern, String> filter : config.nicknameRegexFilters.entrySet()) {
            nickname = filter.getKey().matcher(nickname).replaceAll(filter.getValue());
        }
        return nickname;
    }

    @Override
    protected Task<String> getDiscord(NicknameSyncConfig config, Someone.Resolved someone) {
        DiscordGuild guild = discordSRV.discordAPI().getGuildById(config.serverId);
        if (guild == null) {
            return Task.failed(new SyncFail(GenericSyncResults.GUILD_NOT_FOUND));
        }

        return someone.guildMember(guild)
                .thenApply(DiscordGuildMember::getNickname)
                .thenApply(nickname -> cleanNickname(config, nickname));
    }

    @Override
    protected Task<String> getGame(NicknameSyncConfig config, Someone.Resolved someone) {
        NicknameModule module = getModule();
        if (module == null) {
            return Task.failed(new SyncFail(GenericSyncResults.MODULE_NOT_FOUND));
        }

        return module.getNickname(someone.playerUUID())
                .thenApply(nickname -> cleanNickname(config, nickname));
    }

    @Override
    protected Task<ISyncResult> applyDiscord(
            NicknameSyncConfig config,
            Someone.Resolved someone,
            @Nullable String newNickname
    ) {
        DiscordGuild guild = discordSRV.discordAPI().getGuildById(config.serverId);
        if (guild == null) {
            return Task.completed(GenericSyncResults.GUILD_NOT_FOUND);
        }

        ISyncResult permissionFailResult = DiscordPermissionResult.check(guild.asJDA(), Collections.singleton(Permission.NICKNAME_MANAGE));
        if (permissionFailResult != null) {
            return Task.completed(permissionFailResult);
        }

        return someone.guildMember(guild)
                .thenApply(member -> {
                    if (!member.getGuild().getSelfMember().canInteract(member)) {
                        throw new SyncFail(GenericSyncResults.MEMBER_CANNOT_INTERACT);
                    }
                    return member;
                })
                .thenCompose(member -> member.asJDA().modifyNickname(newNickname).submit())
                .thenApply(v -> NicknameSyncResult.SET_DISCORD);
    }

    @Override
    protected Task<ISyncResult> applyGame(
            NicknameSyncConfig config,
            Someone.Resolved someone,
            @Nullable String newNickname
    ) {
        NicknameModule module = getModule();
        if (module == null) {
            return Task.failed(new SyncFail(GenericSyncResults.MODULE_NOT_FOUND));
        }

        return module.setNickname(someone.playerUUID(), newNickname).thenApply(v -> NicknameSyncResult.SET_GAME);
    }
}
