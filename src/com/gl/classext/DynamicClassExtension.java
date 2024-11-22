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

        operationsMap.put(new OperationKey(aClass, anExtensionClass, anOperationName), anOperation);
    }

    public <R, T, U, E> void addExtensionOperation(Class<T> aClass,
                                                   Class<E> anExtensionClass,
                                                   String anOperationName,
                                                   BiFunction<T, U, R> anOperation) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionClass);
        Objects.requireNonNull(anOperationName);
        Objects.requireNonNull(anOperation);

        operationsMap.put(new OperationKey(aClass, anExtensionClass, anOperationName), anOperation);
    }

    public <T, E> void addVoidExtensionOperation(Class<T> aClass,
                                                    Class<E> anExtensionClass,
                                                    String anOperationName,
                                                    Consumer<T> anOperation) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionClass);
        Objects.requireNonNull(anOperationName);
        Objects.requireNonNull(anOperation);

        operationsMap.put(new OperationKey(aClass, anExtensionClass, anOperationName), anOperation);
    }

    public <T, U, E> void addVoidExtensionOperation(Class<T> aClass,
                                                    Class<E> anExtensionClass,
                                                    String anOperationName,
                                                    BiConsumer<T, U> anOperation) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionClass);
        Objects.requireNonNull(anOperationName);
        Objects.requireNonNull(anOperation);

        operationsMap.put(new OperationKey(aClass, anExtensionClass, anOperationName), anOperation);
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
     * Otherwise, a dynamic extension having no operations will be returned.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionClass class of extension object to be returned
     * @return an extension object
     */
    @SuppressWarnings({"unchecked"})
    public <T> T extension(Object anObject, Class<T> anExtensionClass) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionClass);

        return (T) Proxy.newProxyInstance(anExtensionClass.getClassLoader(),
                new Class<?>[]{anExtensionClass},
                (proxy, method, args) -> performOperation(anObject, anExtensionClass, method, args));
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
            Object operation = findExtensionOperation(anObject, anExtensionClass, method);
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
    <T> Object findExtensionOperation(Object anObject, Class<T> anExtensionClass, Method method) {
        Object result;

        Class current = anObject.getClass();
        do {
            result = getExtensionOperation(current, anExtensionClass, method.getName());
            current = current.getSuperclass();
        } while (current != null && result == null);
        return result;
    }

    @SuppressWarnings({"rawtypes"})
    record OperationKey(Class clazz, Class extensionClass, String operationName) {
    }

    private final ConcurrentHashMap<OperationKey, Object> operationsMap = new ConcurrentHashMap<>();
    private static final DynamicClassExtension dynamicClassExtension = new DynamicClassExtension();

    public static DynamicClassExtension sharedInstance() {
        return dynamicClassExtension;
    }

    public static <T, E> Builder<E> sharedBuilder(Class<E> aExtensionClass) {
        return dynamicClassExtension.builder(aExtensionClass);
    }

    public <T, E> Builder<E> builder(Class<E> aExtensionClass) {
        return new Builder<E>(aExtensionClass, this);
    }

    public static class Builder<E> {
        private DynamicClassExtension dynamicClassExtension = new DynamicClassExtension();
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
