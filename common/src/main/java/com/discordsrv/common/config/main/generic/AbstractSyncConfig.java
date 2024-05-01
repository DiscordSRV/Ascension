package com.discordsrv.common.config.main.generic;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.configurate.annotation.Constants;
import com.discordsrv.common.sync.enums.SyncDirection;
import com.discordsrv.common.sync.enums.SyncSide;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

import java.util.Arrays;

/**
 * A configuration for a synchronizable.
 * @param <C> the implementation type
 * @param <G> the game identifier
 * @param <D> the Discord identifier
 */
@ConfigSerializable
public abstract class AbstractSyncConfig<C extends AbstractSyncConfig<C, G, D>, G, D> {

    @Comment("The direction to synchronize in.\n"
            + "Valid options: %1, %2, %3")
    @Constants.Comment({"bidirectional", "minecraft_to_discord", "discord_to_minecraft"})
    public SyncDirection direction = SyncDirection.BIDIRECTIONAL;

    @Comment("Timed resynchronization")
    public TimerConfig timer = new TimerConfig();

    @ConfigSerializable
    public static class TimerConfig {

        @Comment("If timed synchronization is enabled")
        public boolean enabled = true;

        @Comment("The amount of minutes between timed synchronization cycles")
        public int cycleTime = 5;
    }

    @Comment("Decides which side takes priority when using timed synchronization or the resync command and there are differences\n"
            + "Valid options: %1, %2")
    @Constants.Comment({"minecraft", "discord"})
    public SyncSide tieBreaker = SyncSide.MINECRAFT;

    public abstract boolean isSet();

    public abstract G gameId();
    public abstract D discordId();
    public abstract boolean isSameAs(C otherConfig);

    public abstract String describe();

    public boolean validate(String syncName, DiscordSRV discordSRV) {
        String label = syncName + " (" + describe() + ")";
        boolean invalidTieBreaker, invalidDirection = false;
        if ((invalidTieBreaker = (tieBreaker == null)) || (invalidDirection = (direction == null))) {
            if (invalidTieBreaker) {
                discordSRV.logger().error(label + " has invalid tie-breaker: " + tieBreaker
                                                  + ", should be one of " + Arrays.toString(SyncSide.values()));
            }
            if (invalidDirection) {
                discordSRV.logger().error(label + " has invalid direction: " + direction
                                                  + ", should be one of " + Arrays.toString(SyncDirection.values()));
            }
            return false;
        } else if (direction != SyncDirection.BIDIRECTIONAL) {
            boolean minecraft;
            if ((direction == SyncDirection.MINECRAFT_TO_DISCORD) != (minecraft = (tieBreaker == SyncSide.MINECRAFT))) {
                SyncSide opposite = (minecraft ? SyncSide.DISCORD : SyncSide.MINECRAFT);
                discordSRV.logger().warning(
                        label + " with direction " + direction + " with tie-breaker " + tieBreaker + " (should be " + opposite + ")"
                );
                tieBreaker = opposite; // Fix the config
            }
        }
        return true;
    }

}
