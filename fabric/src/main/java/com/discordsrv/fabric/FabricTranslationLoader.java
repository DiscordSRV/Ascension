package com.discordsrv.fabric;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.component.translation.Translation;
import com.discordsrv.common.core.component.translation.TranslationLoader;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class FabricTranslationLoader extends TranslationLoader {

    public FabricTranslationLoader(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    protected void loadMCTranslations(AtomicBoolean any) {
        Map<String, Translation> translations = new HashMap<>();

        getClass().getClassLoader().resources("assets/minecraft/lang/en_us.json").forEach(url -> {
            try {
                translations.putAll(getFromJson(url));
            } catch (Throwable t) {
                logger.debug("Failed to load translations from " + url, t);
            }
        });

        if (!translations.isEmpty()) {
            discordSRV.componentFactory().translationRegistry().register(Locale.US, translations);
            logger.debug("Found " + translations.size() + " Minecraft translations for en_us");
            any.set(true);
        }
    }
}
