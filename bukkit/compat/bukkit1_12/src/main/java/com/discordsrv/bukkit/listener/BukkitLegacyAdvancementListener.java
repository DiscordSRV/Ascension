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

package com.discordsrv.bukkit.listener;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.message.receive.game.AwardMessageReceiveEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.common.abstraction.player.IPlayer;
import com.discordsrv.common.core.logging.NamedLogger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.advancement.Advancement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Arrays;

public class BukkitLegacyAdvancementListener extends AbstractBukkitListener<PlayerAdvancementDoneEvent> {

    private final NMS nms;

    public BukkitLegacyAdvancementListener(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "ADVANCEMENT_LISTENER"));

        Server server = Bukkit.getServer();
        String version = server.getBukkitVersion().split("-", 2)[0];

        NMS nms = null;
        try {
            String nmsVersion = server.getClass().getName().split("\\.")[3];
            if ((version.startsWith("1.19") && !version.matches("1.19.[1-3].*"))
                    || version.startsWith("1.2")) {
                // 1.19.4+
                nms = new NMS("org.bukkit.craftbukkit." + nmsVersion + ".advancement.CraftAdvancement",
                              "d", "i", "a");
            } else {
                // <1.19.4
                nms = new NMS("org.bukkit.craftbukkit." + nmsVersion + ".advancement.CraftAdvancement",
                              "c", "i", "a");
            }
        } catch (Throwable t) {
            logger().error("Could not get NMS methods for advancements.");
            logger().debug(t);
        }
        this.nms = nms;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        handleEventWithErrorHandling(event);
    }

    @Override
    protected void handleEvent(@NotNull PlayerAdvancementDoneEvent event, Void __) {
        if (nms == null) {
            return;
        }

        String gameRuleValue = event.getPlayer().getWorld().getGameRuleValue("announceAdvancements");
        if ("false".equals(gameRuleValue)) {
            logger().trace("Skipping forwarding advancement, disabled by gamerule");
            return;
        }

        try {
            ReturnData data = nms.getData(event.getAdvancement());
            if (data == null) {
                return;
            }

            MinecraftComponent title = MinecraftComponent.fromJson(data.titleJson);
            IPlayer srvPlayer = discordSRV.playerProvider().player(event.getPlayer());
            discordSRV.eventBus().publish(
                    new AwardMessageReceiveEvent(
                            event,
                            srvPlayer,
                            title,
                            null,
                            null,
                            null,
                            null,
                            false
                    )
            );
        } catch (ReflectiveOperationException e) {
            logger().debug("Failed to get advancement data", e);
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

    // Event is not cancellable
    @Override
    protected void observeEvents(boolean enable) {}
}
