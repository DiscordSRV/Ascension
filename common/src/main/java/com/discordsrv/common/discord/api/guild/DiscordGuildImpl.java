package com.discordsrv.common.discord.api.guild;

import com.discordsrv.api.discord.api.entity.guild.DiscordGuild;
import com.discordsrv.api.discord.api.entity.guild.DiscordGuildMember;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.Optional;

public class DiscordGuildImpl implements DiscordGuild {

    private final DiscordSRV discordSRV;
    private final String id;
    private final int memberCount;

    public DiscordGuildImpl(DiscordSRV discordSRV, Guild guild) {
        this.discordSRV = discordSRV;
        this.id = guild.getId();
        this.memberCount = guild.getMemberCount();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getMemberCount() {
        return memberCount;
    }

    @Override
    public Optional<DiscordGuildMember> getMemberById(String id) {
        JDA jda = discordSRV.jda();
        if (jda == null) {
            return Optional.empty();
        }

        Guild guild = jda.getGuildById(this.id);
        if (guild == null) {
            return Optional.empty();
        }

        Member member = guild.getMemberById(id);
        return member != null
                ? Optional.of(new DiscordGuildMemberImpl(member))
                : Optional.empty();
    }
}
