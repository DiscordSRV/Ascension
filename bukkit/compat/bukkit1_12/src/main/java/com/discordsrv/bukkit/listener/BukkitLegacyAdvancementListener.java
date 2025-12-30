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
import com.discordsrv.api.events.message.preprocess.game.AdvancementMessagePreProcessEvent;
import com.discordsrv.bukkit.BukkitDiscordSRV;
import com.discordsrv.bukkit.player.BukkitPlayer;
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
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

public class BukkitLegacyAdvancementListener extends AbstractBukkitListener<PlayerAdvancementDoneEvent> {

    private final NMS nms;

    public BukkitLegacyAdvancementListener(BukkitDiscordSRV discordSRV) {
        super(discordSRV, new NamedLogger(discordSRV, "ADVANCEMENT_LISTENER"));

        Server server = Bukkit.getServer();
        String version = server.getBukkitVersion().split("-", 2)[0];

        NMS nms = null;
        try {
            String nmsVersion = server.getClass().getName().split("\\.")[3];
            String cbClassName = "org.bukkit.craftbukkit." + nmsVersion + ".advancement.CraftAdvancement";
            if (version.startsWith("1.12")
                    || version.startsWith("1.13")
                    || version.startsWith("1.14")
                    || version.startsWith("1.15")
                    || version.startsWith("1.16")
                    || version.startsWith("1.17")
                    || version.startsWith("1.18")) {
                nms = new NMS(
                        cbClassName,
                        // nms Advancement
                        "c",
                        // nms AdvancementDisplay
                        "i", "a", "b", "e",
                        // nms AdvancementFrameType
                        "a"
                );
            } else if (version.startsWith("1.19")) {
                nms = new NMS(
                        cbClassName,
                        // nms Advancement
                        "d",
                        // nms AdvancementDisplay
                        "i", "a", "b", "e",
                        // nms AdvancementFrameType
                        "a"
                );
            } else {
                logger().error("Unsupported version for legacy advancements: " + version);
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

        BukkitPlayer player = discordSRV.playerProvider().player(event.getPlayer());

        Boolean gameRuleValue = player.getGameRuleValueForCurrentWorld(com.discordsrv.bukkit.gamerule.GameRule.SHOW_ADVANCEMENT_MESSAGES);
        if (Objects.equals(gameRuleValue, false)) {
            logger().trace("Skipping forwarding advancement, disabled by gamerule");
            return;
        }

        try {
            ReturnData data = nms.getData(event.getAdvancement());
            if (data == null) {
                return;
            }

            MinecraftComponent title = MinecraftComponent.fromJson(data.titleJson);
            MinecraftComponent description = MinecraftComponent.fromJson(data.descriptionJson);
            AdvancementMessagePreProcessEvent.AdvancementFrame frame =
                    data.frameId != null
                    ? AdvancementMessagePreProcessEvent.AdvancementFrame.fromId(data.frameId.toUpperCase(Locale.ROOT))
                    : null;

            discordSRV.eventBus().publish(
                    new AdvancementMessagePreProcessEvent(
                            event,
                            player,
                            null,
                            title,
                            description,
                            frame,
                            null,
                            false
                    )
            );
        } catch (ReflectiveOperationException e) {
            logger().debug("Failed to get advancement data", e);
        }
    }

    private static class NMS {

        // CraftAdvancement
        private final Method handleMethod;

        // nms Advancement
        private final Method advancementDisplayMethod;

        // nms AdvancementDisplay
        private final Method broadcastToChatMethod;
        private final Method titleMethod;
        private final Method descriptionMethod;
        private final Method frameMethod;

        // nms AdvancementFrameType
        private final Method idMethod;

        // nms IChatBaseComponent$ChatSerializer
        private final Method toJsonMethod;

        public NMS(
                String craftAdvancementClassName,
                // nms Advancement
                String displayMethodName,
                // nms AdvancementDisplay
                String broadcastToChatMethodName,
                String titleMethodName,
                String descriptionName,
                String frameMethodName,
                // nms AdvancementFrameType
                String idMethodName
        ) throws ReflectiveOperationException {
            Class<?> clazz = Class.forName(craftAdvancementClassName);
            handleMethod = clazz.getDeclaredMethod("getHandle");

            Class<?> advancementClass = handleMethod.getReturnType();
            advancementDisplayMethod = advancementClass.getDeclaredMethod(displayMethodName);

            Class<?> advancementDisplayClass = advancementDisplayMethod.getReturnType();
            broadcastToChatMethod = advancementDisplayClass.getDeclaredMethod(broadcastToChatMethodName);
            titleMethod = advancementDisplayClass.getDeclaredMethod(titleMethodName);
            descriptionMethod = advancementDisplayClass.getDeclaredMethod(descriptionName);
            frameMethod = advancementDisplayClass.getDeclaredMethod(frameMethodName);

            Class<?> advancementFrameTypeClass = frameMethod.getReturnType();
            idMethod = advancementFrameTypeClass.getDeclaredMethod(idMethodName);

            Class<?> serializer = Class.forName(titleMethod.getReturnType().getName() + "$ChatSerializer");
            toJsonMethod = Arrays.stream(serializer.getDeclaredMethods())
                    .filter(method -> method.getReturnType().equals(String.class))
                    .findAny().orElseThrow(() -> new NoSuchMethodException("ChatSerializer toJson"));
        }

        public ReturnData getData(Advancement bukkitAdvancement) throws ReflectiveOperationException {
            Object advancement = handleMethod.invoke(bukkitAdvancement);
            Object advancementDisplay = advancementDisplayMethod.invoke(advancement);
            if (advancementDisplay == null) {
                // Not something that would be displayed in chat
                return null;
            }

            boolean broadcastToChat = (boolean) broadcastToChatMethod.invoke(advancementDisplay);
            if (!broadcastToChat) {
                // Not something that would be displayed in chat
                return null;
            }

            Object titleChat = titleMethod.invoke(advancementDisplay);
            Object descriptionChat = descriptionMethod.invoke(advancementDisplay);
            Enum<?> enumValue = (Enum<?>) frameMethod.invoke(advancementDisplay);
            String enumId = enumValue != null ? (String) idMethod.invoke(enumValue) : null;
            return new ReturnData(
                    toJson(titleChat),
                    toJson(descriptionChat),
                    enumId
            );
        }

        private String toJson(Object chat) throws ReflectiveOperationException {
            return (String) toJsonMethod.invoke(null, chat);
        }
    }

    private static class ReturnData {

        private final String titleJson;
        private final String descriptionJson;
        private final String frameId;

        public ReturnData(String titleJson, String descriptionJson, String frameId) {
            this.titleJson = titleJson;
            this.descriptionJson = descriptionJson;
            this.frameId = frameId;
        }
    }

    // Event is not cancellable
    @Override
    protected void observeEvents(boolean enable) {}

    @Override
    protected void collectRelevantHandlerLists(Consumer<Class<?>> eventClassConsumer) {
        eventClassConsumer.accept(PlayerAdvancementDoneEvent.class);
    }
}
