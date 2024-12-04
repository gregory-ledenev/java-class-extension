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

/**
 * <p>The {@code StaticClassExtension} class offers methods for dynamically finding and creating extension objects as needed. With
 * this approach you should define and implement extensions as usual Java classes and then utilize the Java Class Extension
 * library to find matching extension classes and create extension objects using the {@code extension(Object Class)} method.</p>
 *
 * <p>For example, we can create a {@code Shippable} interface that defines new methods for a {@code Shippable} extension (category) and
 * provides a {@code ship()} method. Then we should implement all needed extension classes which implement the {@code ShippableInterface}
 * and provide particular implementation for all {@code Shippable} methods. Note: All those extension classes must either implement
 * the {@code DelegateHolder} interface to used to supply extensions with items to work with or provide a constructor that takes
 * an {@code Item} as a parameter.</p>
 *  <pre><code>
 * public interface Shippable {
 *     ShippingInfo ship();
 * }
 *
 * class ItemShippable implements Shippable {
 *     public ItemShippable(Item item) {
 *         this.item = item;
 *     }
 *
 *     public ShippingInfo ship() {
 *         return new ShippingInfo("done");
 *     }
 *     …
 * }
 *
 * class BookShippable extends ItemShippable{
 *     public ShippingInfo ship() {
 *         return new ShippingInfo("done");
 *     }
 * }
 * </code></pre>
 * Using {@code StaticClassExtension}, shipping an item becomes as simple as calling:
 *  <pre><code>
 * Book book = new Book("The Mythical Man-Month");
 * StaticClassExtension.sharedExtension(book, Shippable.class).ship();
 * </code></pre>
 * Shipping a collection of items is equally straightforward:
 *  <pre><code>
 * Item[] items = {
 *     new Book("The Mythical Man-Month"),
 *     new Furniture("Sofa"),
 *     new ElectronicItem(“Soundbar")
 * };
 *
 * for (Item item : items) {
 *     StaticClassExtension.sharedExtension(item, Shippable.class).ship();
 * }
 * </code></pre>
 * The following are requirements for all the static extension classes:
 *
 * <p>Extension classes must be named as <i>[ClassName][ExtensionName]</i> - a class name followed by an extension name.
 * For example, {@code BookShippable} where {@code Book} is the name of the class and {@code Shippable} is the name of
 * extension.</p>
 * <p>Extension classes must provide a way for {@code StaticClassExtension} to supply delegate objects to work with. It
 * can be done either:
 * <ul>
 *  <li>By providing a single parameter constructor that takes a delegate object to work with as an argument.</li>
 *  <li>By implementing the {@code DelegateHolder} interface. The {@code DelegateHolder.setDelegate(Object)} method is
 *  used to supply extensions ith delegate objects to work with. Usually, it is fine to implement the {@code DelegateHolder}
 *  interface in a common root of some classes' hierarchy.</li>
 * </ul>
 * </p>
 * <p>By default, {@code StaticClassExtension} searches for extension classes in the same package as the extension interface.
 * To register extension classes located in different packages, use the {@code addExtensionPackage(Class, String)} method of
 * {@code StaticClassExtension}. For example:
 *  <pre><code>
 * StaticClassExtension.sharedInstance().addExtensionPackage(Shippable.class, "test.toys.shipment");
 * </code></pre>
 * </p>
 * <p>Note: Extensions returned by {@code StaticClassExtension} do not directly correspond to the extension classes
 * themselves. Therefore, it is crucial not to cast these extensions. Instead, always utilize only the methods provided
 * by the extension interface.</p>
 *
 * @see <a href="https://github.com/gregory-ledenev/java-class-extension/blob/main/doc/static-class-extensions.md">More details</a>
 * @author Gregory Ledenev
 * @version 0.9.9
 */
public class StaticClassExtension implements ClassExtension {

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
     * {@inheritDoc}
     */
    public <T> T extension(Object anObject, Class<T> anExtensionInterface) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionInterface);

        return extension(anObject, anExtensionInterface, null);
    }

    /**
     * Convenience static method that finds and returns an extension object according to a supplied interface. It is the
     * same as calling {@code sharedInstance().extension(anObject, anExtensionInterface)} It uses cache to avoid
     * redundant objects creation. If no cache should be used - turn it OFF via the {@code setCacheEnabled(false)} call
     *
     * @param anObject             object to return an extension object for
     * @param anExtensionInterface interface of extension object to be returned
     * @return an extension object
     */
    public static <T> T sharedExtension(Object anObject, Class<T> anExtensionInterface) {
        return sharedInstance().extension(anObject, anExtensionInterface);
    }

    /**
     * Finds and returns an extension object according to a supplied class. It uses cache to avoid redundant objects
     * creation.
     *
     * @param anObject             object to return an extension object for
     * @param anExtensionInterface class of extension object to be returned
     * @param aPackageNames        additional packages to lookup for extensions
     * @return an extension object
     */
    @SuppressWarnings("unchecked")
    protected <T> T extension(Object anObject, Class<T> anExtensionInterface, List<String> aPackageNames) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionInterface);

        return isCacheEnabled() ?
                (T) extensionCache.getOrCreate(new ClassExtensionKey(anObject, anExtensionInterface), () -> extensionNoCache(anObject, anExtensionInterface, aPackageNames)) :
                extensionNoCache(anObject, anExtensionInterface, aPackageNames);
    }

    private <T> List<String> getPackageNames(Class<T> anExtensionClass, List<String> aPackageNames) {
        List<String> packageNames = new ArrayList<>();
        packageNames.add(anExtensionClass.getPackageName());
        if (aPackageNames != null)
            packageNames.addAll(aPackageNames);
        return packageNames;
    }

    /**
     * Finds and returns an extension object according to a supplied extension name and an extension package.
     *
     * @param anObject             object to return an extension object for
     * @param anExtensionInterface extension interface
     * @param aPackageNames        packages to lookup extension in
     * @return an extension object
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected <T> T extensionNoCache(Object anObject, Class<T> anExtensionInterface, List<String> aPackageNames) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionInterface);

        List<String> packageNames = getPackageNames(anExtensionInterface, aPackageNames);

        Class<?> extensionInterface = findAnnotatedInterface(anExtensionInterface, ExtensionInterface.class);
        if (extensionInterface == null) {
            extensionInterface = anExtensionInterface;
        } else {
            packageNames = getAnnotatedPackageNames(extensionInterface, packageNames);
        }
        packageNames.addAll(extensionPackages(extensionInterface));

        Class<?> extensionClass = extensionClassForObject(anObject, extensionInterface, packageNames);
        if (extensionClass == null)
            throw new IllegalArgumentException(MessageFormat.format("No extension {0} for a {1} class",
                    extensionNames(packageNames, anObject.getClass().getSimpleName(), extensionInterface.getSimpleName()),
                    anObject.getClass().getName()));
        try {
            if (!extensionInterface.isAssignableFrom(extensionClass))
                throw new IllegalStateException(MessageFormat.format("Extension \"{0}\"class does not implement the \"{1}\" interface",
                        extensionClass.getName(), extensionInterface.getName()));

            Constructor<?> constructor = getDeclaredConstructor(extensionClass, anObject.getClass());
            Object extension = constructor.getParameterCount() == 0 ? constructor.newInstance() : constructor.newInstance(anObject);
            if (extension instanceof DelegateHolder)
                ((DelegateHolder) extension).setDelegate(anObject);

            return (T) Proxy.newProxyInstance(extensionInterface.getClassLoader(),
                    new Class<?>[]{anExtensionInterface, PrivateDelegate.class},
                    (proxy, method, args) -> performOperation(extension, anObject, method, args));
        } catch (Exception aE) {
            throw new RuntimeException(aE);
        }
    }

    private static List<String> getAnnotatedPackageNames(Class<?> extensionInterface, List<String> packageNames) {
        ExtensionInterface annotation = extensionInterface.getAnnotation(ExtensionInterface.class);
        if (annotation != null) {
            String[] annotatedPackageNames = annotation.packages();
            if (annotatedPackageNames != null && annotatedPackageNames.length > 0) {
                packageNames = new ArrayList<>(packageNames);
                packageNames.addAll(Arrays.asList(annotatedPackageNames));
            }
        }
        return packageNames;
    }

    private static <T> Object performOperation(T anExtension, Object anObject, Method aMethod, Object[] aArgs)
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
            } else if (aMethod.getDeclaringClass().isAssignableFrom(PrivateDelegate.class)) {
                // invoke object method
                result = aMethod.invoke((PrivateDelegate)() -> anObject, aArgs);
            } else {
                throw new IllegalArgumentException("Unexpected method: " + methodName);
            }
        }

        return result;
    }

    static Class<?> findAnnotatedInterface(Class<?> anInterfaceClass, Class<? extends Annotation> anAnnotationClass) {
        if (anInterfaceClass.isAnnotationPresent(anAnnotationClass))
            return anInterfaceClass;

        for (Class<?> superInterface : anInterfaceClass.getInterfaces()) {
            if (superInterface.isAnnotationPresent(anAnnotationClass)) {
                return superInterface;
            }
            Class<?> result = findAnnotatedInterface(superInterface, anAnnotationClass);
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
            if (packageName == null)
                continue;

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
            if (!result.isEmpty())
                result.append(", ");
            result.append(extensionName(packageName, aSimpleClassName, extensionName));
        }
        return result.toString();
    }

    //region Extension Packages methods

    private final Map<Class<?>, List<String>> extensionPackages = new HashMap<>();

    List<String> extensionPackages(Class<?> anExtensionInterface) {
        Objects.requireNonNull(anExtensionInterface);

        synchronized (extensionPackages) {
            List<String> result = extensionPackages.get(anExtensionInterface);
            return result != null ? new ArrayList<>(result) : Collections.emptyList();
        }
    }

    /**
     * Adds an extension package to the list of packages used to lookup for extensions
     *
     * @param anExtensionInterface an extension interface to add a package for
     * @param anExtensionPackage   the name of the package
     */
    public void addExtensionPackage(Class<?> anExtensionInterface, String anExtensionPackage) {
        Objects.requireNonNull(anExtensionInterface);
        Objects.requireNonNull(anExtensionPackage);

        synchronized (extensionPackages) {
            List<String> result = extensionPackages.computeIfAbsent(anExtensionInterface, k -> new ArrayList<>());
            result.add(anExtensionPackage);
        }
    }

    /**
     * Removes an extension package from the list of packages used to lookup for extensions
     *
     * @param anExtensionInterface an extension interface to remove a package for
     * @param anExtensionPackage   the name of the package
     */
    public void removeExtensionPackage(Class<?> anExtensionInterface, String anExtensionPackage) {
        Objects.requireNonNull(anExtensionInterface);
        Objects.requireNonNull(anExtensionPackage);

        synchronized (extensionPackages) {
            List<String> result = extensionPackages.get(anExtensionInterface);
            if (result != null) {
                result.remove(anExtensionPackage);
                if (result.isEmpty())
                    extensionPackages.remove(anExtensionInterface);
            }
        }
    }
    //endregion

    //region Cache methods
    private boolean cacheEnabled = true;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCacheEnabled(boolean isCacheEnabled) {
        if (cacheEnabled != isCacheEnabled) {
            cacheEnabled = isCacheEnabled;
            if (! isCacheEnabled)
                cacheClear();
        }
    }

    @SuppressWarnings("rawtypes")
    final ThreadSafeWeakCache extensionCache = new ThreadSafeWeakCache<ClassExtensionKey, ClassExtension>();

    /**
     * {@inheritDoc}
     */
    public void cacheCleanup() {
        extensionCache.cleanup();
    }

    /**
     * {@inheritDoc}
     */
    public void cacheClear() {
        extensionCache.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void scheduleCacheCleanup() {
        extensionCache.scheduleCleanup();
    }

    /**
     * {@inheritDoc}
     */
    public void shutdownCacheCleanup() {
        extensionCache.shutdownCleanup();
    }

    /**
     * {@inheritDoc}
     */
    public boolean cacheIsEmpty() {
        return extensionCache.isEmpty();
    }
    //endregion
}
