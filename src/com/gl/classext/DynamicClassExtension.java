package com.gl.classext;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class DynamicClassExtension {
    public <R, T, E> void addExtensionOperation(Class<T> aClass,
                                                          Class<E> anExtensionClass,
                                                          String anOperationName,
                                                          Function<T, R> anOperation) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionClass);
        Objects.requireNonNull(anOperationName);
        Objects.requireNonNull(anOperation);

        operationsMap.put(new OperationKey(aClass, anExtensionClass, operationName(anOperationName, null)), anOperation);
    }

    static final private String[] DUMMY_ARGS = {"true"};
    public <R, T, U, E> void addExtensionOperation(Class<T> aClass,
                                                   Class<E> anExtensionClass,
                                                   String anOperationName,
                                                   BiFunction<T, U, R> anOperation) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionClass);
        Objects.requireNonNull(anOperationName);
        Objects.requireNonNull(anOperation);

        operationsMap.put(new OperationKey(aClass, anExtensionClass, operationName(anOperationName, DUMMY_ARGS)), anOperation);
    }

    public <T, E> void addVoidExtensionOperation(Class<T> aClass,
                                                    Class<E> anExtensionClass,
                                                    String anOperationName,
                                                    Consumer<T> anOperation) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionClass);
        Objects.requireNonNull(anOperationName);
        Objects.requireNonNull(anOperation);

        operationsMap.put(new OperationKey(aClass, anExtensionClass, operationName(anOperationName, null)), anOperation);
    }

    public <T, U, E> void addVoidExtensionOperation(Class<T> aClass,
                                                    Class<E> anExtensionClass,
                                                    String anOperationName,
                                                    BiConsumer<T, U> anOperation) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionClass);
        Objects.requireNonNull(anOperationName);
        Objects.requireNonNull(anOperation);

        operationsMap.put(new OperationKey(aClass, anExtensionClass, operationName(anOperationName, DUMMY_ARGS)), anOperation);
    }

    public <T, E> Object getExtensionOperation(Class<T> aClass,
                                               Class<E> anExtensionClass,
                                               String anOperationName) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionClass);
        Objects.requireNonNull(anOperationName);

        return operationsMap.get(new OperationKey(aClass, anExtensionClass, anOperationName));
    }

    public <T, E> void removeExtensionOperation(Class<T> aClass,
                                                Class<E> anExtensionClass,
                                                String anOperationName) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionClass);
        Objects.requireNonNull(anOperationName);

        operationsMap.remove(new OperationKey(aClass, anExtensionClass, anOperationName));
    }

    /**
     * Finds and returns an extension object according to a supplied class. You should use calls of
     * {@code addExtensionOperation()} to compose dynamic extensions before calling the {@code dynamicExtension} method.
     * Otherwise, an empty dynamic extension having no operations will be returned.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionClass class of extension object to be returned
     * @return an extension object
     */
    @SuppressWarnings({"unchecked"})
    public <T> T extensionNoCache(Object anObject, Class<T> anExtensionClass) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionClass);

        return (T) Proxy.newProxyInstance(anExtensionClass.getClassLoader(),
                new Class<?>[]{anExtensionClass},
                (proxy, method, args) -> performOperation(anObject, anExtensionClass, method, args));
    }

    /**
     * Finds and returns an extension object according to a supplied class. You should use calls of
     * {@code addExtensionOperation()} to compose dynamic extensions before calling the {@code dynamicExtension} method.
     * Otherwise, an empty dynamic extension having no operations will be returned. It uses cache to avoid redundant
     * objects creation.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionClass class of extension object to be returned
     * @return an extension object
     */
    @SuppressWarnings({"unchecked"})
    public <T> T extension(Object anObject, Class<T> anExtensionClass) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionClass);

        return (T) extensionCache.getOrCreate(anObject, () -> extensionNoCache(anObject, anExtensionClass));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    <T> Object performOperation(Object anObject, Class<T> anExtensionClass, Method method, Object[] args) {
        Object result = null;

        if ("equals".equals(method.getName())) {
            result = anObject.equals(args[0]);
        } else if ("hashCode".equals(method.getName())) {
            return anObject.hashCode();
        } else if ("toString".equals(method.getName())) {
            return anObject.toString();
        } else {
            Object operation = findExtensionOperation(anObject, anExtensionClass, method, args);
            if (operation != null) {
                if (void.class.equals(method.getReturnType())) {
                    if (args == null || args.length == 0) {
                        ((Consumer) operation).accept(anObject);
                    } else if (args.length == 1) {
                        ((BiConsumer) operation).accept(anObject, args[0]);
                    }
                } else if (args == null || args.length == 0) {
                    result = ((Function) operation).apply(anObject);
                } else if (args.length == 1) {
                    result = ((BiFunction) operation).apply(anObject, args[0]);
                }
            } else {
                throw new IllegalArgumentException(MessageFormat.format("No {0} operation for {1}",
                        method.getName(), anObject));
            }
        }

        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    <T> Object findExtensionOperation(Object anObject, Class<T> anExtensionClass, Method method, Object[] anArgs) {
        Object result;

        Class current = anObject.getClass();
        do {
            result = getExtensionOperation(current, anExtensionClass, operationName(method.getName(), anArgs));
            current = current.getSuperclass();
        } while (current != null && result == null);
        return result;
    }

    String operationName(String anOperationName, Object[] anArgs) {
        return anArgs == null || anArgs.length == 0 ? anOperationName : anOperationName + "Bi";
    }

    @SuppressWarnings({"rawtypes"})
    record OperationKey(Class clazz, Class extensionClass, String operationName) {
    }

    private final ConcurrentHashMap<OperationKey, Object> operationsMap = new ConcurrentHashMap<>();
    private static final DynamicClassExtension dynamicClassExtension = new DynamicClassExtension();
    //region Cache methods

    @SuppressWarnings("rawtypes")
    final ThreadSafeWeakCache extensionCache = new ThreadSafeWeakCache();

    /**
     * Cleanups cache by removing keys for all already garbage collected values
     */
    public void cacheCleanup() {
        extensionCache.cleanup();
    }

    /**
     * Clears cache
     */
    public void cacheClear() {
        extensionCache.clear();
    }

    /**
     * Schedules automatic cache cleanup that should be performed once a minute
     */
    public void scheduleCacheCleanup() {
        extensionCache.scheduleCleanup();
    }

    /**
     * Shutdowns automatic cache cleanup
     */
    public void shutdownCacheCleanup() {
        extensionCache.shutdownCleanup();
    }

    /**
     * Check if cache is empty
     * @return true if cache is empty; false otherwise
     */
    boolean cacheIsEmpty() {
        return extensionCache.isEmpty();
    }
    //endregion

    public static DynamicClassExtension sharedInstance() {
        return dynamicClassExtension;
    }

    public static <E> Builder<E> sharedBuilder(Class<E> aExtensionClass) {
        return dynamicClassExtension.builder(aExtensionClass);
    }

    public <E> Builder<E> builder(Class<E> aExtensionClass) {
        return new Builder<>(aExtensionClass, this);
    }

    public static class Builder<E> {
        private final DynamicClassExtension dynamicClassExtension;
        private final Class<E> extensionClass;
        private String operationName;

        Builder(Class<E> aExtensionClass, DynamicClassExtension aDynamicClassExtension) {
            extensionClass = aExtensionClass;
            dynamicClassExtension = aDynamicClassExtension;
        }

        Builder(Class<E> aExtensionClass, String aOperationName, DynamicClassExtension aDynamicClassExtension) {
            extensionClass = aExtensionClass;
            operationName = aOperationName;
            dynamicClassExtension = aDynamicClassExtension;
        }

        public Builder<E> name(String anOperationName) {
            return new Builder<>(extensionClass, anOperationName, dynamicClassExtension);
        }

        public <R, T1> Builder<E> op(Class<T1> anObjectClass, Function<T1, R> anOperation) {
            dynamicClassExtension.addExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension);
        }

        public <R, T1, U> Builder<E> op(Class<T1> anObjectClass, BiFunction<T1, U, R> anOperation) {
            dynamicClassExtension.addExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension);
        }

        public <T1> Builder<E> voidOp(Class<T1> anObjectClass, Consumer<T1> anOperation) {
            dynamicClassExtension.addVoidExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension);
        }

        public <T1, U> Builder<E> voidOp(Class<T1> anObjectClass, BiConsumer<T1, U> anOperation) {
            dynamicClassExtension.addVoidExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension);
        }

        public DynamicClassExtension build() {
            return dynamicClassExtension;
        }
    }
}
