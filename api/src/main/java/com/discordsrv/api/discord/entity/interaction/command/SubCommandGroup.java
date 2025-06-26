/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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
import net.dv8tion.jda.api.interactions.DiscordLocale;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

public class SubCommandGroup implements JDAEntity<SubcommandGroupData> {

    @NotNull
    public static SubCommandGroup.Builder builder(@NotNull String name, @NotNull String description) {
        return new SubCommandGroup.Builder(name, description);
    }

    private final Map<Locale, String> nameTranslations;
    private final Map<Locale, String> descriptionTranslations;
    private final List<DiscordCommand> commands;

    private SubCommandGroup(
            Map<Locale, String> nameTranslations,
            Map<Locale, String> descriptionTranslations,
            List<DiscordCommand> commands
    ) {
        this.nameTranslations = nameTranslations;
        this.descriptionTranslations = descriptionTranslations;
        this.commands = commands;
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

    @NotNull
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
    public List<DiscordCommand> getCommands() {
        return commands;
    }

    @Override
    public SubcommandGroupData asJDA() {
        SubcommandGroupData data = new SubcommandGroupData(getName(), getDescription())
                .addSubcommands(getCommands().stream().map(DiscordCommand::asJDASubcommand).toArray(SubcommandData[]::new));

        for (Map.Entry<Locale, String> entry : getNameTranslations().entrySet()) {
            DiscordLocale locale = DiscordCommand.getJDALocale(entry.getKey());
            if (locale != null) {
                data.setNameLocalization(locale, entry.getValue());
            }
        }
        for (Map.Entry<Locale, String> entry : getDescriptionTranslations().entrySet()) {
            DiscordLocale locale = DiscordCommand.getJDALocale(entry.getKey());
            if (locale != null) {
                data.setDescriptionLocalization(locale, entry.getValue());
            }
        }

        return data;
    }


    public static class Builder {

        protected final Map<Locale, String> nameTranslations = new LinkedHashMap<>();
        protected final Map<Locale, String> descriptionTranslations = new LinkedHashMap<>();
        protected final List<DiscordCommand> commands = new ArrayList<>();

        private Builder(String name, String description) {
            this.nameTranslations.put(Locale.ROOT, name);
            this.descriptionTranslations.put(Locale.ROOT, description);
        }

        public SubCommandGroup.Builder addCommand(@NotNull DiscordCommand command) {
            this.commands.add(command);
            return this;
        }

        public void setDescription(String description) {
            this.descriptionTranslations.put(Locale.ROOT, description);
        }

        /**
         * Adds a name translation for this command option.
         * @param locale the language
         * @param translation the translation
         * @return this builder, useful for chaining
         */
        @NotNull
        public SubCommandGroup.Builder addNameTranslation(@NotNull Locale locale, @NotNull String translation) {
            this.nameTranslations.put(locale, translation);
            return this;
        }

        public SubCommandGroup.Builder addNameTranslations(@NotNull Map<Locale, String> translations) {
            translations.forEach(this::addNameTranslation);
            return this;
        }

        /**
         * Adds a description translation for this subcommand group.
         * @param locale the language
         * @param translation the translation
         * @return this builder, useful for chaining
         */
        @NotNull
        public SubCommandGroup.Builder addDescriptionTranslation(@NotNull Locale locale, @NotNull String translation) {
            this.descriptionTranslations.put(locale, translation);
            return this;
        }

        public SubCommandGroup.Builder addDescriptionTranslations(@NotNull Map<Locale, String> translations) {
            translations.forEach(this::addDescriptionTranslation);
            return this;
        }

        @NotNull
        public SubCommandGroup build() {
            return new SubCommandGroup(nameTranslations, descriptionTranslations, commands);
        }
    }
}
