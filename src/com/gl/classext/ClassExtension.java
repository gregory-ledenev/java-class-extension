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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * <p>Class {@code ClassExtension} provides a way to mimic class extensions (categories) by finding matching extension objects and
 * using them to perform any extended functionality.</p>
 * <p>For example: lets imagine a {@code Shape} class that provides only coordinates and dimensions of shapes. If we need to
 * introduce a drawable shape we can create a {@code Shape(Drawable)} class extension with the {@code draw()} method, and we can call
 * the {@code draw()} method as simple as {@code new Shape().draw()}. Though such kind of extension is not available in Java the
 * {@code ClassExtension} provides a way to simulate that. To do it we should introduce an extension class
 * {@code Shape_Drawable} with the {@code draw()} method. Now we can call the {@code draw()} method as simple as
 * {@code ClassExtension.extension(new Shape(), Shape_Drawable.class).draw()}.</p>
 * <pre><code>
 *     class Shape {
 *         // some properties and methods here
 *     }
 *     ...more shape classes here...
 *
 *     class Shape_Drawable implements DelegateHolder {
 *         private Shape delegate;
 *         public void draw() {
 *             // use delegate properties to draw a shape
 *         }
 *         public Shape getDelegate() {
 *             return delegate;
 *         }
 *         public void setDelegate(Shape aDelegate) {
 *             delegate = aDelegate;
 *         }
 *     }
 *     ...more shape drawable class extensions here
 *
 *     class ShapesView {
 *         void drawShapes() {
 *             List&lt;Shape&gt; shapes = ...
 *             for (Shape shape : shapes)
 *              ClassExtension.extension(shape, Shape_Drawable.class).draw();
 *         }
 *     }
 *     </code></pre>
 *
 * <p>All the extension classes must implement the DelegateHolder interface and must end with the name of an extension delimited by underscore
 * e.g. Shape_Drawable where shape is the name of the class and Drawable is the name of extension</p>
 *
 * <p>{@code ClassExtension} takes care of inheritance so it is possible to design and implement class extensions hierarchy
 * that fully or partially resembles original classes' hierarchy. If there's no explicit extension specified for particular
 * class - its parent extension will be utilised. For example, if there's no explicit {@code Drawable} extension for
 * {@code Oval} objects - base {@code Shape_Drawable} will be used instead.</p>
 *
 * <p>Cashing of extension objects are supported out of the box. Cache utilises weak references to release extension objects
 * that are not in use. Though, to perform full cleanup either the cacheCleanup() should be used or automatic cleanup can
 * be initiated via the scheduleCacheCleanup(). If automatic cache cleanup is used - it can be stopped by calling the
 * shutdownCacheCleanup().</p>
 */
public class ClassExtension {

    /**
     * The interface all the class extensions must implement. It defines the 'delegate' property which is used to supply
     * extension objects with values to work with
     *
     * @param <T> defines a class of delegate objects to work with
     */
    public interface DelegateHolder<T> {
        T getDelegate();

        void setDelegate(T aDelegate);
    }

    /**
     * Finds and returns an extension object according to a supplied class. It uses cache to avoid redundant objects
     * creation.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionClass class of extension object to be returned
     * @return an extension object
     */
    @SuppressWarnings({"rawtypes"})
    public static <T extends DelegateHolder> T extension(Object anObject, Class<T> anExtensionClass) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionClass);

        return extension(anObject, anExtensionClass, null);
    }

    /**
     * Finds and returns an extension object according to a supplied class. It uses cache to avoid redundant objects
     * creation.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionClass class of extension object to be returned
     * @param aPackageNames    additional packages to lookup for extensions
     * @return an extension object
     */
    @SuppressWarnings({"rawtypes"})
    public static <T extends DelegateHolder> T extension(Object anObject, Class<T> anExtensionClass, List<String> aPackageNames) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionClass);

        return extension(anObject, extensionName(anExtensionClass), getPackageNames(anExtensionClass, aPackageNames));
    }

    @SuppressWarnings({"rawtypes"})
    private static <T extends DelegateHolder> List<String> getPackageNames(Class<T> anExtensionClass, List<String> aPackageNames) {
        List<String> packageNames = new ArrayList<>();
        packageNames.add(anExtensionClass.getPackageName());
        if (aPackageNames != null)
            packageNames.addAll(aPackageNames);
        return packageNames;
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
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionClass);

        return extensionNoCache(anObject, anExtensionClass, null);
    }

    /**
     * Finds and returns an extension object according to a supplied class.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionClass class of extension object to be returned
     * @param aPackageNames    additional packages to lookup for extensions
     * @return an extension object
     */
    @SuppressWarnings({"rawtypes"})
    public static <T extends DelegateHolder> T extensionNoCache(Object anObject, Class<T> anExtensionClass, List<String> aPackageNames) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionClass);

        return extensionNoCache(anObject, extensionName(anExtensionClass), getPackageNames(anExtensionClass, aPackageNames));
    }

    /**
     * Finds and returns an extension object according to a supplied extension name and an extension package.
     *
     * @param anObject        object to return an extension object for
     * @param anExtensionName name of extension
     * @param aPackageNames   packages to lookup extension in
     * @return an extension object
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends DelegateHolder> T extensionNoCache(Object anObject, String anExtensionName, List<String> aPackageNames) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionName);

        Class<T> extensionClass = extensionClassForObject(anObject, anExtensionName, aPackageNames);
        if (extensionClass == null)
            throw new IllegalArgumentException(MessageFormat.format("No extension {0} for a {1} class",
                    extensionNames(aPackageNames, anObject.getClass().getSimpleName(), anExtensionName),
                    anObject.getClass().getName()));
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
     * @param aPackageNames   packages to lookup extension in
     * @return an extension object
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends DelegateHolder> T extension(Object anObject, String anExtensionName, List<String> aPackageNames) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionName);

        return (T) extensionCache.getOrCreate(anObject, () -> extensionNoCache(anObject, anExtensionName, aPackageNames));
    }

    static <T> Class<T> extensionClassForObject(Object anObject, String anExtensionName, List<String> aPackageNames) {
        if (aPackageNames == null)
            return null;

        Class<T> result = null;
        List<String> packageNames = new ArrayList<>(aPackageNames);
        Collections.reverse(packageNames);

        for (String packageName : packageNames) {
            result = extensionClassForObject(anObject, anExtensionName, packageName);
            if (result != null)
                break;
        }
        return result;
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

    static String extensionNames(List<String> aPackageNames, String aSimpleClassName, String extensionName) {
        if (aPackageNames == null)
            return null;

        StringBuilder result = new StringBuilder();
        for (String packageName : aPackageNames) {
            if (result.isEmpty())
                result.append(", ");
            result.append(extensionName(packageName, aSimpleClassName, extensionName));
        }
        return result.toString();
    }

    static <T> String extensionName(Class<T> clazz) {
        int index = clazz.getName().indexOf("_");
        if (index == -1) throw new IllegalArgumentException("Class " + clazz.getName() + " is not an extension");
        return clazz.getName().substring(index + 1);
    }

    //region Cache methods

    @SuppressWarnings("rawtypes")
    static final ThreadSafeWeakCache extensionCache = new ThreadSafeWeakCache();

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

    /**
     * Check if cache is empty
     * @return true if cache is empty; false otherwise
     */
    static boolean cacheIsEmpty() {
        return extensionCache.isEmpty();
    }
    //endregion
}
