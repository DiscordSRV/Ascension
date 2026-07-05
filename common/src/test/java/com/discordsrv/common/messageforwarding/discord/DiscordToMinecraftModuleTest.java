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

package com.discordsrv.common.messageforwarding.discord;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.DiscordUserPrimaryGuild;
import com.discordsrv.api.discord.entity.channel.DiscordChannelType;
import com.discordsrv.api.discord.entity.channel.DiscordDMChannel;
import com.discordsrv.api.discord.entity.channel.DiscordMessageChannel;
import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.entity.guild.DiscordGuildMember;
import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.ReceivedDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.MockDiscordSRV;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.generic.DiscordUserFilterConfig;
import com.discordsrv.common.config.main.generic.FilterMode;
import com.discordsrv.common.feature.messageforwarding.discord.DiscordToMinecraftChatModule;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class DiscordToMinecraftModuleTest {

    @Test
    public void testInputValidation() {
        List<String> stringsThatShouldBeRemoved = Arrays.asList(
                "\u0000",
                "\u0001",
                "\u0002"
        );

        BaseChannelConfig config = new BaseChannelConfig();

        for (String string : stringsThatShouldBeRemoved) {
            Component component = DiscordToMinecraftChatModule.convertToComponent(MockDiscordSRV.getInstance(), message(string, 0), config);
            String plain = PlainTextComponentSerializer.plainText().serialize(component);
            Assertions.assertFalse(plain.contains(string));
        }
    }

    @Test
    public void testFormattingAllowed() {
        long userIdAllowed = 1;

        BaseChannelConfig config = new BaseChannelConfig();
        config.discordToMinecraft.formattingLimit.filters.add(new DiscordUserFilterConfig.SingleFilter(userIdAllowed, FilterMode.WHITELIST));

        Map<String, TextDecoration> formats = new LinkedHashMap<>();
        formats.put("**text**", TextDecoration.BOLD);
        formats.put("~~text~~", TextDecoration.STRIKETHROUGH);
        formats.put("__text__", TextDecoration.UNDERLINED);
        formats.put("*text*", TextDecoration.ITALIC);

        for (Map.Entry<String, TextDecoration> entry : formats.entrySet()) {
            Component notAllowed = DiscordToMinecraftChatModule.convertToComponent(MockDiscordSRV.getInstance(), message(entry.getKey(), 0), config);
            Component allowed = DiscordToMinecraftChatModule.convertToComponent(MockDiscordSRV.getInstance(), message(entry.getKey(), userIdAllowed), config);

            TextDecoration decoration = entry.getValue();
            Assertions.assertFalse(anyComponentHas(notAllowed, component -> component.hasDecoration(decoration)), "Should not have format " + decoration);
            Assertions.assertTrue(anyComponentHas(allowed, component -> component.hasDecoration(decoration)), "Should have format " + decoration);
        }
    }

    @Test
    public void regexFilteringTest() {
        BaseChannelConfig config = new BaseChannelConfig();
        config.discordToMinecraft.contentRegexFilters.clear();

        // Validate order being kept
        config.discordToMinecraft.contentRegexFilters.put(Pattern.compile("first"), "second");
        config.discordToMinecraft.contentRegexFilters.put(Pattern.compile("second"), "first");

        config.discordToMinecraft.contentRegexFilters.put(Pattern.compile("cat"), "dog");
        config.discordToMinecraft.contentRegexFilters.put(Pattern.compile("asd|zxc"), "qwe");

        Map<String, String> tests = new LinkedHashMap<>();
        tests.put("first", "first");
        tests.put("second", "first");
        tests.put("cat", "dog");
        tests.put("cats", "dogs");
        tests.put("  asd!", "  qwe!");
        tests.put("  zxc!", "  qwe!");

        for (Map.Entry<String, String> entry : tests.entrySet()) {
            Component component = DiscordToMinecraftChatModule.convertToComponent(MockDiscordSRV.getInstance(), message(entry.getKey(), 0), config);
            String plain = PlainTextComponentSerializer.plainText().serialize(component);

            Assertions.assertEquals(entry.getValue(), plain);
        }
    }

    @Test
    public void textConveyedNormallyTest() {
        BaseChannelConfig config = new BaseChannelConfig();

        // Various inputs that should not be
        List<String> inputs = Arrays.asList(
                "(╯°□°)╯︵ ┻━┻",
                "┬─┬ノ( º _ ºノ)",
                "*hello",
                "[click:run_command:/seed]/seed",
                "[hover:show_text:text]text",
                "<click:run_command:/seed>/seed",
                "<hover:show_text:text>text"
        );

        for (String entry : inputs) {
            Component component = DiscordToMinecraftChatModule.convertToComponent(MockDiscordSRV.getInstance(), message(entry, 0), config);
            String plain = PlainTextComponentSerializer.plainText().serialize(component);

            Assertions.assertEquals(entry, plain);
        }
    }

    @Test
    public void testEventsNotConverted() {
        BaseChannelConfig config = new BaseChannelConfig();

        List<String> inputs = Arrays.asList(
                "[click:run_command:/seed]/seed",
                "[hover:show_text:text]text",
                "<click:run_command:/seed>/seed",
                "<hover:show_text:text>text"
        );

        for (String input : inputs) {
            Component component = DiscordToMinecraftChatModule.convertToComponent(MockDiscordSRV.getInstance(), message(input, 0), config);
            Assertions.assertFalse(anyComponentHas(component, componentToTest -> componentToTest.clickEvent() != null || componentToTest.hoverEvent() != null));
        }
    }

    private boolean anyComponentHas(Component component, Predicate<Component> componentPredicate) {
        for (Component child : component.children()) {
            if (anyComponentHas(child, componentPredicate)) {
                return true;
            }
        }

        return componentPredicate.test(component);
    }

    private ReceivedDiscordMessage message(String content, long userId) {
        return new ReceivedDiscordMessage() {
            @Override
            public @Nullable String getContent() {
                return content;
            }

            @Override
            public @NotNull @Unmodifiable List<DiscordMessageEmbed> getEmbeds() {
                return Collections.emptyList();
            }

            @Override
            public boolean isWebhookMessage() {
                return false;
            }

            @Override
            public @NotNull String getJumpUrl() {
                return "";
            }

            @Override
            public @NotNull List<Attachment> getAttachments() {
                return Collections.emptyList();
            }

            @Override
            public boolean isFromSelf() {
                return false;
            }

            @Override
            public @NotNull DiscordUser getAuthor() {
                return user(userId);
            }

            @Override
            public @NotNull DiscordMessageChannel getChannel() {
                return messageChannel();
            }

            @Override
            public @Nullable ReceivedDiscordMessage getReplyingTo() {
                return null;
            }

            @Override
            public @Nullable DiscordGuildMember getMember() {
                return null;
            }

            @Override
            public @Nullable DiscordGuild getGuild() {
                return null;
            }

            @Override
            public Set<DiscordUser> getMentionedUsers() {
                return Collections.emptySet();
            }

            @Override
            public Set<DiscordGuildMember> getMentionedMembers() {
                return Collections.emptySet();
            }

            @Override
            public @NotNull OffsetDateTime getDateCreated() {
                return OffsetDateTime.now();
            }

            @Override
            public @Nullable OffsetDateTime getDateEdited() {
                return null;
            }

            @Override
            public @NotNull Task<Void> delete() {
                return taskNotImplemented();
            }

            @Override
            public Task<ReceivedDiscordMessage> edit(@NotNull SendableDiscordMessage message) {
                return taskNotImplemented();
            }

            @Override
            public Task<ReceivedDiscordMessage> reply(@NotNull SendableDiscordMessage message) {
                return taskNotImplemented();
            }

            @Override
            public long getId() {
                return 0;
            }
        };
    }

    private DiscordUser user(long userId) {
        return new DiscordUser() {
            @Override
            public @Nullable DiscordUserPrimaryGuild getPrimaryGuild() {
                return null;
            }

            @Override
            public boolean isSelf() {
                return false;
            }

            @Override
            public boolean isBot() {
                return false;
            }

            @Override
            public @NotNull String getUsername() {
                return "";
            }

            @Override
            public @NotNull String getEffectiveName() {
                return "";
            }

            @Override
            public @NotNull String getDiscriminator() {
                return "";
            }

            @Override
            public @Nullable String getAvatarUrl() {
                return null;
            }

            @Override
            public @NotNull String getEffectiveAvatarUrl() {
                return "";
            }

            @Override
            public Task<DiscordDMChannel> openPrivateChannel() {
                return taskNotImplemented();
            }

            @Override
            public User asJDA() {
                return null;
            }

            @Override
            public CharSequence getAsMention() {
                return null;
            }

            @Override
            public long getId() {
                return userId;
            }
        };
    }

    private DiscordMessageChannel messageChannel() {
        return new DiscordMessageChannel() {
            @Override
            public @NotNull Task<ReceivedDiscordMessage> sendMessage(@NotNull SendableDiscordMessage message) {
                return taskNotImplemented();
            }

            @Override
            public Task<Void> deleteMessageById(long id, boolean webhookMessage) {
                return taskNotImplemented();
            }

            @Override
            public @NotNull Task<ReceivedDiscordMessage> editMessageById(long id, @NotNull SendableDiscordMessage message) {
                return taskNotImplemented();
            }

            @Override
            public MessageChannel getAsJDAMessageChannel() {
                return null;
            }

            @Override
            public DiscordChannelType getType() {
                return DiscordChannelType.TEXT;
            }

            @Override
            public long getId() {
                return 0;
            }
        };
    }

    private <T> Task<T> taskNotImplemented() {
        return Task.failed(new NotImplementedException());
    }
}
