/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.bukkit.listener.award;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Used for Spigot and Paper in versions before they added advancement apis.
 */
public class BukkitLegacyAdvancementListener extends AbstractBukkitAwardListener {

    private final NMS nms;

    public BukkitLegacyAdvancementListener(DiscordSRV discordSRV, IBukkitAwardForwarder forwarder) {
        super(discordSRV, forwarder);

        String version = Bukkit.getServer().getBukkitVersion().split("-", 2)[0];

        NMS nms = null;
        try {
            if ((version.startsWith("1.19") && !version.matches("1.19.[1-3].*"))
                    || version.startsWith("1.2")) {
                // 1.19.4+
                nms = new NMS("org.bukkit.craftbukkit." + version + ".advancement.CraftAdvancement",
                                   "d", "i", "a");
            } else {
                // <1.19.4
                nms = new NMS("org.bukkit.craftbukkit." + version + ".advancement.CraftAdvancement",
                                   "c", "i", "a");
            }
        } catch (Throwable t) {
            logger.error("Could not get NMS methods for advancements.");
            logger.debug(t);
        }
        this.nms = nms;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        if (nms == null) {
            return;
        }

        try {
            ReturnData data = nms.getData(event.getAdvancement());
            if (data == null) {
                return;
            }

            forwarder.publishEvent(
                    event,
                    event.getPlayer(),
                    data.titleJson != null ? ComponentUtil.toAPI(BukkitComponentSerializer.gson().deserialize(data.titleJson)) : null,
                    null,
                    false);
        } catch (ReflectiveOperationException e) {
            logger.debug("Failed to get advancement data", e);
        }
    }

    private static class NMS {

        private final Method handleMethod;
        private final Method advancementDisplayMethod;
        private final Method broadcastToChatMethod;
        private final Method titleMethod;
        private final Method toJsonMethod;

        public NMS(
                String craftAdvancementClassName,
                String displayMethodName,
                String broadcastToChatMethodName,
                String titleMethodName
        ) throws ReflectiveOperationException {
            Class<?> clazz = Class.forName(craftAdvancementClassName);
            handleMethod = clazz.getDeclaredMethod("getHandle");
            Class<?> nmsClass = handleMethod.getReturnType();
            advancementDisplayMethod = nmsClass.getDeclaredMethod(displayMethodName);
            Class<?> displayClass = advancementDisplayMethod.getReturnType();
            broadcastToChatMethod = displayClass.getDeclaredMethod(broadcastToChatMethodName);
            titleMethod = displayClass.getDeclaredMethod(titleMethodName);

            Class<?> serializer = Class.forName(titleMethod.getReturnType().getName() + "$ChatSerializer");
            toJsonMethod = Arrays.stream(serializer.getDeclaredMethods())
                    .filter(method -> method.getReturnType().equals(String.class))
                    .findAny().orElseThrow(() -> new NoSuchMethodException("ChatSerializer toJson"));
        }

        public ReturnData getData(Advancement advancement) throws ReflectiveOperationException {
            Object nms = handleMethod.invoke(advancement);
            Object display = advancementDisplayMethod.invoke(nms);
            if (display == null) {
                // Not something that would be displayed in chat
                return null;
            }

            boolean broadcastToChat = (boolean) broadcastToChatMethod.invoke(display);
            if (!broadcastToChat) {
                // Not something that would be displayed in chat
                return null;
            }

            Object titleChat = titleMethod.invoke(display);
            return new ReturnData(
                    toJson(titleChat)
            );
        }

        private String toJson(Object chat) throws ReflectiveOperationException {
            return (String) toJsonMethod.invoke(null, chat);
        }
    }

    private static class ReturnData {

        private final String titleJson;

        public ReturnData(String titleJson) {
            this.titleJson = titleJson;
        }
    }
}
