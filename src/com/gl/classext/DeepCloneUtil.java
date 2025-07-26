
/*
Copyright 2024 Gregory Ledenev (gregory.ledenev37@gmail.com)

MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the “Software”), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package com.gl.classext;

import java.lang.reflect.*;
import java.util.*;

public class DeepCloneUtil {

    /**
     * Deep clones an object handling cloneable and immutable objects. Note: all collections will be cloned to their
     * unmodifiable view.
     *
     * @param original the object to clone
     * @param <T>      the type of the object
     * @return a deep clone of the original object
     */
    public static <T> T deepClone(T original) {
        return deepClone(original, false);
    }

    /**
     * Deep clones an object handling cloneable and immutable objects. Note: all collections will be cloned to their
     * unmodifiable view.
     *
     * @param original         the object to clone
     * @param copyImmutable    whether to clone immutable objects or return the original
     * @param <T>              the type of the object
     * @return a deep clone of the original object
     */
    public static <T> T deepClone(T original, boolean copyImmutable) {
        return deepClone(original, new Stack<>(), copyImmutable);
    }

    /**
     * Deep clones an object handling cloneable and immutable objects. Note: all collections will be cloned to their
     * unmodifiable view.
     *
     * @param original         the object to clone
     * @param callStack        a stack to track circular references
     * @param copyImmutable    whether to clone immutable objects or return the original
     * @param <T>              the type of the object
     * @return a deep clone of the original object
     */
    @SuppressWarnings("unchecked")
    private static <T> T deepClone(T original, Stack<Object> callStack, boolean copyImmutable) {
        if (original == null)
            return null;

        if (callStack.contains(original))
            throw new IllegalArgumentException("Circular reference detected for: " + original);
        callStack.push(original);

        try {
            Class<?> clazz = original.getClass();

            if (isImmutable(clazz))
                return copyImmutable ? copyImmutableObject(original) : original;

            switch (original) {
                case List<?> originalList -> {
                    return (T) copyList(originalList, callStack, copyImmutable);
                }
                case Set<?> originalSet -> {
                    return (T) copySet(originalSet, callStack, copyImmutable);
                }
                case Map<?, ?> originalMap -> {
                    return (T) copyMap(originalMap, callStack, copyImmutable);
                }
                default -> {
                }
            }

            if (clazz.isArray())
                return (T) copyArray(original, callStack, copyImmutable);

            Object clonedOrCopied = tryClone(original);
            if (clonedOrCopied != null)
                return (T) clonedOrCopied;

            clonedOrCopied = tryCopyConstructor(original);
            if (clonedOrCopied != null)
                return (T) clonedOrCopied;

            throw new UnsupportedOperationException("Deep copy not supported for class: " + clazz.getName());
        } finally {
            callStack.pop();
        }
    }

    @SuppressWarnings({"unchecked", "BoxingBoxedValue"})
    private static <T> T copyImmutableObject(T original) {
        Class<?> clazz = original.getClass();

        switch (original) {
            case String s -> {
                return (T) new String(s);
            }
            case Integer i -> {
                return (T) Integer.valueOf(i);
            }
            case Long l -> {
                return (T) Long.valueOf(l);
            }
            case Float f -> {
                return (T) Float.valueOf(f);
            }
            case Double d -> {
                return (T) Double.valueOf(d);
            }
            case Boolean b -> {
                return (T) Boolean.valueOf(b);
            }
            case Character c -> {
                return (T) Character.valueOf(c);
            }
            case Byte c -> {
                return (T) Byte.valueOf(c);
            }
            case Short s -> {
                return (T) Short.valueOf(s);
            }
            default -> {
            }
        }

        if (clazz.isEnum())
            return original;

        return original;
    }

    private static <T> List<T> copyList(List<T> originalList, Stack<Object> callStack, boolean copyImmutable) {
        List<T> copyList = new ArrayList<>(originalList.size());
        for (T item : originalList)
            copyList.add(deepClone(item, callStack, copyImmutable));

        return Collections.unmodifiableList(copyList);
    }

    private static <T> Set<T> copySet(Set<T> originalSet, Stack<Object> callStack, boolean copyImmutable) {
        Set<T> copySet = new HashSet<>();
        for (T item : originalSet)
            copySet.add(deepClone(item, callStack, copyImmutable));

        return Collections.unmodifiableSet(copySet);
    }

    private static <K, V> Map<K, V> copyMap(Map<K, V> originalMap, Stack<Object> callStack, boolean copyImmutable) {
        Map<K, V> copyMap = new HashMap<>();
        for (Map.Entry<K, V> entry : originalMap.entrySet()) {
            K copiedKey = deepClone(entry.getKey(), callStack, copyImmutable);
            V copiedValue = deepClone(entry.getValue(), callStack, copyImmutable);
            copyMap.put(copiedKey, copiedValue);
        }

        return Collections.unmodifiableMap(copyMap);
    }

    private static Object copyArray(Object originalArray, Stack<Object> callStack, boolean copyImmutable) {
        int length = Array.getLength(originalArray);
        Class<?> componentType = originalArray.getClass().getComponentType();
        Object copyArray = Array.newInstance(componentType, length);
        for (int i = 0; i < length; i++) {
            Object element = Array.get(originalArray, i);
            Object copiedElement = deepClone(element, callStack, copyImmutable);
            Array.set(copyArray, i, copiedElement);
        }

        return copyArray;
    }

    private static Object tryClone(Object original) {
        try {
            Method cloneMethod = original.getClass().getMethod("clone");
            if (Modifier.isPublic(cloneMethod.getModifiers()))
                return cloneMethod.invoke(original);
        } catch (Exception e) {
            // do nothing, just return null
        }

        return null;
    }

    private static Object tryCopyConstructor(Object original) {
        try {
            Constructor<?> constructor = original.getClass().getConstructor(original.getClass());
            return constructor.newInstance(original);
        } catch (Exception e) {
            // do nothing, just return null
        }

        return null;
    }

    private static boolean isImmutable(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz.equals(String.class)
                || Number.class.isAssignableFrom(clazz)
                || clazz.equals(Boolean.class)
                || clazz.isEnum()
                || clazz.equals(Character.class)
                || clazz.equals(Byte.class)
                || clazz.equals(Short.class)
                || clazz.equals(Integer.class)
                || clazz.equals(Long.class)
                || clazz.equals(Float.class)
                || clazz.equals(Double.class);
    }
}
