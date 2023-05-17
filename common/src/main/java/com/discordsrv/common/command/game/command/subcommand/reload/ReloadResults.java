package com.discordsrv.common.command.game.command.subcommand.reload;

import com.discordsrv.api.DiscordSRVApi;

public enum ReloadResults implements DiscordSRVApi.ReloadResult {
    SUCCESS,
    SECURITY_FAILED,
    STORAGE_CONNECTION_FAILED,
    DISCORD_CONNECTION_RELOAD_REQUIRED,
    DISCORD_CONNECTION_FAILED
}
