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

package com.discordsrv.common.config.messages;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.command.combined.abstraction.CommandExecution;
import com.discordsrv.common.config.Config;
import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.config.configurate.annotation.Untranslated;
import com.discordsrv.common.config.helper.DiscordMessage;
import com.discordsrv.common.config.helper.MinecraftMessage;
import com.discordsrv.common.util.CompletableFutureUtil;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@ConfigSerializable
public class MessagesConfig implements Config {

    public static final String FILE_NAME = "messages.yaml";

    @Override
    public final String getFileName() {
        return FILE_NAME;
    }

    // Helper methods

    private void withPlayer(DiscordSRV discordSRV, UUID playerUUID, Consumer<IOfflinePlayer> playerConsumer) {
        CompletableFuture<IOfflinePlayer> playerFuture = CompletableFutureUtil.timeout(
                discordSRV,
                discordSRV.playerProvider().lookupOfflinePlayer(playerUUID),
                Duration.ofSeconds(5)
        );

        playerFuture.whenComplete((player, __) -> playerConsumer.accept(player));
    }

    private void withUser(DiscordSRV discordSRV, long userId, Consumer<DiscordUser> userConsumer) {
        CompletableFuture<DiscordUser> userFuture = CompletableFutureUtil.timeout(
                discordSRV,
                discordSRV.discordAPI().retrieveUserById(userId),
                Duration.ofSeconds(5)
        );

        userFuture.whenComplete((player, __) -> userConsumer.accept(player));
    }

    private void withPlayerAndUser(
            DiscordSRV discordSRV,
            UUID playerUUID,
            long userId,
            BiConsumer<IOfflinePlayer, DiscordUser> playerAndUserConsumer
    ) {
        CompletableFuture<IOfflinePlayer> playerFuture = CompletableFutureUtil.timeout(
                discordSRV,
                discordSRV.playerProvider().lookupOfflinePlayer(playerUUID),
                Duration.ofSeconds(5)
        );
        CompletableFuture<DiscordUser> userFuture = CompletableFutureUtil.timeout(
                discordSRV,
                discordSRV.discordAPI().retrieveUserById(userId),
                Duration.ofSeconds(5)
        );

        playerFuture.whenComplete((player, __) -> userFuture.whenComplete((user, ___) -> playerAndUserConsumer.accept(player, user)));
    }

    // Methods for responding directly to CommandExecutions

    public void playerNotFound(CommandExecution execution) {
        execution.send(
                minecraft.playerNotFound.asComponent(),
                discord.playerNotFound.get()
        );
    }

    public void userNotFound(CommandExecution execution) {
        execution.send(
                minecraft.userNotFound.asComponent(),
                discord.userNotFound.get()
        );
    }

    public void unableToCheckLinkingStatus(CommandExecution execution) {
        execution.send(
                minecraft.unableToCheckLinkingStatus.asComponent(),
                discord.unableToCheckLinkingStatus.get()
        );
    }

    public void playerAlreadyLinked3rd(CommandExecution execution) {
        execution.send(
                minecraft.playerAlreadyLinked3rd.asComponent(),
                discord.playerAlreadyLinked3rd.get()
        );
    }

    public void userAlreadyLinked3rd(CommandExecution execution) {
        execution.send(
                minecraft.userAlreadyLinked3rd.asComponent(),
                discord.userAlreadyLinked3rd.get()
        );
    }

    public void nowLinked3rd(DiscordSRV discordSRV, CommandExecution execution, UUID playerUUID, long userId) {
        withPlayerAndUser(discordSRV, playerUUID, userId, (player, user) -> execution.send(
                minecraft.nowLinked3rd.textBuilder()
                        .applyPlaceholderService()
                        .addContext(user, player)
                        .addPlaceholder("user_id", userId)
                        .addPlaceholder("player_uuid", playerUUID)
                        .build(),
                execution.messages().discord.nowLinked3rd.format()
                        .addContext(user, player)
                        .addPlaceholder("user_id", userId)
                        .addPlaceholder("player_uuid", playerUUID)
                        .applyPlaceholderService()
                        .build()
        ));
    }

    public void discordUserLinkedTo(
            DiscordSRV discordSRV,
            CommandExecution execution,
            UUID playerUUID,
            long userId
    ) {
        withPlayerAndUser(discordSRV, playerUUID, userId, (player, user) -> execution.send(
                minecraft.discordUserLinkedTo
                        .textBuilder()
                        .applyPlaceholderService()
                        .addContext(user, player)
                        .addPlaceholder("user_id", userId)
                        .addPlaceholder("player_uuid", playerUUID)
                        .build(),
                discord.discordUserLinkedTo.format()
                        .addContext(user, player)
                        .addPlaceholder("user_id", userId)
                        .addPlaceholder("player_uuid", playerUUID)
                        .applyPlaceholderService()
                        .build()
        ));
    }

    public void discordUserUnlinked(
            DiscordSRV discordSRV,
            CommandExecution execution,
            long userId
    ) {
        withUser(discordSRV, userId, (user) -> execution.send(
                minecraft.discordUserUnlinked
                        .textBuilder()
                        .applyPlaceholderService()
                        .addContext(user)
                        .addPlaceholder("user_id", userId)
                        .build(),
                discord.discordUserUnlinked.format()
                        .addContext(user)
                        .addPlaceholder("user_id", userId)
                        .applyPlaceholderService()
                        .build()
        ));
    }

    public void minecraftPlayerLinkedTo(
            DiscordSRV discordSRV,
            CommandExecution execution,
            UUID playerUUID,
            long userId
    ) {
        withPlayerAndUser(discordSRV, playerUUID, userId, (player, user) -> execution.send(
                minecraft.minecraftPlayerLinkedTo
                        .textBuilder()
                        .applyPlaceholderService()
                        .addContext(player, user)
                        .addPlaceholder("player_uuid", playerUUID)
                        .addPlaceholder("user_id", userId)
                        .build(),
                discord.minecraftPlayerLinkedTo.format()
                        .addContext(player, user)
                        .addPlaceholder("player_uuid", playerUUID)
                        .addPlaceholder("user_id", userId)
                        .applyPlaceholderService()
                        .build()
        ));
    }

    public void minecraftPlayerUnlinked(
            DiscordSRV discordSRV,
            CommandExecution execution,
            UUID playerUUID
    ) {
        withPlayer(discordSRV, playerUUID, (player) -> execution.send(
                minecraft.minecraftPlayerUnlinked
                        .textBuilder()
                        .applyPlaceholderService()
                        .addContext(player)
                        .addPlaceholder("player_uuid", playerUUID)
                        .build(),
                discord.minecraftPlayerUnlinked.format()
                        .addPlaceholder("player_uuid", playerUUID)
                        .applyPlaceholderService()
                        .build()
        ));
    }

    public void unlinked(CommandExecution execution) {
        execution.send(
                minecraft.unlinked.asComponent(),
                discord.unlinked.get()
        );
    }

    public Minecraft minecraft = new Minecraft();

    @ConfigSerializable
    public static class Minecraft {
        private static final String ERROR_COLOR = "&c";
        private static final String SUCCESS_COLOR = "&a";
        private static final String NEUTRAL_COLOR = "&b";
        private static final String BLURPLE_COLOR = "&#5865F2";

        private MinecraftMessage make(String rawFormat) {
            return new MinecraftMessage(rawFormat);
        }

        @Comment("Generic")
        @Constants(ERROR_COLOR)
        public MinecraftMessage noPermission = make("%1Sorry, but you do not have permission to use that command");
        @Constants(ERROR_COLOR)
        public MinecraftMessage pleaseSpecifyPlayer = make("%1Please specify the Minecraft player");
        @Constants(ERROR_COLOR)
        public MinecraftMessage pleaseSpecifyUser = make("%1Please specify the Discord user");
        @Constants(ERROR_COLOR)
        public MinecraftMessage pleaseSpecifyPlayerOrUser = make("%1Please specify the Minecraft player or Discord user");
        @Constants(ERROR_COLOR)
        public MinecraftMessage playerNotFound = make("%1Minecraft player not found");
        @Constants(ERROR_COLOR)
        public MinecraftMessage userNotFound = make("%1Discord user not found");
        @Constants(ERROR_COLOR)
        public MinecraftMessage unableToCheckLinkingStatus = make("%1Unable to check linking status, please try again later");

        @Constants({
                SUCCESS_COLOR + "[hover:show_text:%user_id%][click:copy_to_clipboard:%user_id%]@%user_name%[click][hover]",
                NEUTRAL_COLOR,
                SUCCESS_COLOR + "[hover:show_text:%player_uuid%][click:copy_to_clipboard:%player_uuid%]%player_name|text:'<Unknown>'%[click][hover]"
        })
        public MinecraftMessage discordUserLinkedTo = make("%1 %2is linked to %3");

        @Constants({
                SUCCESS_COLOR + "[hover:show_text:%user_id%][click:copy_to_clipboard:%user_id%]@%user_name%[click][hover]",
                NEUTRAL_COLOR,
                ERROR_COLOR
        })
        public MinecraftMessage discordUserUnlinked = make("%1 %2is %3unlinked");

        @Constants({
                SUCCESS_COLOR + "[hover:show_text:%player_uuid%][click:copy_to_clipboard:%player_uuid%]%player_name|text:'<Unknown>'%[click][hover]",
                NEUTRAL_COLOR,
                SUCCESS_COLOR + "[hover:show_text:%user_id%][click:copy_to_clipboard:%user_id%]@%user_name%[click][hover]"
        })
        public MinecraftMessage minecraftPlayerLinkedTo = make("%1 %2is linked to %3");

        @Constants({
                SUCCESS_COLOR + "[hover:show_text:%player_uuid%][click:copy_to_clipboard:%player_uuid%]%player_name|text:'<Unknown>'%[click][hover]",
                NEUTRAL_COLOR,
                ERROR_COLOR
        })
        public MinecraftMessage minecraftPlayerUnlinked = make("%1 %2is %3unlinked");

        @Untranslated(Untranslated.Type.COMMENT)
        @Comment("/discord link")
        @Constants(ERROR_COLOR)
        public MinecraftMessage alreadyLinked1st = make("%1You are already linked");
        @Constants(ERROR_COLOR)
        public MinecraftMessage pleaseSpecifyPlayerAndUserToLink = make("%1Please specify the Minecraft player and the Discord user to link");
        @Constants(ERROR_COLOR)
        public MinecraftMessage playerAlreadyLinked3rd = make("%1That player is already linked");
        @Constants(ERROR_COLOR)
        public MinecraftMessage userAlreadyLinked3rd = make("%1That player is already linked");
        @Constants(ERROR_COLOR)
        public MinecraftMessage pleaseWaitBeforeRunningThatCommandAgain = make("%1Please wait before running that command again");
        @Constants(ERROR_COLOR)
        public MinecraftMessage unableToLinkAtThisTime = make("%1Unable to check linking status, please try again later");
        @Constants(NEUTRAL_COLOR)
        public MinecraftMessage checkingLinkStatus = make("%1Checking linking status...");
        @Constants(SUCCESS_COLOR)
        public MinecraftMessage nowLinked1st = make("%1You are now linked!");
        @Constants({
                SUCCESS_COLOR,
                NEUTRAL_COLOR,
                SUCCESS_COLOR + "[hover:show_text:%player_uuid%][click:copy_to_clipboard:%player_uuid%]%player_name|text:'<Unknown>'%[click][hover]" + NEUTRAL_COLOR,
                SUCCESS_COLOR + "[hover:show_text:%user_id%][click:copy_to_clipboard:%user_id%]@%user_name%[click][hover]" + NEUTRAL_COLOR
        })
        public MinecraftMessage nowLinked3rd = make("%1Link created successfully %2(%3 and %4)");
        @Constants({
                NEUTRAL_COLOR,
                "&f[click:open_url:%minecraftauth_link%][hover:show_text:Click to open]%minecraftauth_link_simple%[click]" + NEUTRAL_COLOR,
                "&fMinecraftAuth"
        })
        public MinecraftMessage minecraftAuthLinking = make("%1Please visit %2 to link your account through %3");
        @Constants({NEUTRAL_COLOR, BLURPLE_COLOR})
        public MinecraftMessage storageLinking = make(
                "%1Join our %2Discord %1server at "
                        + "[click:open_url:%discord_invite%]%discord_invite_simple%[click]"
                        + " %1and link your account by running the "
                        + "&r[click:copy_to_clipboard:%code%][hover:show_text:Click to copy linking code]/minecraft link %code%"); // TODO

        @Untranslated(Untranslated.Type.COMMENT)
        @Comment("/discord unlink")
        @Constants({SUCCESS_COLOR})
        public MinecraftMessage unlinked = make("%1Accounts unlinked");

    }

    public Discord discord = new Discord();

    @ConfigSerializable
    public static class Discord {

        private static final String SUCCESS_PREFIX = "✅ ";
        private static final String INPUT_ERROR_PREFIX = "\uD83D\uDDD2️ ";
        private static final String ERROR_PREFIX = "❌ ";

        private DiscordMessage make(String rawFormat) {
            return new DiscordMessage(SendableDiscordMessage.builder().setContent(rawFormat));
        }

        @Comment("Generic")
        @Constants(INPUT_ERROR_PREFIX)
        public DiscordMessage pleaseSpecifyPlayer = make("%1Please specify the Minecraft player");
        @Constants(INPUT_ERROR_PREFIX)
        public DiscordMessage pleaseSpecifyUser = make("%1Please specify the Discord user");
        @Constants(INPUT_ERROR_PREFIX)
        public DiscordMessage pleaseSpecifyPlayerOrUser = make("%1Please specify the Minecraft player or Discord user");
        @Constants(ERROR_PREFIX)
        public DiscordMessage playerNotFound = make("%1Minecraft player not found");
        @Constants(ERROR_PREFIX)
        public DiscordMessage userNotFound = make("%1Discord user not found");
        @Constants(ERROR_PREFIX)
        public DiscordMessage unableToCheckLinkingStatus = make("%1Unable to check linking status, please try again later");

        @Constants({
                SUCCESS_PREFIX,
                "**%user_name%** (<@%user_id%>)",
                "**%player_name%** (%player_uuid%)"
        })
        public DiscordMessage discordUserLinkedTo = make("%1%2 is linked to %3");

        @Constants({
                ERROR_PREFIX,
                "**%user_name%** (<@%user_id%>)"
        })
        public DiscordMessage discordUserUnlinked = make("%1%2 is __unlinked__");

        @Constants({
                SUCCESS_PREFIX,
                "**%player_name%** (%player_uuid%)",
                "**%user_name%** (<@%user_id%>)"
        })
        public DiscordMessage minecraftPlayerLinkedTo = make("%1%2 is linked to %3");

        @Constants({
                ERROR_PREFIX,
                "**%player_name%** (%player_uuid%)"
        })
        public DiscordMessage minecraftPlayerUnlinked = make("%1%2 is __unlinked__");

        @Untranslated(Untranslated.Type.COMMENT)
        @Comment("/discord link")
        @Constants(ERROR_PREFIX)
        public DiscordMessage playerAlreadyLinked3rd = make("%1That Minecraft player is already linked");
        @Constants(ERROR_PREFIX)
        public DiscordMessage userAlreadyLinked3rd = make("%1That Discord user is already linked");
        @Constants(ERROR_PREFIX)
        public DiscordMessage alreadyLinked1st = make("%1You are already linked");
        @Constants({
                SUCCESS_PREFIX,
                "**%player_name%** (%player_uuid%)",
                "**%user_name%** (<@%user_id%>)"
        })
        public DiscordMessage nowLinked3rd = make("%1Link created successfully\n%2 and %3");
        @Constants(ERROR_PREFIX)
        public DiscordMessage pleaseWaitBeforeRunningThatCommandAgain = make("%1Please wait before running that command again");
        @Constants(ERROR_PREFIX)
        public DiscordMessage invalidLinkingCode = make("%1Invalid linking code");
        @Constants({SUCCESS_PREFIX, "**%player_name%**"})
        public DiscordMessage accountLinked = make("%1Account linked to %2 successfully");

        @Untranslated(Untranslated.Type.COMMENT)
        @Comment("/discord unlink")
        @Constants({SUCCESS_PREFIX})
        public DiscordMessage unlinked = make("%1Accounts unlinked");

    }
}
