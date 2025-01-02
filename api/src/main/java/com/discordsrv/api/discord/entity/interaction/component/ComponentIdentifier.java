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

package com.discordsrv.api.discord.entity.interaction.component;

import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * An identifier for commands and components to match up with interaction events, and to avoid conflicts between extensions.
 */
public class ComponentIdentifier {

    private static final String ID_PREFIX = "DiscordSRV/";
    private static final char PART_SEPARATOR = ':';

    private static final String REGEX = "[\\w\\d-_]{1,40}";
    private static final Pattern PATTERN = java.util.regex.Pattern.compile(REGEX);

    /**
     * Creates a new {@link ComponentIdentifier}.
     *
     * @param extensionName the name of the plugin or mod that owns this identifier (1-40 characters, a-z, A-Z, 0-9, -, _)
     * @param identifier the identifier of this component (1-40 characters, a-z, A-Z, 0-9, -, _)
     * @return a new {@link ComponentIdentifier}
     * @throws IllegalArgumentException if the extension name or identifier does not match the required constraints
     */
    @NotNull
    public static ComponentIdentifier of(
            @NotNull @org.intellij.lang.annotations.Pattern(REGEX) String extensionName,
            @NotNull @org.intellij.lang.annotations.Pattern(REGEX) String identifier
    ) {
        if (!PATTERN.matcher(extensionName).matches()) {
            throw new IllegalArgumentException("Extension name does not match the required pattern");
        } else if (!PATTERN.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Identifier does not match the required pattern");
        }
        return new ComponentIdentifier(extensionName, identifier);
    }

    @Nullable
    public static ComponentIdentifier parseFromDiscord(@NotNull String discordIdentifier) {
        if (!discordIdentifier.startsWith(ID_PREFIX)) {
            return null;
        }
        discordIdentifier = discordIdentifier.substring(ID_PREFIX.length());

        @Subst("Example:Test")
        String[] parts = discordIdentifier.split(Pattern.quote(ID_PREFIX));
        if (parts.length != 2) {
            return null;
        }

        try {
            return of(parts[0], parts[1]);
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    private final String extensionName;
    private final String identifier;

    private ComponentIdentifier(String extensionName, String identifier) {
        this.extensionName = extensionName;
        this.identifier = identifier;
    }

    public String getExtensionName() {
        return extensionName;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getDiscordIdentifier() {
        return ID_PREFIX + getExtensionName() + PART_SEPARATOR + getIdentifier();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ComponentIdentifier that = (ComponentIdentifier) o;
        return Objects.equals(extensionName, that.extensionName) && Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(extensionName, identifier);
    }

    @Override
    public String toString() {
        return "ComponentIdentifier{" +
                "extensionName='" + extensionName + '\'' +
                ", identifier='" + identifier + '\'' +
                '}';
    }
}
