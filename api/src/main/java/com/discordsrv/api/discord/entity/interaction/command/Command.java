/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.discordsrv.api.discord.entity.interaction.command;

import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.events.interaction.command.*;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * A Discord command.
 */
public class Command implements JDAEntity<CommandData> {

    private static final String CHAT_INPUT_NAME_REGEX = "(?U)[\\w-]{1,32}";
    public static final Pattern CHAT_INPUT_NAME_PATTERN = Pattern.compile(CHAT_INPUT_NAME_REGEX);

    /**
     * Creates a chat input or slash command builder.
     *
     * @param id a unique identifier for this interaction, used to check if a given event was for this interaction
     * @param name the name of the command, 1-32 characters alphanumeric and dashes
     * @param description the description of the command
     * @return a new chat input command builder
     * @see com.discordsrv.api.discord.events.interaction.command.DiscordChatInputInteractionEvent
     */
    public static ChatInputBuilder chatInput(
            ComponentIdentifier id,
            @org.intellij.lang.annotations.Pattern(CHAT_INPUT_NAME_REGEX) String name,
            String description
    ) {
        if (!CHAT_INPUT_NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Name must be alphanumeric (dashes allowed), 1 and 32 characters");
        }
        return new ChatInputBuilder(id, name, description);
    }

    /**
     * Creates a new user context menu command.
     *
     * @param id a unique identifier for this interaction, used to check if a given event was for this interaction
     * @param name the name of the command, 1-32 characters
     * @return a new command builder
     * @see com.discordsrv.api.discord.events.interaction.command.DiscordUserContextInteractionEvent
     */
    public static Builder<DiscordUserContextInteractionEvent> user(
            ComponentIdentifier id,
            @org.intellij.lang.annotations.Pattern(".{1,32}") String name
    ) {
        return new Builder<>(id, CommandType.USER, name);
    }

    /**
     * Creates a new message context menu command.
     *
     * @param id a unique identifier for this interaction, used to check if a given event was for this interaction
     * @param name the name of the command, 1-32 characters
     * @return a new command builder
     * @see com.discordsrv.api.discord.events.interaction.command.DiscordMessageContextInteractionEvent
     */
    public static Builder<DiscordMessageContextInteractionEvent> message(
            ComponentIdentifier id,
            @org.intellij.lang.annotations.Pattern(".{1,32}") String name
    ) {
        return new Builder<>(id, CommandType.MESSAGE, name);
    }

    private final ComponentIdentifier id;
    private final CommandType type;
    private final Map<Locale, String> nameTranslations;
    private final Map<Locale, String> descriptionTranslations;
    private final List<SubCommandGroup> subCommandGroups;
    private final List<Command> subCommands;
    private final List<CommandOption> options;
    private final Long guildId;
    private final boolean guildOnly;
    private final DefaultPermission defaultPermission;
    private final Consumer<? extends AbstractCommandInteractionEvent<?>> eventHandler;
    private final Consumer<DiscordCommandAutoCompleteInteractionEvent> autoCompleteHandler;

    private Command(
            ComponentIdentifier id,
            CommandType type,
            Map<Locale, String> nameTranslations,
            Map<Locale, String> descriptionTranslations,
            List<SubCommandGroup> subCommandGroups,
            List<Command> subCommands,
            List<CommandOption> options,
            Long guildId,
            boolean guildOnly,
            DefaultPermission defaultPermission,
            Consumer<? extends AbstractCommandInteractionEvent<?>> eventHandler,
            Consumer<DiscordCommandAutoCompleteInteractionEvent> autoCompleteHandler
    ) {
        this.id = id;
        this.type = type;
        this.nameTranslations = nameTranslations;
        this.descriptionTranslations = descriptionTranslations;
        this.subCommandGroups = subCommandGroups;
        this.subCommands = subCommands;
        this.options = options;
        this.guildId = guildId;
        this.guildOnly = guildOnly;
        this.defaultPermission = defaultPermission;
        this.eventHandler = eventHandler;
        this.autoCompleteHandler = autoCompleteHandler;
    }

    @NotNull
    public ComponentIdentifier getId() {
        return id;
    }

    @Nullable
    public Long getGuildId() {
        return guildId;
    }

    @NotNull
    public CommandType getType() {
        return type;
    }

    @NotNull
    public String getName() {
        return nameTranslations.get(Locale.ROOT);
    }

    @NotNull
    @Unmodifiable
    public Map<Locale, String> getNameTranslations() {
        return Collections.unmodifiableMap(nameTranslations);
    }

    @Nullable
    public String getDescription() {
        return descriptionTranslations.get(Locale.ROOT);
    }

    @NotNull
    @Unmodifiable
    public Map<Locale, String> getDescriptionTranslations() {
        return Collections.unmodifiableMap(descriptionTranslations);
    }

    @NotNull
    @Unmodifiable
    public List<SubCommandGroup> getSubCommandGroups() {
        return Collections.unmodifiableList(subCommandGroups);
    }

    @NotNull
    @Unmodifiable
    public List<Command> getSubCommands() {
        return Collections.unmodifiableList(subCommands);
    }

    @NotNull
    @Unmodifiable
    public List<CommandOption> getOptions() {
        return Collections.unmodifiableList(options);
    }

    public boolean isGuildOnly() {
        return guildOnly;
    }

    @NotNull
    public DefaultPermission getDefaultPermission() {
        return defaultPermission;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends AbstractCommandInteractionEvent<?>> Consumer<T> getEventHandler() {
        if (eventHandler == null) {
            return null;
        }

        return (Consumer<T>) eventHandler;
    }

    @Nullable
    public Consumer<DiscordCommandAutoCompleteInteractionEvent> getAutoCompleteHandler() {
        return autoCompleteHandler;
    }

    @Override
    public CommandData asJDA() {
        CommandData commandData;
        switch (type) {
            case USER:
                commandData = Commands.user(getName());
                break;
            case MESSAGE:
                commandData = Commands.message(getName());
                break;
            case CHAT_INPUT:
                SlashCommandData slashCommandData = Commands.slash(getName(), Objects.requireNonNull(getDescription()));
                slashCommandData.addSubcommandGroups(subCommandGroups.stream().map(JDAEntity::asJDA).toArray(SubcommandGroupData[]::new));
                slashCommandData.addSubcommands(subCommands.stream().map(Command::asJDASubcommand).toArray(SubcommandData[]::new));
                slashCommandData.addOptions(options.stream().map(JDAEntity::asJDA).toArray(OptionData[]::new));
                commandData = slashCommandData;
                break;
            default:
                throw new IllegalStateException("Missing switch case");
        }

        commandData.setGuildOnly(guildOnly);
        commandData.setDefaultPermissions(defaultPermission.asJDA());

        return commandData;
    }

    public SubcommandData asJDASubcommand() {
        SubcommandData data = new SubcommandData(nameTranslations.get(Locale.ROOT), descriptionTranslations.get(Locale.ROOT));
        data.addOptions(options.stream().map(JDAEntity::asJDA).toArray(OptionData[]::new));
        return data;
    }

    public static class ChatInputBuilder extends Builder<DiscordChatInputInteractionEvent> {

        private final Map<Locale, String> descriptionTranslations = new LinkedHashMap<>();
        private final List<SubCommandGroup> subCommandGroups = new ArrayList<>();
        private final List<Command> subCommands = new ArrayList<>();
        private final List<CommandOption> options = new ArrayList<>();
        private Consumer<DiscordCommandAutoCompleteInteractionEvent> autoCompleteHandler;

        private ChatInputBuilder(ComponentIdentifier id, String name, String description) {
            super(id, CommandType.CHAT_INPUT, name);
            this.descriptionTranslations.put(Locale.ROOT, description);
        }

        /**
         * Adds a description translation for this command.
         * @param locale the language
         * @param translation the translation
         * @return this builder, useful for chaining
         * @throws IllegalStateException if this isn't a {@link CommandType#CHAT_INPUT} command
         */
        @NotNull
        public ChatInputBuilder addDescriptionTranslation(@NotNull Locale locale, @NotNull String translation) {
            if (type != CommandType.CHAT_INPUT) {
                throw new IllegalStateException("Descriptions are only available for CHAT_INPUT commands");
            }
            this.descriptionTranslations.put(locale, translation);
            return this;
        }

        /**
         * Adds a sub command group to this command.
         *
         * @param subCommandGroup the sub command group
         * @return this builder, useful for chaining
         */
        @NotNull
        public ChatInputBuilder addSubCommandGroup(@NotNull SubCommandGroup subCommandGroup) {
            this.subCommandGroups.add(subCommandGroup);
            return this;
        }

        /**
         * Adds a sub command to this command.
         *
         * @param command the sub command
         * @return this builder, useful for chaining
         */
        @NotNull
        public ChatInputBuilder addSubCommand(@NotNull Command command) {
            this.subCommands.add(command);
            return this;
        }

        /**
         * Adds an option to this command.
         *
         * @param option the option
         * @return this builder, useful for chaining
         */
        @NotNull
        public ChatInputBuilder addOption(@NotNull CommandOption option) {
            this.options.add(option);
            return this;
        }

        /**
         * Sets the auto complete handler for this command, this can be used instead of listening to the {@link DiscordCommandAutoCompleteInteractionEvent}.
         * @param autoCompleteHandler the auto complete handler, only receives events for this command
         * @return this builder, useful for chaining
         */
        @NotNull
        public ChatInputBuilder setAutoCompleteHandler(Consumer<DiscordCommandAutoCompleteInteractionEvent> autoCompleteHandler) {
            this.autoCompleteHandler = autoCompleteHandler;
            return this;
        }

        @Override
        public Command build() {
            return new Command(
                    id,
                    type,
                    nameTranslations,
                    descriptionTranslations,
                    subCommandGroups,
                    subCommands,
                    options,
                    guildId,
                    guildOnly,
                    defaultPermission,
                    eventHandler,
                    autoCompleteHandler
            );
        }
    }

    public static class Builder<E extends AbstractCommandInteractionEvent<?>> {

        protected final ComponentIdentifier id;
        protected final CommandType type;
        protected final Map<Locale, String> nameTranslations = new LinkedHashMap<>();
        protected Long guildId = null;
        protected boolean guildOnly = true;
        protected DefaultPermission defaultPermission = DefaultPermission.EVERYONE;
        protected Consumer<? extends AbstractCommandInteractionEvent<?>> eventHandler;

        private Builder(ComponentIdentifier id, CommandType type, String name) {
            this.id = id;
            this.type = type;
            this.nameTranslations.put(Locale.ROOT, name);
        }

        /**
         * Adds a name translation for this command.
         * @param locale the language
         * @param translation the translation
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder<E> addNameTranslation(@NotNull Locale locale, @NotNull String translation) {
            this.nameTranslations.put(locale, translation);
            return this;
        }

        /**
         * Sets the id of the guild this command should be registered to, or {@code null} to register the command globally.
         * @param guildId the guild id
         * @return this builder, useful for chaining
         */
        public Builder<E> setGuildId(Long guildId) {
            this.guildId = guildId;
            return this;
        }

        /**
         * Sets if this command is limited to Discord servers.
         * @param guildOnly if this command is limited to Discord servers
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder<E> setGuildOnly(boolean guildOnly) {
            this.guildOnly = guildOnly;
            return this;
        }

        /**
         * Sets the permission level required to use the command by default.
         * @param defaultPermission the permission level
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder<E> setDefaultPermission(@NotNull DefaultPermission defaultPermission) {
            this.defaultPermission = defaultPermission;
            return this;
        }

        /**
         * Sets the event handler for this command, this can be used instead of listening to the specific interaction event.
         * @param eventHandler the event handler, only receives events for this command
         * @return this builder, useful for chaining
         */
        @NotNull
        public Builder<E> setEventHandler(Consumer<E> eventHandler) {
            this.eventHandler = eventHandler;
            return this;
        }

        public Command build() {
            return new Command(
                    id,
                    type,
                    nameTranslations,
                    Collections.emptyMap(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    guildId,
                    guildOnly,
                    defaultPermission,
                    eventHandler,
                    null
            );
        }
    }

    public interface DefaultPermission extends JDAEntity<DefaultMemberPermissions> {

        DefaultPermission EVERYONE = new Simple(true);
        DefaultPermission ADMINISTRATOR = new Simple(false);

        DefaultPermission BAN_MEMBERS = Permissions.fromJDA(Permission.BAN_MEMBERS);
        DefaultPermission MODERATE_MEMBERS = Permissions.fromJDA(Permission.MODERATE_MEMBERS);
        DefaultPermission MANAGE_PERMISSIONS = Permissions.fromJDA(Permission.MANAGE_PERMISSIONS);
        DefaultPermission MESSAGE_MANAGE = Permissions.fromJDA(Permission.MESSAGE_MANAGE);

        class Simple implements DefaultPermission {

            private final boolean value;

            private Simple(boolean value) {
                this.value = value;
            }

            @Override
            public DefaultMemberPermissions asJDA() {
                return value ? DefaultMemberPermissions.ENABLED : DefaultMemberPermissions.DISABLED;
            }
        }

        class Permissions implements DefaultPermission {

            public static Permissions fromJDA(Permission... permissions) {
                return new Permissions(Permission.getRaw(permissions));
            }

            private final long permissions;

            public Permissions(long permissions) {
                this.permissions = permissions;
            }

            @Override
            public DefaultMemberPermissions asJDA() {
                return DefaultMemberPermissions.enabledFor(permissions);
            }
        }
    }

    public enum RegistrationResult {

        /**
         * Indicates that the command was successfully added to the registry, and will be registered.
         */
        REGISTERED,

        /**
         * Command is already registered, and was ignored.
         */
        ALREADY_REGISTERED,

        /**
         * There was already a command with the same name,
         * therefor the command won't be registered unless other commands with the same name are unregistered.
         */
        NAME_ALREADY_IN_USE,

        /**
         * There are too many commands of the same type,
         * therefore the command won't be registered unless other commands of the same type are unregistered.
         */
        TOO_MANY_COMMANDS

    }
}
