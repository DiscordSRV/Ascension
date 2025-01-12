package com.discordsrv.fabric.listener;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.events.message.receive.game.GameChatMessageReceiveEvent;
import com.discordsrv.common.feature.channel.global.GlobalChannel;
import com.discordsrv.fabric.FabricDiscordSRV;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.kyori.adventure.text.Component;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;

public class FabricChatListener {
    private final FabricDiscordSRV discordSRV;

    public FabricChatListener(FabricDiscordSRV discordSRV) {
        this.discordSRV = discordSRV;

        ServerMessageEvents.CHAT_MESSAGE.register(this::onChatMessage);
    }

    private void onChatMessage(SignedMessage signedMessage, ServerPlayerEntity serverPlayerEntity, MessageType.Parameters parameters) {
        Component component = discordSRV.componentFactory().parse(signedMessage.getSignedContent());

        discordSRV.eventBus().publish(new GameChatMessageReceiveEvent(
            null,
            discordSRV.playerProvider().player(serverPlayerEntity),
            MinecraftComponent.fromAdventure((com.discordsrv.unrelocate.net.kyori.adventure.text.Component) component),
            new GlobalChannel(discordSRV),
            false
        ));
    }

}
