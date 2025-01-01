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

package com.discordsrv.common.feature.linking.requirelinking.requirement.type;

import com.discordsrv.api.discord.entity.guild.DiscordRole;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.events.discord.member.role.AbstractDiscordMemberRoleChangeEvent;
import com.discordsrv.api.events.discord.member.role.DiscordMemberRoleAddEvent;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.feature.linking.requirelinking.RequiredLinkingModule;
import com.discordsrv.common.helper.Someone;

import java.util.concurrent.CompletableFuture;

public class DiscordRoleRequirementType extends LongRequirementType {

    public DiscordRoleRequirementType(RequiredLinkingModule<? extends DiscordSRV> module) {
        super(module);
    }

    @Override
    public String name() {
        return "DiscordRole";
    }

    @Override
    public CompletableFuture<Boolean> isMet(Long value, Someone.Resolved someone) {
        DiscordRole role = module.discordSRV().discordAPI().getRoleById(value);
        if (role == null) {
            return CompletableFuture.completedFuture(false);
        }

        return role.getGuild()
                .retrieveMemberById(someone.userId())
                .thenApply(member -> member.getRoles().contains(role));
    }

    @Subscribe
    public void onDiscordMemberRoleChange(AbstractDiscordMemberRoleChangeEvent<?> event) {
        boolean add = event instanceof DiscordMemberRoleAddEvent;

        Someone someone = Someone.of(event.getMember().getUser().getId());
        for (DiscordRole role : event.getRoles()) {
            stateChanged(someone, role.getId(), add);
        }
    }
}
