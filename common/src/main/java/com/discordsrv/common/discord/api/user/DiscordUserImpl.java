package com.discordsrv.common.discord.api.user;

import com.discordsrv.api.discord.api.entity.user.DiscordUser;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

public class DiscordUserImpl implements DiscordUser {

    private final String id;
    private final String username;
    private final String discriminator;

    public DiscordUserImpl(User user) {
        this.id = user.getId();
        this.username = user.getName();
        this.discriminator = user.getDiscriminator();
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull String getUsername() {
        return username;
    }

    @Override
    public @NotNull String getDiscriminator() {
        return discriminator;
    }
}
