package com.gl.classext;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public class PropertyValueSupport {
    private final ConcurrentHashMap<String, Method> methodHandleCache = new ConcurrentHashMap<>();

    public Object getPropertyValue(Object target, String propertyName) {
        Method handle = getOrCreateMethodHandle(target.getClass(), propertyName);
        try {
            return handle.invoke(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Method getOrCreateMethodHandle(Class<?> targetClass, String propertyName) {
        String key = targetClass.getName() + "#" + propertyName;
        return methodHandleCache.computeIfAbsent(key, k -> createMethodHandle(targetClass, propertyName));
    }

    public static final String[] JB_METHOD_PREFIXES_CLASS = {"get", "is", null};
    public static final String[] JB_METHOD_PREFIXES_RECORD = {null, "get", "is"};

    private Method createMethodHandle(Class<?> targetClass, String propertyName) {
        if (propertyName == null || propertyName.isEmpty()) {
            throw new IllegalArgumentException("Property name must not be null or empty");
        }
        Method result = null;
        String[] prefixes = targetClass.isRecord() ? JB_METHOD_PREFIXES_RECORD : JB_METHOD_PREFIXES_CLASS;
        for (String prefix : prefixes) {
            try {
                final String name;
                if (prefix != null) name = prefix +
                        Character.toUpperCase(propertyName.charAt(0)) +
                        propertyName.substring(1);
                else {
                    name = propertyName;
                }

                result = targetClass.getMethod(name);
                break;
            } catch (NoSuchMethodException e) {
                // Expected, continue to the next prefix
            }
        }
        if (result == null)
            throw new RuntimeException("Failed to find accessor method for property '" + propertyName + "' in class " + targetClass.getName());
        return result;
    }
}
