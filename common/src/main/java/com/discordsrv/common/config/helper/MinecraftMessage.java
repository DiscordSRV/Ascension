package com.discordsrv.common.config.helper;

import com.discordsrv.api.DiscordSRVApi;
import com.discordsrv.api.component.GameTextBuilder;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.component.util.ComponentUtil;
import net.kyori.adventure.text.Component;

public class MinecraftMessage {

    private final String rawFormat;

    public MinecraftMessage(String rawFormat) {
        this.rawFormat = rawFormat;
    }

    public String rawFormat() {
        return rawFormat;
    }

    public GameTextBuilder textBuilder() {
        DiscordSRVApi discordSRV = DiscordSRVApi.get();
        if (discordSRV == null) {
            throw new IllegalStateException("DiscordSRVApi == null");
        }
        return discordSRV.componentFactory().textBuilder(rawFormat);
    }

    public MinecraftComponent make() {
        return textBuilder().build();
    }

    public Component asComponent() {
        return ComponentUtil.fromAPI(make());
    }
}
