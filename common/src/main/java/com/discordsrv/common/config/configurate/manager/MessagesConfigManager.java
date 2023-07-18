package com.discordsrv.common.config.configurate.manager;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.messages.MessagesConfig;
import com.discordsrv.common.exception.ConfigException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public abstract class MessagesConfigManager<C extends MessagesConfig> {

    public Map<String, MessagesConfigSingleManager<C>> configs = new LinkedHashMap<>();

    public MessagesConfigManager(DiscordSRV discordSRV) {
        configs.put(Locale.US.getISO3Language(), new MessagesConfigSingleManager<>(discordSRV, this, Locale.US));
    }

    public abstract C createConfiguration();

    public MessagesConfigSingleManager<C> getManager(Locale locale) {
        return configs.get(locale.getISO3Language());
    }

    public void load() throws ConfigException {
        for (Map.Entry<String, MessagesConfigSingleManager<C>> entry : configs.entrySet()) {
            entry.getValue().load();
        }
    }
}
