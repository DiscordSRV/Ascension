/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.discordsrv.modded.util;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Helper used by mixins to locate classes inside the NeoForge loader and invoke methods
 * (static or instance) in a concise, null-safe way.
 */
public final class MixinUtils {

    public static <R> ClassBuilder<R> withClass(String className) {
        return new ClassBuilder<>(ClassLoaderUtils.find(className), null);
    }

    public static <R> ClassBuilder<R> withClass(String className, Class<R> returnType) {
        return new ClassBuilder<>(ClassLoaderUtils.find(className), returnType);
    }

    public static final class ClassBuilder<R> {
        private final ClassLoaderUtils.LoadedClass lc;
        private final Class<R> returnType; // may be null if unknown
        private Object instance;
        private String methodName;
        private Object[] args = new Object[0];

        private ClassBuilder(ClassLoaderUtils.LoadedClass lc, Class<R> returnType) {
            this.lc = lc;
            this.returnType = returnType;
        }

        /**
         * Ask the target class to provide its instance via a static withInstance(Consumer) callback.
         * If the target class or method aren't present this is a no-op.
         */
        public ClassBuilder<R> withInstance() {
            if (lc.notPresent()) return this;
            AtomicReference<Object> ref = new AtomicReference<>();
            Consumer<Object> consumer = ref::set;
            try {
                lc.invokeStatic("withInstance", new Class<?>[]{Consumer.class}, consumer);
            } catch (Throwable ignored) {}
            Object val = ref.get();
            if (val != null) this.instance = val;
            return this;
        }

        /**
         * Explicitly set the instance to invoke the method on.
         */
        public ClassBuilder<R> withInstance(Object instance) {
            this.instance = instance;
            return this;
        }

        /**
         * Select a method name to call and the arguments to pass. The invocation will try to match
         * a method by runtime argument types (assignability) and fall back to exact signature lookup.
         */
        public ClassBuilder<R> withMethod(String name, Object... args) {
            this.methodName = name;
            this.args = args == null ? new Object[0] : args;
            return this;
        }

        /**
         * Execute the configured method. Returns an Optional with the result or Optional.empty() on failure.
         * If the configured returnType is Void.class the method will be invoked but Optional.empty() returned.
         */
        @SuppressWarnings("unchecked")
        public Optional<R> execute() {
            if (lc.notPresent() || methodName == null) return Optional.empty();

            Optional<Object> res;
            if (instance != null) {
                res = lc.invokeInstanceByArgs(instance, methodName, args);
            } else {
                res = lc.invokeStaticByArgs(methodName, args);
            }

            if (returnType == Void.class) {
                // ensure invocation occurred (res may be empty if lookup failed), but return empty for Void
                return Optional.empty();
            }

            if (returnType != null) {
                return res.flatMap(o -> {
                    try {
                        return Optional.ofNullable(returnType.cast(o));
                    } catch (Throwable t) {
                        return Optional.empty();
                    }
                });
            }

            // Best-effort unchecked cast when returnType wasn't provided
            return (Optional<R>) res;
        }
    }
}
