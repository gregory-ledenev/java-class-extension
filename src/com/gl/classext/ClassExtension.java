/*
Copyright 2024 Gregory Ledenev (gregory.ledenev37@gmail.com)

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

import java.text.MessageFormat;

public class ClassExtension {

    @SuppressWarnings({"rawtypes"})
    private static final ThreadSafeWeakCache extensionCache = new ThreadSafeWeakCache();


    public interface DelegateHolder<T> {
        public T getDelegate();

        public void setDelegate(T aDelegate);
    }

    /**
     * Finds and returns an extension object according to a supplied class. It uses cache to avoid redundant objects creation.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionClass class of extension object to be returned
     * @return an extension object
     */
    @SuppressWarnings({"rawtypes"})
    public static <T extends DelegateHolder> T extension(Object anObject, Class<T> anExtensionClass) {
        return extension(anObject, extensionName(anExtensionClass), anExtensionClass.getPackageName());
    }

    /**
     * Finds and returns an extension object according to a supplied class.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionClass class of extension object to be returned
     * @return an extension object
     */
    @SuppressWarnings({"rawtypes"})
    public static <T extends DelegateHolder> T extensionNoCache(Object anObject, Class<T> anExtensionClass) {
        return extensionNoCache(anObject, extensionName(anExtensionClass), anExtensionClass.getPackageName());
    }

    /**
     * Finds and returns an extension object according to a supplied extension name and an extension package.
     *
     * @param anObject        object to return an extension object for
     * @param anExtensionName name of extension
     * @param aPackageName    package to find extension in
     * @return an extension object
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends DelegateHolder> T extensionNoCache(Object anObject, String anExtensionName, String aPackageName) {
        Class<T> extensionClass = extensionClassForObject(anObject, anExtensionName, aPackageName);
        if (extensionClass == null)
            throw new IllegalArgumentException(MessageFormat.format("No extension {0} for a {1} class", extensionName(aPackageName, anObject.getClass().getSimpleName(), anExtensionName), anObject.getClass().getName()));
        try {
            T result = extensionClass.getDeclaredConstructor().newInstance();
            result.setDelegate(anObject);
            return result;
        } catch (Exception aE) {
            throw new RuntimeException(aE);
        }
    }

    /**
     * Finds and returns an extension object according to a supplied extension name and an extension package.
     * It uses cache to avoid redundant objects creation.
     *
     * @param anObject        object to return an extension object for
     * @param anExtensionName name of extension
     * @param aPackageName    package to find extension in
     * @return an extension object
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends DelegateHolder> T extension(Object anObject, String anExtensionName, String aPackageName) {
        return (T) extensionCache.getOrCreate(anObject, () -> extensionNoCache(anObject, anExtensionName, aPackageName));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static <T> Class<T> extensionClassForObject(Object anObject, String anExtensionName, String aPackageName) {
        Class<T> result = null;
        Class current = anObject.getClass();
        do {
            try {
                String fullClassName = current.getName();
                int index = fullClassName.lastIndexOf(".");
                String className = index != -1 ? fullClassName.substring(index + 1) : fullClassName;
                result = (Class<T>) Class.forName(extensionName(aPackageName, className, anExtensionName));
            } catch (Exception aE) {
                // nothing to do; just walk up
            }
            current = current.getSuperclass();
        } while (current != null && result == null);

        return result;
    }

    static String extensionName(String aPackageName, String aSimpleClassName, String extensionName) {
        return MessageFormat.format("{0}.{1}_{2}", aPackageName, aSimpleClassName, extensionName);
    }

    static <T> String extensionName(Class<T> clazz) {
        int index = clazz.getName().indexOf("_");
        if (index == -1) throw new IllegalArgumentException("Class " + clazz.getName() + " is not an extension");
        return clazz.getName().substring(index + 1);
    }

    /**
     * Cleanups cache by removing keys for all already garbage collected values
     */
    public static void cacheCleanup() {
        extensionCache.cleanup();
    }

    /**
     * Clears cache
     */
    public static void cacheClear() {
        extensionCache.clear();
    }

    /**
     * Schedules automatic cache cleanup that should be performed once a minute
     */
    public static void scheduleCacheCleanup() {
        extensionCache.scheduleCleanup();
    }

    /**
     * Shutdowns automatic cache cleanup
     */
    public static void shutdownCacheCleanup() {
        extensionCache.shutdownCleanup();
    }
}
