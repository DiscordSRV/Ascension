package com.discordsrv.fabric;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.core.component.translation.TranslationLoader;
import net.minecraft.util.Language;

import java.net.URL;

//TODO: Pull translations from mods and datapacks
public class FabricTranslationLoader extends TranslationLoader {

    public FabricTranslationLoader(DiscordSRV discordSRV) {
        super(discordSRV);
    }

    @Override
    protected URL findResource(String name) {
        ClassLoader classLoader = Language.class.getClassLoader();
        URL url = null;
        while (classLoader != null && url == null) {
            url = classLoader.getResource(name);
            classLoader = classLoader.getParent();
        }
        return url;
    }
}
