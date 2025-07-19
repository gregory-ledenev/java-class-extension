package com.gl.classext;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * PropertyValueSupport is a utility class that provides a way to access property values
 * of an object using reflection. It supports caching of method handles for improved performance.
 * It can handle both regular classes and records, and it uses standard JavaBean naming conventions
 * for getter methods.
 */
public class PropertyValueSupport {
    private final boolean useCache;
    private final ConcurrentHashMap<String, Method> gettersMethodCache = new ConcurrentHashMap<>();
    private static final AtomicReference<PropertyValueSupport> sharedInstance = new AtomicReference<>();

    /**
     * Creates a new instance of PropertyValueSupport with caching enabled.
     */
    public PropertyValueSupport() {
        this(true);
    }

    /**
     * Creates a new instance of PropertyValueSupport with the option to enable or disable caching.
     *
     * @param aUseCache true to enable caching, false to disable it
     */
    public PropertyValueSupport(boolean aUseCache) {
        useCache = aUseCache;
    }

    /**
     * Returns a shared instance of PropertyValueSupport, creating it if it does not already exist.
     * Caching is disabled for the shared instance to avoid issues with class loaders.
     *
     * @return a shared instance of PropertyValueSupport
     */
    public static PropertyValueSupport sharedInstance() {
        return sharedInstance.updateAndGet(p -> p != null ? p : new PropertyValueSupport());
    }


    public Object getPropertyValue(Object target, String propertyName) {
        Method handle = getterMethod(getTargetClass(target), propertyName);
        try {
            return handle.invoke(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> getTargetClass(Object target) {
        Class<?> result = target.getClass();

        // avoid using internal classes that cant be accessed from outside
        // casting them down to a more general type
        if (! Modifier.isPublic(result.getModifiers())) {
            if (List.class.isAssignableFrom(result))
                result = List.class;
            else if (Set.class.isAssignableFrom(result))
                result = Set.class;
            else if (Map.class.isAssignableFrom(result))
                result = Map.class;
        }

        return result;
    }

    private Method getterMethod(Class<?> targetClass, String propertyName) {
        if (useCache) {
            String key = targetClass.getName() + "#" + propertyName;
            return gettersMethodCache.computeIfAbsent(key, k -> findGetterMethod(targetClass, propertyName));
        } else {
            return findGetterMethod(targetClass, propertyName);
        }
    }

    public static final String[] JB_METHOD_PREFIXES_CLASS = {"get", "is", null};
    public static final String[] JB_METHOD_PREFIXES_RECORD = {null, "get", "is"};

    private Method findGetterMethod(Class<?> targetClass, String propertyName) {
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

    /**
     * Converts a getter method name to a property name according to JavaBean naming conventions.
     * For example, "getName" becomes "name", and "isActive" becomes "active".
     *
     * @param getterName the name of the getter method
     * @return the corresponding property name
     */
    public static String getPropertyNameForGetterName(String getterName) {
        if (getterName.startsWith("get")) {
            return Character.toLowerCase(getterName.charAt(3)) + getterName.substring(4);
        }
        if (getterName.startsWith("is")) {
            return Character.toLowerCase(getterName.charAt(2)) + getterName.substring(3);
        }
        return getterName;
    }

    //region Setters support

    /**
     * Sets the value of a property on an object using reflection.
     * The property name should follow JavaBean naming conventions.
     *
     * @param object   the object on which to set the property
     * @param property the name of the property to set
     * @param value    the value to set; can be null
     */
    public void setPropertyValue(Object object, String property, Object value) {
        try {
            String setter = "set" + property.substring(0, 1).toUpperCase() + property.substring(1);
            Method method;
            if (value != null) {
                Class<?> valueClassForSetter = getValueClassForSetter(value);
                method = setterMethod(object, setter, valueClassForSetter);
            } else {
                method = anySetterMethod(object.getClass(), setter);
            }

            method.invoke(object, value);
        } catch (Exception e) {
            throw new RuntimeException("Error setting value at: " + property, e);
        }
    }

    /**
     * Sets a value in an array at the specified index.
     * The array must be an object array or a primitive array.
     *
     * @param object the array object
     * @param i      the index at which to set the value
     * @param value  the value to set; can be null for object arrays
     */
    public static void setArrayValue(Object object, int i, Object value) {
        Class<?> componentType = object.getClass().getComponentType();
        if (componentType.isPrimitive()) {
            if (boolean.class.equals(componentType))
                Array.setBoolean(object, i, (Boolean) value);
            else if (byte.class.equals(componentType))
                Array.setByte(object, i, (Byte) value);
            else if (char.class.equals(componentType))
                Array.setChar(object, i, (Character) value);
            else if (short.class.equals(componentType))
                Array.setShort(object, i, (Short) value);
            else if (int.class.equals(componentType))
                Array.setInt(object, i, (Integer) value);
            else if (long.class.equals(componentType))
                Array.setLong(object, i, (Long) value);
            else if (float.class.equals(componentType))
                Array.setFloat(object, i, (Float) value);
            else if (double.class.equals(componentType))
                Array.setDouble(object, i, (Double) value);
        } else {
            ((Object[]) object)[i] = value;
        }
    }

    private final Map<String, Method> settersMethodCache = new ConcurrentHashMap<>();

    private Method setterMethod(Object object, String setter, Class<?> parameterType) {
        String key = object.getClass().getName() + "#" + setter + "#" + parameterType.getName();
        return settersMethodCache.computeIfAbsent(key,
                k -> {
                    try {
                        return object.getClass().getMethod(setter, parameterType);
                    } catch (NoSuchMethodException aE) {
                        throw new RuntimeException(aE);
                    }
                });
    }

    private Method anySetterMethod(Class<?> clazz, String methodName) {
        String key = clazz.getName() + "#" + methodName;
        return settersMethodCache.computeIfAbsent(key, k -> {
            List<Method> methods = Arrays.stream(clazz.getMethods())
                    .filter(method -> method.getName().equals(methodName) &&
                            method.getParameterCount() == 1 &&
                            !method.getParameterTypes()[0].isPrimitive())
                    .toList();

            if (methods.size() > 1) {
                throw new IllegalStateException("Multiple matching methods found for: " + methodName);
            }

            return methods.isEmpty() ? null : methods.get(0);
        });
    }

    private static Class<?> getValueClassForSetter(Object value) {
        return switch (value) {
            case Byte _ -> byte.class;
            case Short _ -> short.class;
            case Integer _ -> int.class;
            case Long _ -> long.class;
            case Float _ -> float.class;
            case Double _ -> double.class;
            case Character _ -> char.class;
            case Boolean _ -> boolean.class;
            default -> value.getClass();
        };
    }
//endregion
}
