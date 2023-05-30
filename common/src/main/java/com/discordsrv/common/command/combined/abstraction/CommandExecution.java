package com.discordsrv.common.command.combined.abstraction;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public interface CommandExecution {

    void setEphemeral(boolean ephemeral);

    String getArgument(String label);

    default void send(Text... texts) {
        send(Arrays.asList(texts));
    }

    default void send(Collection<Text> texts) {
        send(texts, Collections.emptyList());
    }

    void send(Collection<Text> texts, Collection<Text> extra);

    void runAsync(Runnable runnable);
}
