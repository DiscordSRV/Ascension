package com.discordsrv.modded.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class ClassLoaderUtils {

    private static ClassLoader classLoader = ClassLoaderUtils.class.getClassLoader();
    public static void setClassLoader(ClassLoader loader) {
        classLoader = loader;
    }

    /**
     * Load a class using the loader's classloader and, if successful, run the provided consumer.
     * If the class isn't present the consumer will not be invoked.
     */
    public static void withClass(String className, Consumer<Class<?>> consumer) {
        try {
            Class<?> cls = Class.forName(className, false, classLoader);
            if (consumer != null) consumer.accept(cls);

        } catch (Throwable ignored) {}
    }

    /**
     * Wrapper for classes that expose a static "withInstance(Consumer)".
     * If the target class is present, calls its static withInstance method with the provided consumer.
     */
    public static void withClassWithInstance(String className, Consumer<Object> instanceConsumer) {
        withClass(className, cls -> {
            try {
                Method m = cls.getMethod("withInstance", Consumer.class);
                m.invoke(null, instanceConsumer);
            } catch (Throwable ignored) {}
        });
    }

    /**
     * Find a class via the loader and return a wrapper that makes reflection easier.
     * If the class cannot be found, an empty LoadedClass is returned.
     */
    public static LoadedClass find(String className) {
        try {
            Class<?> cls = Class.forName(className, false, classLoader);
            return new LoadedClass(cls);
        } catch (Throwable ignored) {
            return LoadedClass.empty();
        }
    }

    /**
     * Class<?> wrapper that provides methods to find and invoke methods
     * either by exact parameter types or by matching runtime argument types.
     */
    public static final class LoadedClass {
        private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS = new HashMap<>();
        static {
            PRIMITIVE_WRAPPERS.put(boolean.class, Boolean.class);
            PRIMITIVE_WRAPPERS.put(byte.class, Byte.class);
            PRIMITIVE_WRAPPERS.put(char.class, Character.class);
            PRIMITIVE_WRAPPERS.put(short.class, Short.class);
            PRIMITIVE_WRAPPERS.put(int.class, Integer.class);
            PRIMITIVE_WRAPPERS.put(long.class, Long.class);
            PRIMITIVE_WRAPPERS.put(float.class, Float.class);
            PRIMITIVE_WRAPPERS.put(double.class, Double.class);
            PRIMITIVE_WRAPPERS.put(void.class, Void.class);
        }

        private final Class<?> cls;
        private static final LoadedClass EMPTY = new LoadedClass(null);

        private LoadedClass(Class<?> cls) {
            this.cls = cls;
        }

        public static LoadedClass empty() {
            return EMPTY;
        }

        public boolean notPresent() {
            return cls == null;
        }

        /**
         * Find a method by exact parameter types.
         */
        public Optional<Method> findMethodExact(String name, Class<?>... paramTypes) {
            if (notPresent()) return Optional.empty();
            try {
                Method m = cls.getMethod(name, paramTypes);
                return Optional.of(m);
            } catch (Throwable t) {
                return Optional.empty();
            }
        }

        /**
         * Find a method by matching the runtime argument types (assignability). Null arguments will match non-primitive params.
         */
        public Optional<Method> findMethodByArgs(String name, Object... args) {
            if (notPresent()) return Optional.empty();
            Method[] methods = cls.getMethods();
            outer: for (Method m : methods) {
                if (!m.getName().equals(name)) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length != args.length) continue;
                for (int i = 0; i < params.length; i++) {
                    Object arg = args[i];
                    Class<?> paramType = params[i];
                    if (arg == null) {
                        if (paramType.isPrimitive()) continue outer; // cannot pass null to primitive
                        else continue;
                    }
                    Class<?> argClass = arg.getClass();
                    if (!isAssignable(paramType, argClass)) continue outer;
                }
                return Optional.of(m);
            }
            return Optional.empty();
        }

        /**
         * Invoke a static method by exact parameter types.
         */
        public Optional<Object> invokeStatic(String name, Class<?>[] paramTypes, Object... args) {
            return findMethodExact(name, paramTypes).flatMap(m -> invokeAccessible(m, null, args));
        }

        /**
         * Invoke a static method by matching runtime argument types.
         */
        public Optional<Object> invokeStaticByArgs(String name, Object... args) {
            return findMethodByArgs(name, args).flatMap(m -> invokeAccessible(m, null, args));
        }

        /**
         * Invoke an instance method by exact parameter types.
         */
        public Optional<Object> invokeInstance(Object instance, String name, Class<?>[] paramTypes, Object... args) {
            if (instance == null) return Optional.empty();
            return findMethodExact(name, paramTypes).flatMap(m -> invokeAccessible(m, instance, args));
        }

        /**
         * Invoke an instance method by matching runtime argument types.
         */
        public Optional<Object> invokeInstanceByArgs(Object instance, String name, Object... args) {
            if (instance == null) return Optional.empty();
            return findMethodByArgs(name, args).flatMap(m -> invokeAccessible(m, instance, args));
        }

        /**
         * Invoke by args and return boolean success (useful for void methods where result is null).
         */
        public boolean invokeStaticByArgsAndReport(String name, Object... args) {
            Optional<Method> om = findMethodByArgs(name, args);
            return om.map(m -> invokeMethodAndReport(m, null, args)).orElse(false);
        }

        public boolean invokeInstanceByArgsAndReport(Object instance, String name, Object... args) {
            if (instance == null) return false;
            Optional<Method> om = findMethodByArgs(name, args);
            return om.map(m -> invokeMethodAndReport(m, instance, args)).orElse(false);
        }

        private boolean invokeMethodAndReport(Method m, Object instance, Object... args) {
            try {
                m.setAccessible(true);
                m.invoke(instance, args);
                return true;
            } catch (Throwable t) {
                return false;
            }
        }

        private Optional<Object> invokeAccessible(Method m, Object instance, Object... args) {
            try {
                m.setAccessible(true);
                return Optional.ofNullable(m.invoke(instance, args));
            } catch (Throwable t) {
                return Optional.empty();
            }
        }

        private boolean isAssignable(Class<?> paramType, Class<?> argClass) {
            if (paramType.isPrimitive()) paramType = PRIMITIVE_WRAPPERS.getOrDefault(paramType, paramType);
            return paramType.isAssignableFrom(argClass);
        }
    }
}
