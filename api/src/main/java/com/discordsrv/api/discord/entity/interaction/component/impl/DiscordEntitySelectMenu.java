/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.discord.entity.interaction.component.impl;

import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.JDAEntity;
import com.discordsrv.api.discord.entity.channel.DiscordChannel;
import com.discordsrv.api.discord.entity.channel.DiscordChannelType;
import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.discord.entity.interaction.component.ComponentIdentifier;
import com.discordsrv.api.discord.entity.interaction.component.component.ActionRowComponent;
import net.dv8tion.jda.api.components.selections.EntitySelectMenu;
import net.dv8tion.jda.internal.utils.tuple.Pair;
import org.jetbrains.annotations.CheckReturnValue;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DiscordEntitySelectMenu implements ActionRowComponent<EntitySelectMenu> {

    public static Builder builder(ComponentIdentifier identifier, Target... targets) {
        if (targets.length == 0) {
            throw new IllegalArgumentException("Must have at least one target");
        }
        return new Builder(identifier, Arrays.asList(targets));
    }

    private final ComponentIdentifier identifier;
    private final List<Target> target;
    private final boolean disabled;
    private final List<DiscordChannelType> channelTypes;
    private final List<Pair<Target, Long>> defaultValues;

    private DiscordEntitySelectMenu(
            ComponentIdentifier identifier,
            List<Target> target,
            boolean disabled,
            List<DiscordChannelType> channelTypes,
            List<Pair<Target, Long>> defaultValues
    ) {
        this.identifier = identifier;
        this.target = Collections.unmodifiableList(target);
        this.disabled = disabled;
        this.channelTypes = Collections.unmodifiableList(channelTypes);
        this.defaultValues = Collections.unmodifiableList(defaultValues);
    }

    public ComponentIdentifier getIdentifier() {
        return identifier;
    }

    @Unmodifiable
    public List<Target> getType() {
        return target;
    }

    public boolean isDisabled() {
        return disabled;
    }

    @Unmodifiable
    public List<DiscordChannelType> getChannelTypes() {
        return channelTypes;
    }

    @Unmodifiable
    public List<Pair<Target, Long>> getDefaultValues() {
        return defaultValues;
    }

    @Override
    public EntitySelectMenu asJDA() {
        return EntitySelectMenu.create(
                identifier.getDiscordIdentifier(),
                target.stream().map(JDAEntity::asJDA).collect(Collectors.toList())
        )
                .setDisabled(disabled)
                .setChannelTypes(channelTypes.stream().map(JDAEntity::asJDA).collect(Collectors.toList()))
                .setDefaultValues(defaultValues.stream().map(defaultValue -> {
                    long id = defaultValue.getRight();
                    switch (defaultValue.getLeft()) {
                        case USER: return EntitySelectMenu.DefaultValue.user(id);
                        case ROLE: return EntitySelectMenu.DefaultValue.role(id);
                        case CHANNEL: return EntitySelectMenu.DefaultValue.channel(id);
                        default: return null;
                    }
                }).collect(Collectors.toList()))
                .build();
    }

    public enum Target implements JDAEntity<EntitySelectMenu.SelectTarget> {
        USER(EntitySelectMenu.SelectTarget.USER),
        ROLE(EntitySelectMenu.SelectTarget.ROLE),
        CHANNEL(EntitySelectMenu.SelectTarget.ROLE);

        private final EntitySelectMenu.SelectTarget target;

        Target(EntitySelectMenu.SelectTarget target) {
            this.target = target;
        }

        @Override
        public EntitySelectMenu.SelectTarget asJDA() {
            return target;
        }
    }

    @CheckReturnValue
    public static class Builder {

        private final ComponentIdentifier identifier;
        private final List<Target> target;
        private boolean disabled = false;
        private final List<DiscordChannelType> channelTypes = new ArrayList<>();
        private final List<Pair<Target, Long>> defaultValues = new ArrayList<>();

        private Builder(ComponentIdentifier identifier, List<Target> target) {
            this.identifier = identifier;
            this.target = target;
        }

        public Builder setDisabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public Builder addChannelTypes(DiscordChannelType... channelTypes) {
            this.channelTypes.addAll(Arrays.asList(channelTypes));
            return this;
        }

        public Builder addDefaultUsers(DiscordUser... users) {
            for (DiscordUser user : users) {
                defaultValues.add(Pair.of(Target.USER, user.getId()));
            }
            return this;
        }

        public Builder addDefaultChannels(DiscordChannel... channels) {
            for (DiscordChannel channel : channels) {
                defaultValues.add(Pair.of(Target.CHANNEL, channel.getId()));
            }
            return this;

        }

        public Builder addDefaultRoles(DiscordRole... roles) {
            for (DiscordRole role : roles) {
                defaultValues.add(Pair.of(Target.ROLE, role.getId()));
            }
            return this;
        }

        public DiscordEntitySelectMenu build() {
            return new DiscordEntitySelectMenu(identifier, target, disabled, channelTypes, defaultValues);
        }
    }
}
