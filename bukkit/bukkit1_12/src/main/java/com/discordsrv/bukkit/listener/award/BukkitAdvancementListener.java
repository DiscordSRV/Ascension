package com.discordsrv.bukkit.listener.award;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.component.util.ComponentUtil;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.advancement.Advancement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.lang.reflect.Method;
import java.util.Arrays;

public class BukkitAdvancementListener extends AbstractBukkitAwardListener {

    private final NMS nms;

    public BukkitAdvancementListener(DiscordSRV discordSRV, IBukkitAwardForwarder forwarder) {
        super(discordSRV, forwarder);

        String className = Bukkit.getServer().getClass().getName();
        String[] packageParts = className.split("\\.");
        if (packageParts.length != 5) {
            this.nms = null;
            logger.error("Server does not have NMS, incompatible with advancements.");
            return;
        }

        String version = packageParts[3];
        NMS nms = null;
        try {
            if ((version.startsWith("v1_19") && !version.startsWith("v1_19_R1") && !version.startsWith("v1_19_R2"))
                    || version.startsWith("v1_2")) {
                // 1.19.4+
                nms = new NMS("org.bukkit.craftbukkit." + version + ".advancement.CraftAdvancement",
                                   "k", "d", "i", "a");
            } else {
                // <1.19.4
                nms = new NMS("org.bukkit.craftbukkit." + version + ".advancement.CraftAdvancement",
                                   "j", "c", "i", "a");
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
            if (data == null || checkIfShouldSkip(event.getPlayer())) {
                return;
            }

            forwarder.publishEvent(
                    event.getPlayer(),
                    data.nameJson != null ? ComponentUtil.toAPI(BukkitComponentSerializer.gson().deserialize(data.nameJson)) : null,
                    data.titleJson != null ? ComponentUtil.toAPI(BukkitComponentSerializer.gson().deserialize(data.titleJson)) : null,
                    false
            );
        } catch (ReflectiveOperationException e) {
            logger.debug("Failed to get advancement data", e);
        }
    }

    private static class NMS {

        private final Method handleMethod;
        private final Method advancementNameMethod;
        private final Method advancementDisplayMethod;
        private final Method broadcastToChatMethod;
        private final Method titleMethod;
        private final Method toJsonMethod;

        public NMS(
                String craftAdvancementClassName,
                String nameMethodName,
                String displayMethodName,
                String broadcastToChatMethodName,
                String titleMethodName
        ) throws ReflectiveOperationException {
            Class<?> clazz = Class.forName(craftAdvancementClassName);
            handleMethod = clazz.getDeclaredMethod("getHandle");
            Class<?> nmsClass = handleMethod.getReturnType();
            advancementNameMethod = nmsClass.getDeclaredMethod(nameMethodName);
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

            Object nameChat = advancementNameMethod.invoke(nms);
            Object titleChat = titleMethod.invoke(display);

            return new ReturnData(
                    toJson(nameChat),
                    toJson(titleChat)
            );
        }

        private String toJson(Object chat) throws ReflectiveOperationException {
            return (String) toJsonMethod.invoke(chat);
        }
    }

    private static class ReturnData {

        private final String nameJson;
        private final String titleJson;

        public ReturnData(String nameJson, String titleJson) {
            this.nameJson = nameJson;
            this.titleJson = titleJson;
        }
    }
}
