package com.discordsrv.common.discord.api.guild;

import com.discordsrv.api.discord.api.entity.guild.DiscordGuildMember;
import com.discordsrv.common.discord.api.user.DiscordUserImpl;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class DiscordGuildMemberImpl extends DiscordUserImpl implements DiscordGuildMember {

    private final String nickname;

    public DiscordGuildMemberImpl(Member member) {
        super(member.getUser());
        this.nickname = member.getNickname();
    }

    @Override
    public @NotNull Optional<String> getNickname() {
        return Optional.ofNullable(nickname);
    }
}
