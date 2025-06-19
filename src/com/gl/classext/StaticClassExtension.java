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
 * can be done either:</p>
 * <ul>
 *  <li>By providing a single parameter constructor that takes a delegate object to work with as an argument.</li>
 *  <li>By implementing the {@code DelegateHolder} interface. The {@code DelegateHolder.setDelegate(Object)} method is
 *  used to supply extensions ith delegate objects to work with. Usually, it is fine to implement the {@code DelegateHolder}
 *  interface in a common root of some classes' hierarchy.</li>
 * </ul>
 * <p>By default, {@code StaticClassExtension} searches for extension classes in the same package as the extension interface.
 * To register extension classes located in different packages, use the {@code addExtensionPackage(Class, String)} method of
 * {@code StaticClassExtension}. For example:
 *  <pre><code>
 * StaticClassExtension.sharedInstance().addExtensionPackage(Shippable.class, "test.toys.shipment");
 * </code></pre>
 * <p>Note: Extensions returned by {@code StaticClassExtension} do not directly correspond to the extension classes
 * themselves. Therefore, it is crucial not to cast these extensions. Instead, always utilize only the methods provided
 * by the extension interface.</p>
 *
 * @see <a href="https://github.com/gregory-ledenev/java-class-extension/blob/main/doc/static-class-extensions.md">More details</a>
 * @author Gregory Ledenev
 */
public class StaticClassExtension extends AbstractClassExtension {

    private static final StaticClassExtension classExtension = new StaticClassExtension();

    public static StaticClassExtension sharedInstance() {
        return classExtension;
    }

    /**
     * {@inheritDoc}
     */
    public <T> T extension(Object anObject, Class<T> anExtensionInterface) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionInterface);

        return extension(anObject, anExtensionInterface, (List<String>) null);
    }

    @Override
    public boolean compatible(Type aType) {
        return aType == Type.STATIC_PROXY || aType == Type.STATIC_DIRECT;
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

        return isCacheEnabled(anExtensionInterface) ?
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
    @SuppressWarnings({"unchecked"})
    protected <T> T extensionNoCache(Object anObject, Class<T> anExtensionInterface, List<String> aPackageNames) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionInterface);

        List<String> packageNames = getPackageNames(anExtensionInterface, aPackageNames);

        Type instantiationStrategy = Type.STATIC_PROXY;

        Class<?> extensionInterface = findAnnotatedInterface(anExtensionInterface, ExtensionInterface.class);
        if (extensionInterface == null) {
            extensionInterface = anExtensionInterface;
        } else {
            packageNames = getAnnotatedPackageNames(extensionInterface, packageNames);
            instantiationStrategy = classExtensionType(extensionInterface);
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

            Object extension = createExtension(anObject, extensionClass);

            if (instantiationStrategy != Type.STATIC_DIRECT) {
                Class<?> finalExtensionInterface = extensionInterface;
                return (T) Proxy.newProxyInstance(extensionInterface.getClassLoader(),
                        new Class<?>[]{anExtensionInterface, PrivateDelegateHolder.class},
                        (proxy, method, args) -> performOperation(this, finalExtensionInterface, extension, anObject, method, args));
            } else {
                if (isVerbose() && ! anExtensionInterface.isAssignableFrom(extensionClass))
                    logger.severe(MessageFormat.format("""
                                                       Extension {0} does not implement {1}. You should \
                                                       either obtain extension for {2} or \
                                                       use Proxy instantiation strategy. Check {2} for @ExtensionInterface annotation""",
                            extensionClass, anExtensionInterface, extensionInterface));
                return (T) extension;
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object createExtension(Object anObject, Class<?> anExtensionClass) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Constructor<?> constructor = getDeclaredConstructor(anExtensionClass, anObject.getClass());
        Object result;
        if (constructor.getParameterCount() == 1) {
            result = constructor.newInstance(anObject);
        } else {
            if (DelegateHolder.class.isAssignableFrom(anExtensionClass)) {
                result = constructor.newInstance();
                ((DelegateHolder) result).setDelegate(anObject);
            } else {
                throw new IllegalArgumentException(MessageFormat.format("Found extension {0} must provide either a single parameter {1}(Object) constructor or implement {2}",
                        anExtensionClass, anExtensionClass.getSimpleName(), DelegateHolder.class));
            }
        }

        return result;
    }

    private static Type classExtensionType(Class<?> extensionInterface) {
        ExtensionInterface annotation = extensionInterface.getAnnotation(ExtensionInterface.class);
        return annotation != null ? annotation.type() : Type.STATIC_PROXY;
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

    private static <T, I> Object performOperation(StaticClassExtension aClassExtension,
                                                  Class<I> anExtensionInterface, T anExtension,
                                                  Object anObject, Method aMethod, Object[] anArgs) {
        Object result;
        Aspects.Pointcut aroundPointcut = null;
        Aspects.Pointcut beforePointcut = null;
        Aspects.Pointcut afterPointcut = null;

        String methodName = aMethod.getName();
        if (aClassExtension.isAspectsEnabled(anExtensionInterface)) {
            aroundPointcut = aClassExtension.getPointcut(anExtension.getClass(), anObject.getClass(), methodName, aMethod.getParameterTypes(), Aspects.AdviceType.AROUND);
            beforePointcut = aroundPointcut == null ? aClassExtension.getPointcut(anExtension.getClass(), anObject.getClass(), methodName, aMethod.getParameterTypes(), Aspects.AdviceType.BEFORE) : null;
            afterPointcut = aroundPointcut == null ? aClassExtension.getPointcut(anExtension.getClass(), anObject.getClass(), methodName, aMethod.getParameterTypes(), Aspects.AdviceType.AFTER) : null;
        }

        if (beforePointcut != null) {
            if (aClassExtension.isVerbose())
                aClassExtension.logger.info(formatAdvice(anObject, beforePointcut, Aspects.AdviceType.BEFORE));
            beforePointcut.before(methodName, anObject, anArgs);
        }

        if ("toString".equals(methodName) && aMethod.getParameterCount() == 0) {
            result = performOperation(aClassExtension, anObject, aMethod, anArgs, aroundPointcut, (operation, object, args) -> object.toString());
        } else if ("hashCode".equals(methodName) && aMethod.getParameterCount() == 0) {
            result = performOperation(aClassExtension, anObject, aMethod, anArgs, aroundPointcut, (operation, object, args) -> object.hashCode());
        } else if ("equals".equals(methodName) && aMethod.getParameterCount() == 1 && aMethod.getParameterTypes()[0] == Object.class) {
            result = performOperation(aClassExtension, anObject, aMethod, anArgs, aroundPointcut, (operation, object, args) -> object.equals(args[0]));
        } else {
            if (aMethod.getDeclaringClass().isAssignableFrom(anExtension.getClass())) {
                // invoke extension method
                if (aClassExtension.isVerbose())
                    aClassExtension.logger.info(MessageFormat.format("Performing operation for extension \"{0}\" -> {1}", anExtension, aMethod));
                result = performOperation(aClassExtension, anExtension, aMethod, anArgs, aroundPointcut);
            } else if (aMethod.getDeclaringClass().isAssignableFrom(anObject.getClass())) {
                // invoke object method
                if (aClassExtension.isVerbose())
                    aClassExtension.logger.info(MessageFormat.format("Performing operation for delegate \"{0}\" -> {1}", anObject, aMethod));
                result = performOperation(aClassExtension, anObject, aMethod, anArgs, aroundPointcut);
            } else if (aMethod.getDeclaringClass().isAssignableFrom(PrivateDelegateHolder.class)) {
                // invoke object method
                result = performOperation(aClassExtension, (PrivateDelegateHolder)() -> anObject, aMethod, anArgs, aroundPointcut);
            } else {
                throw new IllegalArgumentException("Unexpected method: " + methodName);
            }
        }

        if (afterPointcut != null) {
            if (aClassExtension.isVerbose())
                aClassExtension.logger.info(formatAdvice(anObject, afterPointcut, Aspects.AdviceType.AFTER));
            afterPointcut.after(result, methodName, anObject, anArgs);
        }

        return classExtensionForOperationResult(aClassExtension, aMethod, result);
    }

    private static Object performOperation(StaticClassExtension aClassExtension, Object anObject, Method aMethod, Object[] anArgs, Aspects.Pointcut aroundPointcut) {
        return performOperation(aClassExtension, anObject, aMethod, anArgs, aroundPointcut, (operation, anObject1, anArgs1) -> {
            try {
                return aMethod.invoke(anObject1, anArgs1);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private static Object performOperation(StaticClassExtension aClassExtension, Object anObject, Method aMethod, Object[] anArgs, Aspects.Pointcut aroundPointcut, Performer<Object> objectPerformer) {
        Object result;
        if (aroundPointcut != null) {
            if (aClassExtension.isVerbose())
                aClassExtension.logger.info(formatAdvice(anObject, aroundPointcut, Aspects.AdviceType.AROUND));
            result = aroundPointcut.around(objectPerformer, aMethod.getName(), anObject, anArgs);
        } else {
            result = objectPerformer.perform(aMethod.getName(), anObject, anArgs);
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
            String extensionClassName = null;
            try {
                String fullClassName = current.getName();
                int index = fullClassName.lastIndexOf(".");
                String className = index != -1 ? fullClassName.substring(index + 1) : fullClassName;
                extensionClassName = extensionName(aPackageName, className, anExtensionName);
                result = (Class<T>) Class.forName(extensionClassName);
                if (isVerbose())
                    logger.info(MessageFormat.format("Got extension class \"{0}\" for an object of \"{1}\"",
                            extensionClassName, anObject.getClass().getName()));
            } catch (Exception aE) {
                if (isVerbose())
                    logger.info(MessageFormat.format("No extension class \"{0}\" for an object of \"{1}\"",
                            extensionClassName, anObject.getClass().getName()));
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

    /**
     * Returns a new instance of {@code AspectBuilder} used to build aspects for extensions
     * @return a new instance of {@code AspectBuilder}
     */
    public Aspects.AspectBuilder<StaticClassExtension> aspectBuilder() {
        return new Aspects.AspectBuilder<>(this);
    }
}
