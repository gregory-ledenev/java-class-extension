package com.gl.classext;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class DynamicClassExtension {
    public <R, T, E> Function<T, R> addExtensionOperation(Class<T> aClass,
                                                          Class<E> anExtensionClass,
                                                          String anOperationName,
                                                          Function<T, R> anOperation) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionClass);
        Objects.requireNonNull(anOperationName);
        Objects.requireNonNull(anOperation);

        operationsMap.put(new OperationKey(aClass, anExtensionClass, anOperationName), anOperation);
        return anOperation;
    }

    public <T, E> Consumer<T> addVoidExtensionOperation(Class<T> aClass,
                                                    Class<E> anExtensionClass,
                                                    String anOperationName,
                                                    Consumer<T> anOperation) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionClass);
        Objects.requireNonNull(anOperationName);
        Objects.requireNonNull(anOperation);

        operationsMap.put(new OperationKey(aClass, anExtensionClass, anOperationName), anOperation);
        return anOperation;
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
                if (void.class.equals(method.getReturnType()))
                    ((Consumer) operation).accept(anObject);
                else
                    result = ((Function) operation).apply(anObject);
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

    public static class Builder<T, E> {
        private DynamicClassExtension dynamicClassExtension = new DynamicClassExtension();
        private final Class<T> objectClass;
        private final Class<E> extensionClass;
        private String operationName;

        Builder(Class<T> aObjectClass, Class<E> aExtensionClass, String aOperationName, DynamicClassExtension aDynamicClassExtension) {
            objectClass = aObjectClass;
            extensionClass = aExtensionClass;
            operationName = aOperationName;
            dynamicClassExtension = aDynamicClassExtension;
        }

        public Builder(Class<T> aObjectClass, Class<E> aExtensionClass) {
            objectClass = aObjectClass;
            extensionClass = aExtensionClass;
        }

        public Builder<T, E> operationName(String anOperationName) {
            return new Builder<>(objectClass, extensionClass, anOperationName, dynamicClassExtension);
        }

        public <R, T1> Builder<T, E> operation(Class<T1> anObjectClass, Function<T1, R> anOperation) {
            dynamicClassExtension.addExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(objectClass, extensionClass, operationName, dynamicClassExtension);
        }

        public <T1> Builder<T, E> voidOperation(Class<T1> anObjectClass, Consumer<T1> anOperation) {
            dynamicClassExtension.addVoidExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(objectClass, extensionClass, operationName, dynamicClassExtension);
        }

        public DynamicClassExtension build() {
            return dynamicClassExtension;
        }
    }
}
