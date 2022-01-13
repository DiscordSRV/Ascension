/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2022 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.integration;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.module.type.PermissionDataProvider;
import com.discordsrv.common.module.type.PluginIntegration;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;

import java.util.Collection;
import java.util.UUID;

public class LuckPermsIntegration extends PluginIntegration implements PermissionDataProvider {

    private LuckPerms luckPerms;

    public LuckPermsIntegration(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public boolean isEnabled() {
        try {
            Class.forName("net.luckperms.api.LuckPerms");
        } catch (ClassNotFoundException e) {
            return false;
        }

        return true;
    }

    @Override
    public void enable() {
        luckPerms = LuckPermsProvider.get();
    }

    @Override
    public void disable() {
        luckPerms = null;
    }

    @Override
    public boolean hasGroup(UUID player, String groupName) {
        User user = luckPerms.getUserManager().getUser(player);
        if (user == null) {
            return false;
        }

        Collection<Group> groups = user.getInheritedGroups(QueryOptions.defaultContextualOptions());
        return groups.stream().anyMatch(group -> group.getName().equalsIgnoreCase(groupName));
    }

    @Override
    public void addGroup(UUID player, String groupName) {

    }

    @Override
    public void removeGroup(UUID player, String groupName) {

    }
}
