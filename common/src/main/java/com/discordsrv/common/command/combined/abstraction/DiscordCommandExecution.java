package com.discordsrv.common.command.combined.abstraction;

import com.discordsrv.api.discord.events.interaction.command.DiscordChatInputInteractionEvent;
import com.discordsrv.common.DiscordSRV;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class DiscordCommandExecution implements CommandExecution {

    private final DiscordSRV discordSRV;
    private final DiscordChatInputInteractionEvent event;
    private final AtomicBoolean isEphemeral = new AtomicBoolean(true);
    private final AtomicReference<InteractionHook> hook = new AtomicReference<>();

    public DiscordCommandExecution(DiscordSRV discordSRV, DiscordChatInputInteractionEvent event) {
        this.discordSRV = discordSRV;
        this.event = event;
    }

    @Override
    public void setEphemeral(boolean ephemeral) {
        isEphemeral.set(ephemeral);
    }

    @Override
    public String getArgument(String label) {
        OptionMapping mapping = event.asJDA().getOption(label);
        return mapping != null ? mapping.getAsString() : null;
    }

    @Override
    public void send(Collection<Text> texts) {
        StringBuilder builder = new StringBuilder();
        EnumMap<Text.Formatting, Boolean> formats = new EnumMap<>(Text.Formatting.class);

        for (Text text : texts) {
            if (StringUtils.isEmpty(text.content())) continue;

            verifyStyle(builder, formats, text);
            builder.append(text.content());
        }

        verifyStyle(builder, formats, null);

        InteractionHook interactionHook = hook.get();
        boolean ephemeral = isEphemeral.get();
        if (interactionHook != null) {
            interactionHook.sendMessage(builder.toString()).setEphemeral(ephemeral).queue();
        } else {
            event.asJDA().reply(builder.toString()).setEphemeral(ephemeral).queue();
        }
    }

    private void verifyStyle(StringBuilder builder, EnumMap<Text.Formatting, Boolean> formats, Text text) {
        for (Text.Formatting format : Text.Formatting.values()) {
            boolean is = formats.computeIfAbsent(format, key -> false);
            boolean thisIs = text != null && text.discordFormatting().contains(format);

            if (is != thisIs) {
                // should end or start
                builder.append(format.discord());
                formats.put(format, thisIs);
            }
        }
    }

    @Override
    public void runAsync(Runnable runnable) {
        event.asJDA().deferReply(isEphemeral.get()).queue(ih -> {
            hook.set(ih);
            discordSRV.scheduler().run(runnable);
        });
    }
}
