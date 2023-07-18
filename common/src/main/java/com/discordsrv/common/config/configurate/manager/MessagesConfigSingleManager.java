package com.discordsrv.common.config.configurate.manager;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.configurate.manager.abstraction.TranslatedConfigManager;
import com.discordsrv.common.config.configurate.manager.loader.YamlConfigLoaderProvider;
import com.discordsrv.common.config.messages.MessagesConfig;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.nio.file.Paths;
import java.util.Locale;

public class MessagesConfigSingleManager<C extends MessagesConfig>
        extends TranslatedConfigManager<C, YamlConfigurationLoader>
        implements YamlConfigLoaderProvider {

    private final MessagesConfigManager<C> aggregateManager;
    private final Locale locale;

    protected MessagesConfigSingleManager(DiscordSRV discordSRV, MessagesConfigManager<C> aggregateManager, Locale locale) {
        super(discordSRV);
        this.aggregateManager = aggregateManager;
        this.locale = locale;
    }

    @Override
    protected String fileName() {
        boolean multiple = discordSRV.config().messages.multiple;
        if (multiple) {
            return Paths.get("messages", locale.getISO3Language() + ".yaml").toString();
        }

        return MessagesConfig.FILE_NAME;
    }

    @Override
    public Locale locale() {
        return locale;
    }

    @Override
    public C createConfiguration() {
        return aggregateManager.createConfiguration();
    }
}
