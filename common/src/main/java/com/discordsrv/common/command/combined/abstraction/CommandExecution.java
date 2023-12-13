package com.discordsrv.common.command.combined.abstraction;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

public interface CommandExecution {

    Locale locale();

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
