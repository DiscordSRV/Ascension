package com.discordsrv.api.discord.api.entity;

import org.jetbrains.annotations.NotNull;

/**
 * A snowflake identifier.
 */
public interface Snowflake {

    /**
     * Gets the id of this entity.
     * @return the id of this entity
     */
    @NotNull
    String getId();
}
