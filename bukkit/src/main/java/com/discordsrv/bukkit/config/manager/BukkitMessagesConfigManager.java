package com.discordsrv.bukkit.config.manager;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.configurate.manager.MessagesConfigManager;
import com.discordsrv.common.config.messages.MessagesConfig;

public class BukkitMessagesConfigManager extends MessagesConfigManager<MessagesConfig> {

    public BukkitMessagesConfigManager(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    public MessagesConfig createConfiguration() {
        return new MessagesConfig();
    }
}
