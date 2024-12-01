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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

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
 *
 * @author Gregory Ledenev
 * @version 0.9.6
 */
public class StaticClassExtension implements ClassExtension{

    private static final StaticClassExtension classExtension = new StaticClassExtension();

    public static StaticClassExtension sharedInstance() {
        return classExtension;
    }

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
     * @param anExtensionInterface class of extension object to be returned
     * @return an extension object
     */
    public <T> T extension(Object anObject, Class<T> anExtensionInterface) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionInterface);

        return extension(anObject, anExtensionInterface, getPackageNames(anExtensionInterface, null));
    }

    /**
     * Finds and returns a shared extension object according to a supplied class. It uses cache to avoid redundant objects
     * creation.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionClass class of extension object to be returned
     * @return an extension object
     */
    public static <T> T sharedExtension(Object anObject, Class<T> anExtensionClass) {
        return sharedInstance().extension(anObject, anExtensionClass);
    }

    /**
     * Finds and returns an extension object according to a supplied class. It uses cache to avoid redundant objects
     * creation.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionInterface class of extension object to be returned
     * @param aPackageNames    additional packages to lookup for extensions
     * @return an extension object
     */
    @SuppressWarnings("unchecked")
    public <T> T extension(Object anObject, Class<T> anExtensionInterface, List<String> aPackageNames) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionInterface);

        return (T) extensionCache.getOrCreate(anObject, () -> extensionNoCache(anObject, anExtensionInterface, aPackageNames));
    }

    /**
     * Finds and returns a shared extension object according to a supplied class. It uses cache to avoid redundant objects
     * creation.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionClass class of extension object to be returned
     * @param aPackageNames    additional packages to lookup for extensions
     * @return an extension object
     */
    @SuppressWarnings("unused")
    public static <T> T sharedExtension(Object anObject, Class<T> anExtensionClass, List<String> aPackageNames) {
        return sharedInstance().extension(anObject, anExtensionClass, aPackageNames);
    }

    private <T> List<String> getPackageNames(Class<T> anExtensionClass, List<String> aPackageNames) {
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
    public <T> T extensionNoCache(Object anObject, Class<T> anExtensionClass) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionClass);

        return extensionNoCache(anObject, anExtensionClass, getPackageNames(anExtensionClass, null));
    }

    /**
     * Finds and returns an extension object according to a supplied class.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionClass class of extension object to be returned
     * @return an extension object
     */
    public static <T> T sharedExtensionNoCache(Object anObject, Class<T> anExtensionClass) {
        return sharedInstance().extensionNoCache(anObject, anExtensionClass);
    }

    /**
     * Finds and returns a shared extension object according to a supplied class.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionInterface class of extension object to be returned
     * @param aPackageNames    additional packages to lookup for extensions
     * @return an extension object
     */
    @SuppressWarnings("unused")
    public static <T> T sharedExtensionNoCache(Object anObject, Class<T> anExtensionInterface, List<String> aPackageNames) {
        return sharedInstance().extensionNoCache(anObject, anExtensionInterface, aPackageNames);
    }

    /**
     * Finds and returns an extension object according to a supplied extension name and an extension package.
     *
     * @param anObject        object to return an extension object for
     * @param anExtensionInterface extension interface
     * @param aPackageNames   packages to lookup extension in
     * @return an extension object
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public <T> T extensionNoCache(Object anObject, Class<T> anExtensionInterface, List<String> aPackageNames) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionInterface);


        Class<?> extensionInterface = findAnnotatedSuperInterface(anExtensionInterface, ExtensionInterface.class);
        if (extensionInterface == null)
            extensionInterface = anExtensionInterface;


        Class<?> extensionClass = extensionClassForObject(anObject, extensionInterface, aPackageNames);
        if (extensionClass == null)
            throw new IllegalArgumentException(MessageFormat.format("No extension {0} for a {1} class",
                    extensionNames(aPackageNames, anObject.getClass().getSimpleName(), extensionInterface.getSimpleName()),
                    anObject.getClass().getName()));
        try {
            if (! extensionInterface.isAssignableFrom(extensionClass))
                throw new IllegalStateException(MessageFormat.format("Extension \"{0}\"class does not implement the \"{1}\" interface",
                        extensionClass.getName(), extensionInterface.getName()));

            Constructor<?> constructor = getDeclaredConstructor(extensionClass, anObject.getClass());
            Object extension = constructor.getParameterCount() == 0 ? constructor.newInstance() : constructor.newInstance(anObject);
            if (extension instanceof DelegateHolder)
                ((DelegateHolder) extension).setDelegate(anObject);

            final Class<?> finalExtensionInterface=  extensionInterface;
            return (T) Proxy.newProxyInstance(extensionInterface.getClassLoader(),
                    new Class<?>[]{anExtensionInterface},
                    (proxy, method, args) -> performOperation(extension, anObject, finalExtensionInterface, method, args));
        } catch (Exception aE) {
            throw new RuntimeException(aE);
        }
    }

    private static <T> Object performOperation(T anExtension, Object anObject, Class<?> anExtensionInterface, Method aMethod, Object[] aArgs)
            throws InvocationTargetException, IllegalAccessException {
        Object result;

        String methodName = aMethod.getName();
        if ("toString".equals(methodName) && aMethod.getParameterCount() == 0) {
            result = anObject.toString();
        } else if ("hashCode".equals(methodName) && aMethod.getParameterCount() == 0) {
            result = anObject.hashCode();
        } else if ("equals".equals(methodName) && aMethod.getParameterCount() == 1 && aMethod.getParameterTypes()[0] == Object.class) {
            result = anObject.equals(aArgs[0]);
        } else {
            if (aMethod.getDeclaringClass().isAssignableFrom(anExtension.getClass())) {
                // invoke extension method
                result = aMethod.invoke(anExtension, aArgs);
            } else if (aMethod.getDeclaringClass().isAssignableFrom(anObject.getClass())) {
                // invoke object method
                result = aMethod.invoke(anObject, aArgs);
            } else {
                throw new IllegalArgumentException("Unexpected method: " + methodName);
            }
        }

        return result;
    }

    static Class<?> findAnnotatedSuperInterface(Class<?> anInterfaceClass, Class<? extends Annotation> anAnnotationClass) {
        for (Class<?> superInterface : anInterfaceClass.getInterfaces()) {
            if (superInterface.isAnnotationPresent(anAnnotationClass)) {
                return superInterface;
            }
            Class<?> result = findAnnotatedSuperInterface(superInterface, anAnnotationClass);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked"})
    private static <T> Constructor<T> getDeclaredConstructor(Class<T> extensionClass, Class<?> anObjectClass) throws NoSuchMethodException {
        Constructor<T> result = null;
        for (Constructor<?> constructor : extensionClass.getDeclaredConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == 1 && parameterTypes[0].isAssignableFrom(anObjectClass)) {
                result = (Constructor<T>) constructor;
                break;
            }
        }

        return result == null ?
                extensionClass.getDeclaredConstructor() :
                result;
    }

    <T> Class<T> extensionClassForObject(Object anObject, Class<T> anExtensionInterface, List<String> aPackageNames) {
        if (aPackageNames == null)
            return null;

        Class<T> result = null;
        List<String> packageNames = new ArrayList<>(aPackageNames);
        Collections.reverse(packageNames);

        String extensionName = anExtensionInterface.getSimpleName();
        for (String packageName : packageNames) {
            result = extensionClassForObject(anObject, extensionName, packageName);
            if (result != null)
                break;
        }
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    <T> Class<T> extensionClassForObject(Object anObject, String anExtensionName, String aPackageName) {
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

    String extensionName(String aPackageName, String aSimpleClassName, String extensionName) {
        return MessageFormat.format("{0}.{1}{2}", aPackageName, aSimpleClassName, extensionName);
    }

    String extensionNames(List<String> aPackageNames, String aSimpleClassName, String extensionName) {
        if (aPackageNames == null)
            return null;

        StringBuilder result = new StringBuilder();
        for (String packageName : aPackageNames) {
            if (! result.isEmpty())
                result.append(", ");
            result.append(extensionName(packageName, aSimpleClassName, extensionName));
        }
        return result.toString();
    }

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
}
