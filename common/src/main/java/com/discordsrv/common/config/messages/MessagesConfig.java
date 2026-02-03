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

package com.discordsrv.common.config.messages;

import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.config.Config;
import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.helper.BothMessage;
import com.discordsrv.common.config.helper.DiscordMessage;
import com.discordsrv.common.config.helper.MinecraftMessage;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public class MessagesConfig implements Config {

    public static final String FILE_NAME = "messages.yaml";

    @Override
    public final String getFileName() {
        return FILE_NAME;
    }

    private static final String DISCORD_SUCCESS_PREFIX = "✅ ";
    private static final String DISCORD_INPUT_ERROR_PREFIX = "\uD83D\uDDD2️ ";
    private static final String DISCORD_ERROR_PREFIX = "❌ ";

    private static final String DISCORD_USER = "**%user_name%** (<@%user_id%>)";
    private static final String DISCORD_PLAYER = "**%player_name|text:'<Unknown>'%** (%player_uuid%)";
    private static final String DISCORD_PLAYER_SIMPLE = "**%player_name|player_uuid|text:'<Unknown>'%**";

    private static final String MINECRAFT_ERROR_COLOR = "&c";
    private static final String MINECRAFT_SUCCESS_COLOR = "&a";
    private static final String MINECRAFT_NEUTRAL_COLOR = "&b";
    private static final String MINECRAFT_BLURPLE_COLOR = "&#5865F2";

    private static final String MINECRAFT_USER = MINECRAFT_BLURPLE_COLOR + "[hover:show_text:%user_id%][click:copy_to_clipboard:%user_id%]@%user_name%[click][hover]";
    private static final String MINECRAFT_PLAYER = "&f[hover:show_text:%player_uuid%][click:copy_to_clipboard:%player_uuid%]%player_name|text:'<Unknown>'%[click][hover]";

    private static BothMessage both(String minecraftRawFormat, String discordRawFormat) {
        return new BothMessage(minecraft(minecraftRawFormat), discord(discordRawFormat));
    }

    private static BothMessage both(String minecraftRawFormat, SendableDiscordMessage.Builder discordFormat) {
        return new BothMessage(minecraft(minecraftRawFormat), new DiscordMessage(discordFormat));
    }

    private static MinecraftMessage minecraft(String rawFormat) {
        return new MinecraftMessage(rawFormat);
    }

    private static DiscordMessage discord(String rawFormat) {
        return new DiscordMessage(SendableDiscordMessage.builder().setContent(rawFormat));
    }

    @Constants({MINECRAFT_ERROR_COLOR, DISCORD_ERROR_PREFIX})
    public BothMessage noPermission = both(
            "%1Sorry, but you do not have permission to use that command",
            "%2Sorry, but you do not have permission to use that command"
    );

    @Constants({MINECRAFT_ERROR_COLOR, DISCORD_ERROR_PREFIX})
    public BothMessage playerNotFound = both(
            "%1Minecraft player not found",
            "%2Minecraft player not found"
    );

    @Constants({MINECRAFT_ERROR_COLOR, DISCORD_ERROR_PREFIX})
    public BothMessage playerLookupFailed = both(
            "%1Failed to lookup player",
            "%2Failed to lookup player"
    );

    @Constants({MINECRAFT_ERROR_COLOR, DISCORD_ERROR_PREFIX})
    public BothMessage userNotFound = both(
            "%1Discord user not found",
            "%2Discord user not found"
    );

    @Constants({MINECRAFT_ERROR_COLOR, DISCORD_ERROR_PREFIX})
    public BothMessage unableToCheckLinkingStatus = both(
            "%1Unable to check linking status, please try again later",
            "%2Unable to check linking status, please try again later"
    );

    @Constants({MINECRAFT_ERROR_COLOR, DISCORD_ERROR_PREFIX})
    public BothMessage unableToLinkAccountsAtThisTime = both(
            "%1Unable to link accounts at this time, please try again later",
            "%2Unable to link accounts at this time, please try again later"
    );

    @Constants({MINECRAFT_ERROR_COLOR, DISCORD_INPUT_ERROR_PREFIX})
    public BothMessage pleaseSpecifyPlayer = both(
            "%1Please specify the Minecraft player",
            "%2Please specify the Minecraft player"
    );

    @Constants({MINECRAFT_ERROR_COLOR, DISCORD_INPUT_ERROR_PREFIX})
    public BothMessage pleaseSpecifyUser = both(
            "%1Please specify the Discord user",
            "%2Please specify the Discord user"
    );

    @Constants({MINECRAFT_ERROR_COLOR, DISCORD_INPUT_ERROR_PREFIX})
    public BothMessage pleaseSpecifyPlayerOrUser = both(
            "%1Please specify the Minecraft player or Discord user",
            "%2Please specify the Minecraft player or Discord user"
    );

    @Constants({MINECRAFT_ERROR_COLOR, DISCORD_INPUT_ERROR_PREFIX})
    public BothMessage pleaseWaitBeforeRunningThatCommandAgain = both(
            "%1Please wait before running that command again",
            "%2Please wait before running that command again"
    );

    public MinecraftMessage targetCommandArgumentDescription = minecraft(
            "Minecraft player username or UUID or Discord user username or user id"
    );

    public BothMessage playerCommandArgumentDescription = both(
            "Minecraft player username or UUID",
            "Minecraft player username or UUID"
    );

    public BothMessage discordUserCommandArgumentDescription = both(
            "Discord user username or user id",
            "Discord user"
    );

    // DiscordSRV command

    public DiscordMessage discordsrvCommandDescription = discord(
            "DiscordSRV Commands"
    );

    // Minecraft command

    public DiscordMessage minecraftCommandDescription = discord(
            "Minecraft Server Commands"
    );

    // Help command

    public MinecraftMessage helpCommandDescription = minecraft(
            "View command help"
    );

    // PlayerList command

    public DiscordMessage playerListCommandDescription = discord(
            "View players that are online on the server"
    );

    // Linked command

    @Constants({
            MINECRAFT_USER,
            MINECRAFT_NEUTRAL_COLOR,
            MINECRAFT_PLAYER,
            DISCORD_SUCCESS_PREFIX + DISCORD_USER,
            DISCORD_PLAYER
    })
    public BothMessage discordUserLinkedTo3rd = both(
            "%1 %2is linked to %3",
            "%4 is linked to %5"
    );

    @Constants({
            MINECRAFT_PLAYER,
            MINECRAFT_NEUTRAL_COLOR,
            MINECRAFT_USER,
            DISCORD_SUCCESS_PREFIX,
            DISCORD_PLAYER,
            DISCORD_USER
    })
    public BothMessage minecraftPlayerLinkedTo3rd = both(
            "%1 %2is linked to %3",
            "%4%5 is linked to %6"
    );

    @Constants({
            MINECRAFT_NEUTRAL_COLOR,
            MINECRAFT_SUCCESS_COLOR + MINECRAFT_USER,
            DISCORD_SUCCESS_PREFIX,
            DISCORD_PLAYER
    })
    public BothMessage linkedTo1st = both(
            "%1You are linked to %2",
            "%3You are linked to %4"
    );

    // Unlink command

    @Constants({MINECRAFT_SUCCESS_COLOR, DISCORD_SUCCESS_PREFIX})
    public BothMessage unlinkSuccess = both(
            "%1Accounts unlinked",
            "%2Accounts unlinked"
    );

    public BothMessage unlinkCommandDescription = both(
            "Unlink accounts",
            "Unlink a Minecraft player's or Discord user's account (only specify one)"
    );

    // Linked & Unlink command

    @Constants({
            MINECRAFT_USER,
            MINECRAFT_NEUTRAL_COLOR,
            MINECRAFT_ERROR_COLOR,
            DISCORD_ERROR_PREFIX,
            DISCORD_USER
    })
    public BothMessage discordUserUnlinked3rd = both(
            "%1 %2is %3unlinked",
            "%4%5 is __unlinked__"
    );

    @Constants({
            MINECRAFT_PLAYER,
            MINECRAFT_NEUTRAL_COLOR,
            MINECRAFT_ERROR_COLOR,
            DISCORD_ERROR_PREFIX,
            DISCORD_PLAYER
    })
    public BothMessage minecraftPlayerUnlinked3rd = both(
            "%1 %2is %3unlinked",
            "%4%5 is __unlinked__"
    );

    @Constants({
            MINECRAFT_NEUTRAL_COLOR,
            MINECRAFT_ERROR_COLOR,
            DISCORD_ERROR_PREFIX,
    })
    public BothMessage alreadyUnlinked1st = both(
            "%1You are %2unlinked",
            "%3You are unlinked"
    );

    public BothMessage linkedCommandDescription = both(
            "Check the account a Minecraft Player or Discord User is linked to",
            "Check the account a Minecraft Player or Discord user is linked to"
    );

    // Link (self) command

    @Constants(MINECRAFT_ERROR_COLOR)
    public MinecraftMessage pleaseSpecifyPlayerAndUserToLink = minecraft("%1Please specify the Minecraft player and the Discord user to link");

    @Constants({MINECRAFT_ERROR_COLOR, DISCORD_ERROR_PREFIX})
    public BothMessage alreadyLinked1st = both(
            "%1You are already linked",
            "%2You are already linked"
    );

    @Constants({
            MINECRAFT_SUCCESS_COLOR,
            MINECRAFT_USER + MINECRAFT_SUCCESS_COLOR,
            DISCORD_SUCCESS_PREFIX,
            DISCORD_PLAYER_SIMPLE
    })
    public BothMessage nowLinked1st = both(
            "%1You are now linked to %2!",
            "%3You are now linked to %4"
    );

    @Constants(MINECRAFT_NEUTRAL_COLOR)
    public MinecraftMessage checkingLinkStatus = minecraft("%1Checking linking status...");

    @Constants({
            MINECRAFT_NEUTRAL_COLOR,
            "&f[click:open_url:%minecraftauth_link%][hover:show_text:Click to open]%minecraftauth_link_simple%[click]" + MINECRAFT_NEUTRAL_COLOR,
            "&fMinecraftAuth"
    })
    public MinecraftMessage minecraftAuthLinking = minecraft("%1Please visit %2 to link your account through %3");

    @Constants({
            MINECRAFT_NEUTRAL_COLOR,
            MINECRAFT_BLURPLE_COLOR,
            "[click:open_url:%discord_invite%]%discord_invite_simple%[click]",
            "[click:copy_to_clipboard:%code%][hover:show_text:Click to copy linking code]/%discordcommand_minecraft_alias% link %code%"
    })
    public MinecraftMessage storageLinking = minecraft(
            "%1Join our %2Discord %1server at %3 %1and link your account by running the &r%4"
    );

    @Constants(DISCORD_ERROR_PREFIX)
    public DiscordMessage invalidLinkingCode = discord("%1Invalid linking code");

    public BothMessage linkCommandDescription = both(
            "Link accounts",
            "Link accounts"
    );

    // Link (other) command

    @Constants({MINECRAFT_ERROR_COLOR, DISCORD_ERROR_PREFIX})
    public BothMessage playerAlreadyLinked3rd = both(
            "%1That player is already linked",
            "%2That Minecraft player is already linked"
    );

    @Constants({MINECRAFT_ERROR_COLOR, DISCORD_ERROR_PREFIX})
    public BothMessage userAlreadyLinked3rd = both(
            "%1That Discord user is already linked",
            "%2That Discord user is already linked"
    );

    @Constants({
            MINECRAFT_SUCCESS_COLOR,
            MINECRAFT_NEUTRAL_COLOR,
            MINECRAFT_PLAYER + MINECRAFT_NEUTRAL_COLOR,
            MINECRAFT_USER + MINECRAFT_NEUTRAL_COLOR,
            DISCORD_SUCCESS_PREFIX,
            DISCORD_PLAYER,
            DISCORD_USER
    })
    public BothMessage nowLinked3rd = both(
            "%1Link created successfully %2(%3 and %4)",
            "%5Link created successfully\n%6 and %7"
    );

    public DiscordMessage linkOtherCommandDescription = discord(
            "Manually link accounts"
    );

    // Bypass command

    public BothMessage bypassCommandDescription = both(
            "Manage players which bypass required linking requirements",
            "Manage players which bypass required linking requirements"
    );
    public BothMessage bypassAddCommandDescription = both(
            "Add a player to the list of players who bypass required linking requirements",
            "Add a player to the list of players who bypass required linking requirements"
    );
    public BothMessage bypassRemoveCommandDescription = both(
            "Remove a player to the list of players who bypass required linking requirements",
            "Remove a player to the list of players who bypass required linking requirements"
    );
    public BothMessage bypassListCommandDescription = both(
            "List players who bypass required linking requirements",
            "List players who bypass required linking requirements"
    );

    @Constants({MINECRAFT_SUCCESS_COLOR, DISCORD_SUCCESS_PREFIX})
    public BothMessage cannotAlterBypassAlreadyInConfig = both(
            "%1That player is bypassing via the configuration file",
            "%2That player is bypassing via the configuration file"
    );

    @Constants({MINECRAFT_ERROR_COLOR, DISCORD_ERROR_PREFIX})
    public BothMessage alreadyBypassing = both(
            "%1Already bypassing",
            "%2Already bypassing"
    );

    @Constants({MINECRAFT_ERROR_COLOR, DISCORD_ERROR_PREFIX})
    public BothMessage notBypassing = both(
            "%1Not bypassing",
            "%2Not bypassing"
    );

    @Constants({
            MINECRAFT_PLAYER + MINECRAFT_NEUTRAL_COLOR,
            DISCORD_PLAYER
    })
    public BothMessage bypassAdded = both(
            "%1 has been added to the bypass list",
            "%2 has been added to the bypass list"
    );

    @Constants({
            MINECRAFT_PLAYER + MINECRAFT_NEUTRAL_COLOR,
            DISCORD_PLAYER
    })
    public BothMessage bypassRemoved = both(
            "%1 has been removed from the bypass list",
            "%2 has been removed from the bypass list"
    );

    // Broadcast command

    @Constants({MINECRAFT_ERROR_COLOR, MINECRAFT_NEUTRAL_COLOR, MINECRAFT_ERROR_COLOR})
    public MinecraftMessage channelNotFound = minecraft("%1Channel %2%channel% %3not found");

    @Constants(MINECRAFT_SUCCESS_COLOR)
    public MinecraftMessage broadcasted = minecraft("%1Broadcasted");

    public MinecraftMessage broadcastMinecraftCommandDescription = minecraft(
            "Broadcast a Minecraft formatted message to the specified Discord channel(s)"
    );
    public MinecraftMessage broadcastRawCommandDescription = minecraft(
            "Broadcast a raw json message to the specified Discord channel(s)"
    );
    public MinecraftMessage broadcastDiscordCommandDescription = minecraft(
            "Broadcast a plain message with Discord formatting to the specified Discord channel(s)"
    );
    public MinecraftMessage broadcastChannelParameterDescription = minecraft(
            "The Discord channel id or in-game channel name to resolve Discord channels from the configuration"
    );
    public MinecraftMessage broadcastMessageParameterDescription = minecraft(
            "The message to broadcast to Discord"
    );

    // Debug commands

    public BothMessage debugCommandDescription = both(
            "Diagnostic reporting commands",
            "Diagnostic reporting commands"
    );
    public BothMessage debugStartCommandDescription = both(
            "Start debug observation",
            "Start debug observation"
    );
    public BothMessage debugStopCommandDescription = both(
            "Stop debug observation",
            "Stop debug observation"
    );
    public BothMessage debugUploadCommandDescription = both(
            "Upload a debug report for easy sharing",
            "Upload a debug report for easy sharing"
    );
    public BothMessage debugZipCommandDescription = both(
            "Create a debug report into a local zip",
            "Create a debug report into a local zip"
    );

    // Parse command

    public BothMessage parseCommandDescription = both(
            "Parse placeholders through DiscordSRV's placeholder service",
            "Parse placeholders through DiscordSRV's placeholder service"
    );

    public BothMessage parseInputCommandDescription = both(
            "The content to pass to the placeholder service",
            "The content to pass to the placeholder service"
    );

    // Version command

    public BothMessage versionCommandDescription = both(
            "Show the version of DiscordSRV",
            "Show the version of DiscordSRV"
    );

    // Reload command

    public MinecraftMessage reloadCommandDescription = minecraft(
            "Reloads the specified DiscordSRV functions"
    );

    public MinecraftMessage reloadFunctionsParameterDescription = minecraft(
            "The functions to reload"
    );

    // Resync command

    public BothMessage resyncCommandDescription = both(
            "Resync online players",
            "Resync online players"
    );

    public BothMessage resyncSyncParameterDescription = both(
            "The synchronization to resync",
            "The synchronization to resync"
    );

    // Execute command

    @Constants(DISCORD_ERROR_PREFIX)
    public DiscordMessage executeCommandDisabled = discord(
            "%1The execute command is disabled"
    );

    @Constants(DISCORD_ERROR_PREFIX)
    public DiscordMessage executing = discord(
            "Executing `%command%` command"
    );

    public DiscordMessage executeCommandDescription = discord(
            "Run Minecraft console commands"
    );

    public DiscordMessage executeCommandParameterDescription = discord(
            "The command to run"
    );

}
