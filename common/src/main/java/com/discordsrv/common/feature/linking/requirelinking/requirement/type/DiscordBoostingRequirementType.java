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

import com.discordsrv.api.discord.entity.guild.DiscordGuild;
import com.discordsrv.api.eventbus.Subscribe;
import com.discordsrv.api.task.Task;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.feature.linking.requirelinking.RequiredLinkingModule;
import com.discordsrv.common.helper.Someone;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateBoostTimeEvent;

public class DiscordBoostingRequirementType extends LongRequirementType {

    public DiscordBoostingRequirementType(RequiredLinkingModule<? extends DiscordSRV> module) {
        super(module);
    }

    @Override
    public String name() {
        return "DiscordBoosting";
    }

    @Override
    public Task<Boolean> isMet(Long value, Someone.Resolved someone) {
        DiscordGuild guild = module.discordSRV().discordAPI().getGuildById(value);
        if (guild == null) {
            return Task.completed(false);
        }

        return guild.retrieveMemberById(someone.userId())
                .thenApply(member -> member != null && member.isBoosting());
    }

    @Subscribe
    public void onGuildMemberUpdateBoostTime(GuildMemberUpdateBoostTimeEvent event) {
        stateChanged(Someone.of(discordSRV, event.getMember().getIdLong()), null, event.getNewTimeBoosted() != null);
    }
}
