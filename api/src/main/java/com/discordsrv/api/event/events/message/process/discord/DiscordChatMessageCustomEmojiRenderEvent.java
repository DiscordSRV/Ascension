package com.discordsrv.api.event.events.message.process.discord;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.discord.entity.guild.DiscordCustomEmoji;
import com.discordsrv.api.event.events.Event;
import com.discordsrv.api.event.events.Processable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Renders a given {@link DiscordCustomEmoji} into a {@link MinecraftComponent} that can be displayed in Minecraft.
 * @see #process(MinecraftComponent)
 */
public class DiscordChatMessageCustomEmojiRenderEvent implements Event, Processable.Argument<MinecraftComponent> {

    private final DiscordCustomEmoji emoji;
    private MinecraftComponent rendered = null;

    public DiscordChatMessageCustomEmojiRenderEvent(@NotNull DiscordCustomEmoji emoji) {
        this.emoji = emoji;
    }

    @NotNull
    public DiscordCustomEmoji getEmoji() {
        return emoji;
    }

    /**
     * Gets the rendered representation of the emoji.
     * @return the rendered representation of the emoji if this event has been processed otherwise {@code null}
     */
    @Nullable
    public MinecraftComponent getRenderedEmojiFromProcessing() {
        return rendered;
    }

    @Override
    public boolean isProcessed() {
        return rendered != null;
    }

    /**
     * Marks this event as processed, with the given {@link MinecraftComponent} being the representation of {@link DiscordCustomEmoji} in game.
     * @param renderedEmote the rendered emote
     */
    @Override
    public void process(@NotNull MinecraftComponent renderedEmote) {
        if (rendered != null) {
            throw new IllegalStateException("Cannot process an already processed event");
        }

        rendered = renderedEmote;
    }
}
