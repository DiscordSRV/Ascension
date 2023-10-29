package com.discordsrv.common.config.configurate.manager;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.configurate.manager.abstraction.TranslatedConfigManager;
import com.discordsrv.common.config.configurate.manager.loader.YamlConfigLoaderProvider;
import com.discordsrv.common.config.messages.MessagesConfig;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.util.Locale;

public class MessagesConfigSingleManager<C extends MessagesConfig>
        extends TranslatedConfigManager<C, YamlConfigurationLoader>
        implements YamlConfigLoaderProvider {

    private final MessagesConfigManager<C> aggregateManager;
    private final Locale locale;
    private final boolean multi;

    protected MessagesConfigSingleManager(DiscordSRV discordSRV, MessagesConfigManager<C> aggregateManager, Locale locale, boolean multi) {
        super(discordSRV);
        this.aggregateManager = aggregateManager;
        this.locale = locale;
        this.multi = multi;
    }

    @Override
    public String fileName() {
        if (multi) {
            return aggregateManager.directory().resolve(locale.getISO3Language() + ".yaml").toString();
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
