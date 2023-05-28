package com.discordsrv.common.command.combined.abstraction;

import java.util.Arrays;
import java.util.Collection;

public interface CommandExecution {

    void setEphemeral(boolean ephemeral);

    String getArgument(String label);

    default void send(Text... texts) {
        send(Arrays.asList(texts));
    }
    void send(Collection<Text> texts);

    void runAsync(Runnable runnable);
}
