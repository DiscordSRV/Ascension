package com.discordsrv.common.testing;

import java.util.function.Consumer;

public final class TestHelper {

    private static final ThreadLocal<Consumer<Throwable>> error = new ThreadLocal<>();

    public static void fail(Throwable throwable) {
        Consumer<Throwable> handler = error.get();
        if (handler != null) {
            handler.accept(throwable);
        }
    }

    public static void set(Consumer<Throwable> handler) {
        error.set(handler);
    }
}
