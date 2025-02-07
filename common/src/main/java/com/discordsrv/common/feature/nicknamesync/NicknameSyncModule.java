package com.discordsrv.common.feature.nicknamesync;

import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.module.type.NicknameModule;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.sync.AbstractSyncModule;
import com.discordsrv.common.abstraction.sync.SyncFail;
import com.discordsrv.common.abstraction.sync.result.GenericSyncResults;
import com.discordsrv.common.abstraction.sync.result.ISyncResult;
import com.discordsrv.common.config.main.NicknameSyncConfig;
import com.discordsrv.common.feature.nicknamesync.enums.NicknameSyncResult;
import com.discordsrv.common.util.CompletableFutureUtil;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * {@link NicknameSyncModule.Game} just references the Game as the sole object being synced to in Minecraft.
 * {@code long} is used for the guild id being synced to in Discord.
 * The state is the current
 */
public class NicknameSyncModule extends AbstractSyncModule<DiscordSRV, NicknameSyncConfig, NicknameSyncModule.Game, Long, String> {

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

    protected NicknameModule getModule() {
        return discordSRV.getModule(NicknameModule.class);
    }

    protected String cleanNickname(String nickname) {
        return nickname;
    }

    @Override
    protected CompletableFuture<String> getDiscord(NicknameSyncConfig config, long userId) {
        DiscordGuild guild = discordSRV.discordAPI().getGuildById(config.serverId);
        if (guild == null) {
            return CompletableFutureUtil.failed(new SyncFail(GenericSyncResults.GUILD_NOT_FOUND));
        }

        return guild.retrieveMemberById(userId)
                .thenApply(DiscordGuildMember::getNickname)
                .thenApply(this::cleanNickname);
    }

    @Override
    protected CompletableFuture<String> getGame(NicknameSyncConfig config, UUID playerUUID) {
        NicknameModule module = getModule();
        if (module == null) {
            return CompletableFutureUtil.failed(new SyncFail(GenericSyncResults.MODULE_NOT_FOUND));
        }

        return module.getNickname(playerUUID).thenApply(this::cleanNickname);
    }

    @Override
    protected CompletableFuture<ISyncResult> applyDiscord(
            NicknameSyncConfig config,
            long userId,
            @Nullable String newNickname
    ) {
        DiscordGuild guild = discordSRV.discordAPI().getGuildById(config.serverId);
        if (guild == null) {
            return CompletableFuture.completedFuture(GenericSyncResults.GUILD_NOT_FOUND);
        }

        return guild.retrieveMemberById(userId)
                .thenCompose(member -> member.asJDA().modifyNickname(newNickname).submit())
                .thenApply(v -> NicknameSyncResult.SET_DISCORD);
    }

    @Override
    protected CompletableFuture<ISyncResult> applyGame(
            NicknameSyncConfig config,
            UUID playerUUID,
            @Nullable String newNickname
    ) {
        NicknameModule module = getModule();
        if (module == null) {
            return CompletableFutureUtil.failed(new SyncFail(GenericSyncResults.MODULE_NOT_FOUND));
        }

        return module.setNickname(playerUUID, newNickname).thenApply(v -> NicknameSyncResult.SET_GAME);
    }

    public enum Game {
        INSTANCE
    }
}
