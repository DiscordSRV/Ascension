package com.discordsrv.common.events.player;

import com.discordsrv.api.eventbus.EventPriorities;
import com.discordsrv.api.events.Event;
import com.discordsrv.api.events.Processable;
import com.discordsrv.common.abstraction.player.IOfflinePlayer;
import com.discordsrv.common.abstraction.player.provider.model.SkinInfo;
import org.jetbrains.annotations.Nullable;


/**
 * This event is used by DiscordSRV to lookup {@link SkinInfo} for players from platform methods (and optionally integrations).
 * This is also used to determine which skin provider should take priority when there are multiple providers ({@link EventPriorities}).
 */
public class PlayerCollectSkinEvent implements Event, Processable.Argument<SkinInfo> {

    private final IOfflinePlayer player;

    private boolean processed;
    private SkinInfo skinInfo = null;

    public PlayerCollectSkinEvent(IOfflinePlayer player) {
        this.player = player;
    }

    public IOfflinePlayer player() {
        return player;
    }

    @Override
    public boolean isProcessed() {
        return processed;
    }

    /**
     * Returns the skin info from a {@link #process(SkinInfo)}.
     * @return the skin info for the player
     * @throws IllegalStateException if {@link #isProcessed()} doesn't return true
     */
    @Nullable
    public SkinInfo getSkinInfoFromProcessing() {
        if (!processed) {
            throw new IllegalStateException("This event has not been successfully processed yet, no skin is available");
        }
        return skinInfo;
    }

    @Override
    public void process(SkinInfo skinInfo) {
        Processable.Argument.super.process(skinInfo);

        this.skinInfo = skinInfo;
        this.processed = true;
    }
}
