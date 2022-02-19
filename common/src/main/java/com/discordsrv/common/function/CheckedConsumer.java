package com.discordsrv.common.function;

@FunctionalInterface
public interface CheckedConsumer<I> {

    void accept(I input) throws Throwable;
}
