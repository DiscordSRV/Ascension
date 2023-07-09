package com.discordsrv.bukkit.console.executor;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.component.util.ComponentUtil;
import com.discordsrv.unrelocate.net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;

import java.util.function.Consumer;

public class PaperCommandFeedbackExecutor implements Consumer<Component> {

    private final Consumer<MinecraftComponent> componentConsumer;
    private final CommandSender sender;

    @SuppressWarnings("unchecked")
    public PaperCommandFeedbackExecutor(Server server, Consumer<MinecraftComponent> componentConsumer) {
        this.componentConsumer = componentConsumer;
        this.sender = server.createCommandSender((Consumer<? super net.kyori.adventure.text.Component>) (Object) this);
    }

    public CommandSender sender() {
        return sender;
    }

    @Override
    public void accept(Component component) {
        MinecraftComponent minecraftComponent = ComponentUtil.fromUnrelocated(component);
        componentConsumer.accept(minecraftComponent);
    }
}
