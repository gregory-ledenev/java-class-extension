
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
import java.util.function.Function;


public class DeepCloneUtil {

    /**
     * Deep clones an object handling cloneable and immutable objects. Note: all collections will be cloned to their
     * unmodifiable view.
     *
     * @param anOriginal the object to clone
     * @param <T>      the type of the object
     * @return a deep clone of the anOriginal object
     */
    public static <T> T deepClone(T anOriginal) {
        return deepClone(anOriginal, null, false);
    }

    /**
     * Deep clones an object handling cloneable and immutable objects. Note: all collections will be cloned to their
     * unmodifiable view.
     *
     * @param anOriginal      the object to clone
     * @param aCloner         a function to apply for cloning the object, can be null
     * @return a deep clone of the anOriginal object
     */
    public static <T> T deepClone(T anOriginal, Function<Object, Object> aCloner) {
        return deepClone(anOriginal, aCloner, false);
    }

    /**
     * Deep clones an object handling cloneable and immutable objects. Note: all collections will be cloned to their
     * unmodifiable view.
     *
     * @param anOriginal      the object to clone
     * @param aCloner         a function to apply for cloning the object, can be null
     * @param isCopyImmutable whether to clone immutable objects or return the anOriginal
     * @return a deep clone of the anOriginal object
     */
    public static <T> T deepClone(T anOriginal, Function<Object, Object> aCloner, boolean isCopyImmutable) {
        return deepClone(new Stack<>(), anOriginal, aCloner, isCopyImmutable);
    }

    /**
     * Deep clones an object handling cloneable and immutable objects. Note: all collections will be cloned to their
     * unmodifiable view.
     *
     * @param aCloner         a function to apply for cloning the object, can be null
     * @param aCallStack      a stack to track circular references
     * @param <T>             the type of the object
     * @param anOriginal      the object to clone
     * @param isCopyImmutable whether to clone immutable objects or return the anOriginal
     * @return a deep clone of the anOriginal object
     */
    @SuppressWarnings("unchecked")
    private static <T> T deepClone(Stack<Object> aCallStack, T anOriginal, Function<Object, Object> aCloner, boolean isCopyImmutable) {
        if (anOriginal == null)
            return null;

        if (aCallStack.contains(anOriginal))
            throw new IllegalArgumentException("Circular reference detected for: " + anOriginal);
        aCallStack.push(anOriginal);

        try {
            Class<?> clazz = anOriginal.getClass();

            if (isImmutable(clazz))
                return isCopyImmutable ? copyImmutableObject(anOriginal) : anOriginal;

            switch (anOriginal) {
                case List<?> originalList -> {
                    return (T) copyList(aCallStack, originalList, aCloner, isCopyImmutable);
                }
                case Set<?> originalSet -> {
                    return (T) copySet(aCallStack, originalSet, aCloner, isCopyImmutable);
                }
                case Map<?, ?> originalMap -> {
                    return (T) copyMap(aCallStack, originalMap, aCloner, isCopyImmutable);
                }
                default -> {
                }
            }

            if (clazz.isArray())
                return (T) copyArray(aCallStack, anOriginal, aCloner, isCopyImmutable);

            Object clone = aCloner != null ? aCloner.apply(anOriginal) : null;

            if (clone == null) {
                clone = tryClone(anOriginal);
                if (clone != null)
                    return (T) clone;

                clone = tryCopyConstructor(anOriginal);
            }
            if (clone != null)
                return (T) clone;

            throw new UnsupportedOperationException("Deep copy not supported for class: " + clazz.getName());
        } finally {
            aCallStack.pop();
        }
    }

    @SuppressWarnings({"unchecked", "BoxingBoxedValue"})
    private static <T> T copyImmutableObject(T anOriginal) {
        switch (anOriginal) {
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

        return anOriginal;
    }

    private static <T> List<T> copyList(Stack<Object> aCallStack, List<T> anOriginalList, Function<Object, Object> aCloner, boolean isCopyImmutable) {
        List<T> copyList = new ArrayList<>(anOriginalList.size());
        for (T item : anOriginalList)
            copyList.add(deepClone(aCallStack, item, aCloner, isCopyImmutable));

        return Collections.unmodifiableList(copyList);
    }

    private static <T> Set<T> copySet(Stack<Object> aCallStack, Set<T> anOriginalSet, Function<Object, Object> aCloner, boolean isCopyImmutable) {
        Set<T> copySet = new HashSet<>();
        for (T item : anOriginalSet)
            copySet.add(deepClone(aCallStack, item, aCloner, isCopyImmutable));

        return Collections.unmodifiableSet(copySet);
    }

    private static <K, V> Map<K, V> copyMap(Stack<Object> aCallStack, Map<K, V> anOriginalMap, Function<Object, Object> aCloner, boolean isCopyImmutable) {
        Map<K, V> copyMap = new HashMap<>();
        for (Map.Entry<K, V> entry : anOriginalMap.entrySet()) {
            K copiedKey = deepClone(aCallStack, entry.getKey(), aCloner, isCopyImmutable);
            V copiedValue = deepClone(aCallStack, entry.getValue(), aCloner, isCopyImmutable);
            copyMap.put(copiedKey, copiedValue);
        }

        return Collections.unmodifiableMap(copyMap);
    }

    private static Object copyArray(Stack<Object> aCallStack, Object anOriginalArray, Function<Object, Object> aCloner, boolean isCopyImmutable) {
        int length = Array.getLength(anOriginalArray);
        Class<?> componentType = anOriginalArray.getClass().getComponentType();
        Object copyArray = Array.newInstance(componentType, length);
        for (int i = 0; i < length; i++) {
            Object element = Array.get(anOriginalArray, i);
            Object copiedElement = deepClone(aCallStack, element, aCloner, isCopyImmutable);
            Array.set(copyArray, i, copiedElement);
        }

        return copyArray;
    }

    private static Object tryClone(Object anOriginal) {
        try {
            Method cloneMethod = anOriginal.getClass().getMethod("clone");
            if (Modifier.isPublic(cloneMethod.getModifiers()))
                return cloneMethod.invoke(anOriginal);
        } catch (Exception e) {
            // do nothing, just return null
        }

        return null;
    }

    private static Object tryCopyConstructor(Object anOriginal) {
        try {
            Constructor<?> constructor = anOriginal.getClass().getConstructor(anOriginal.getClass());
            return constructor.newInstance(anOriginal);
        } catch (Exception e) {
            // do nothing, just return null
        }

        return null;
    }

    private static boolean isImmutable(Class<?> aClass) {
        return aClass.isPrimitive()
                || aClass.equals(String.class)
                || Number.class.isAssignableFrom(aClass)
                || aClass.equals(Boolean.class)
                || aClass.isEnum()
                || aClass.equals(Character.class)
                || aClass.equals(Byte.class)
                || aClass.equals(Short.class)
                || aClass.equals(Integer.class)
                || aClass.equals(Long.class)
                || aClass.equals(Float.class)
                || aClass.equals(Double.class);
    }
}
