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

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>Class {@code DynamicClassExtension} provides a way to mimic class extensions (categories) by composing extensions
 * as a set of lambda operations. To specify an extension:</p>
 *  <ol>
 *      <li>Create a {@code Builder} for an interface you want to compose an extension for by using the DynamicClassExtension.sharedBuilder(...) method</li>
 *      <li>Specify the name of an operation using {@code Builder.opName(String)}</li>
 *      <li>List all the method implementations per particular classes with lambdas using {@code Builder.op(...)} or
 *      {@code Builder.voidOp(...)}</li>
 *      <li>Repeat 2, 3 for all operations</li>
 *  </ol>
 *  <p>For example, the following code creates {@code Item_Shippable} extensions for {@code Item classes}. There are explicit
 *  {@code ship()} method implementations for all the {@code Item} classes. Though, the {@code log()} method is implemented
 *  for the {@code Item} class only so extensions for all the {@code Item} descendants will utilise the same {@code log()}
 *  method.</p>
 *  <pre><code>
 *  class Item {...}
 *  class Book extends Item {...}
 *  class Furniture extends Item {...}
 *  class ElectronicItem extends Item {...}
 *  class AutoPart extends Item {...}
 *
 *  interface Item_Shippable {
 *      ShippingInfo ship();
 *      void log(boolean isVerbose);
 *  }
 *  ...
 * DynamicClassExtension.sharedBuilder(Item_Shippable.class).
 *      name("ship").
 *          op(Item.class, book -> ...).
 *          op(Book.class, book -> ...).
 *          op(Furniture.class, furniture -> ...).
 *          op(ElectronicItem.class, electronicItem -> ...).
 *      name("log").
 *          voidOp(Item.class, (Item item, Boolean isVerbose) -> {...}).
 *      build();
 *  </code></pre>
 *  <p>Finding an extension and calling its methods is simple and straightforward:</p>
 *  <pre><code>
 *      Item_Shippable itemShippable = DynamicClassExtension.sharedExtension(new Book("The Mythical Man-Month"), Item_Shippable.class);
 *      itemShippable.log(true);
 *      itemShippable.ship();
 *  </code></pre>
 *
 *  <p>For the most of the cases a shared instance of DynamicClassExtension should be used. But if there is a need to have
 *  different implementations of extensions in different places or domains it is possible to create and utilize new
 *  instances of DynamicClassExtension.</p>
 *
 * <p>{@code DynamicClassExtension} takes care of inheritance so it is possible to design and implement class extensions hierarchy
 * that fully or partially resembles original classes' hierarchy. If there's no explicit extension operations specified for particular
 * class - its parent extension will be utilised. For example, if there's no explicit extension operations defined for
 * {@code AutoPart} objects - base {@code ship()} and {@code log(boolean)} operations specified for {@code Item} will be used instead.</p>
 *
 * <p>Cashing of extension objects are supported out of the box. Cache utilises weak references to release extension objects
 * that are not in use. Though, to perform full cleanup either the {@code cacheCleanup()} should be used or automatic cleanup can
 * be initiated via the {@code scheduleCacheCleanup()}. If automatic cache cleanup is used - it can be stopped by calling the
 * {@code shutdownCacheCleanup()}.</p>
 *
 * @author Gregory Ledenev
 * @version 0.9.2
 */
public class DynamicClassExtension {

    static final String SUFFIX_BI = "Bi";

    <R, T, E> void addExtensionOperation(Class<T> aClass,
                                         Class<E> anExtensionClass,
                                         String anOperationName,
                                         Function<T, R> anOperation) {
        checkAddOperationArguments(aClass, anExtensionClass, anOperationName, anOperation);

        OperationKey key = new OperationKey(aClass, anExtensionClass, operationName(anOperationName, null));
        if (operationsMap.containsKey(key))
            duplicateOperationError(displayOperationName(anOperationName, false, null));
        operationsMap.put(key, anOperation);
    }

    private static <R, T, E> void checkAddOperationArguments(Class<T> aClass, Class<E> anExtensionClass, String anOperationName, Object anOperation) {
        Objects.requireNonNull(aClass, "Object class is not specified");
        Objects.requireNonNull(anExtensionClass, "Extension interface is not specified");
        Objects.requireNonNull(anOperationName, "Operation name is not specified");
        Objects.requireNonNull(anOperation, "Operation is not specified");
    }

    void duplicateOperationError(String anOperationName) {
        throw new IllegalArgumentException("Duplicate operation: " + anOperationName);
    }

    static final private String[] DUMMY_ARGS = {"true"};

    <R, T, U, E> void addExtensionOperation(Class<T> aClass,
                                            Class<E> anExtensionClass,
                                            String anOperationName,
                                            BiFunction<T, U, R> anOperation) {
        checkAddOperationArguments(aClass, anExtensionClass, anOperationName, anOperation);

        OperationKey key = new OperationKey(aClass, anExtensionClass, operationName(anOperationName, DUMMY_ARGS));
        if (operationsMap.containsKey(key))
            duplicateOperationError(displayOperationName(anOperationName, false, DUMMY_ARGS));
        operationsMap.put(key, anOperation);
    }

    <T, E> void addVoidExtensionOperation(Class<T> aClass,
                                          Class<E> anExtensionClass,
                                          String anOperationName,
                                          Consumer<T> anOperation) {
        checkAddOperationArguments(aClass, anExtensionClass, anOperationName, anOperation);

        OperationKey key = new OperationKey(aClass, anExtensionClass, operationName(anOperationName, null));
        if (operationsMap.containsKey(key))
            duplicateOperationError(displayOperationName(anOperationName, true, null));
        operationsMap.put(key, anOperation);
    }

    <T, U, E> void addVoidExtensionOperation(Class<T> aClass,
                                             Class<E> anExtensionClass,
                                             String anOperationName,
                                             BiConsumer<T, U> anOperation) {
        checkAddOperationArguments(aClass, anExtensionClass, anOperationName, anOperation);

        OperationKey key = new OperationKey(aClass, anExtensionClass, operationName(anOperationName, DUMMY_ARGS));
        if (operationsMap.containsKey(key))
            duplicateOperationError(displayOperationName(anOperationName, true, DUMMY_ARGS));
        operationsMap.put(key, anOperation);
    }

    <T, E> Object getExtensionOperation(Class<T> aClass,
                                        Class<E> anExtensionClass,
                                        String anOperationName) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionClass);
        Objects.requireNonNull(anOperationName);

        return operationsMap.get(new OperationKey(aClass, anExtensionClass, anOperationName));
    }

    <T, E> void removeExtensionOperation(Class<T> aClass,
                                         Class<E> anExtensionClass,
                                         String anOperationName,
                                         Object[] anArgs) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionClass);
        Objects.requireNonNull(anOperationName);

        operationsMap.remove(new OperationKey(aClass, anExtensionClass, operationName(anOperationName, anArgs)));
    }

    /**
     * Finds and returns an extension object according to a supplied class. You should use calls of
     * {@code addExtensionOperation()} to compose dynamic extensions before calling the {@code dynamicExtension} method.
     * Otherwise, an empty dynamic extension having no operations will be returned.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionClass class of extension object to be returned
     * @return an extension object
     */
    @SuppressWarnings({"unchecked"})
    public <T> T extensionNoCache(Object anObject, Class<T> anExtensionClass) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionClass);

        return (T) Proxy.newProxyInstance(anExtensionClass.getClassLoader(),
                new Class<?>[]{anExtensionClass},
                (proxy, method, args) -> performOperation(anObject, anExtensionClass, method, args));
    }

    /**
     * Finds and returns a shared extension object according to a supplied class. You should use calls of
     * {@code addExtensionOperation()} to compose dynamic extensions before calling the {@code dynamicExtension} method.
     * Otherwise, an empty dynamic extension having no operations will be returned.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionClass class of extension object to be returned
     * @return an extension object
     */
    public <T> T sharedExtensionNoCache(Object anObject, Class<T> anExtensionClass) {
        return sharedInstance().extensionNoCache(anObject, anExtensionClass);
    }

    /**
     * Finds and returns an extension object according to a supplied class. You should use calls of
     * {@code addExtensionOperation()} to compose dynamic extensions before calling the {@code dynamicExtension} method.
     * Otherwise, an empty dynamic extension having no operations will be returned. It uses cache to avoid redundant
     * objects creation.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionClass class of extension object to be returned
     * @return an extension object
     */
    @SuppressWarnings({"unchecked"})
    public <T> T extension(Object anObject, Class<T> anExtensionClass) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionClass);

        return (T) extensionCache.getOrCreate(anObject, () -> extensionNoCache(anObject, anExtensionClass));
    }

    /**
     * Finds and returns a shared extension object according to a supplied class. You should use calls of
     * {@code addExtensionOperation()} to compose dynamic extensions before calling the {@code dynamicExtension} method.
     * Otherwise, an empty dynamic extension having no operations will be returned. It uses cache to avoid redundant
     * objects creation.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionClass class of extension object to be returned
     * @return an extension object
     */
    public static <T> T sharedExtension(Object anObject, Class<T> anExtensionClass) {
        return sharedInstance().extension(anObject, anExtensionClass);
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
            Object operation = findExtensionOperation(anObject, anExtensionClass, method, args);
            if (operation != null) {
                if (void.class.equals(method.getReturnType())) {
                    if (args == null || args.length == 0) {
                        ((Consumer) operation).accept(anObject);
                    } else if (args.length == 1) {
                        ((BiConsumer) operation).accept(anObject, args[0]);
                    }
                } else if (args == null || args.length == 0) {
                    result = ((Function) operation).apply(anObject);
                } else if (args.length == 1) {
                    result = ((BiFunction) operation).apply(anObject, args[0]);
                }
            } else {
                throw new IllegalArgumentException(MessageFormat.format("No \"{0}\" operation for \"{1}\"",
                         displayOperationName(method.getName(), void.class.equals(method.getReturnType()), args), anObject));
            }
        }

        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    <T> Object findExtensionOperation(Object anObject, Class<T> anExtensionClass, Method method, Object[] anArgs) {
        Object result;

        Class current = anObject.getClass();
        do {
            result = getExtensionOperation(current, anExtensionClass, operationName(method.getName(), anArgs));
            current = current.getSuperclass();
        } while (current != null && result == null);
        return result;
    }


    String operationName(String anOperationName, Object[] anArgs) {
        return anArgs == null || anArgs.length == 0 ? anOperationName : anOperationName + SUFFIX_BI;
    }

    String displayOperationName(String anOperationName, boolean isVoid, Object[] anArgs) {
        return MessageFormat.format("{0} {1}({2})",
                isVoid ? "void" : "T",
                anOperationName,
                anArgs != null && anArgs.length > 0 ? "T" : "");
    }

    @SuppressWarnings({"rawtypes"})
    record OperationKey(Class objectClass, Class extensionClass, String operationName) implements Comparable<OperationKey> {
        @Override
        public int compareTo(OperationKey o) {
            return objectClass.getName().compareTo(o.objectClass.getName());
        }

        public String simpleOperationName() {
            return operationName.endsWith(SUFFIX_BI) ?
                    operationName.substring(0, operationName.length() - SUFFIX_BI.length()) :
                    operationName;
        }
    }

    @SuppressWarnings({"rawtypes"})
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();

        Map<Class, List<OperationKey>> byExtensionClass = operationsMap.keySet().stream().
                collect(Collectors.groupingBy(OperationKey::extensionClass));
        byExtensionClass.keySet().stream().
                sorted((o1, o2) -> o1.getName().compareTo(o2.getName())).
                forEach(aClass -> {
            result.append(aClass).append(" {\n");
            Map<String, List<OperationKey>> byOperation = byExtensionClass.get(aClass).stream().
                    collect(Collectors.groupingBy(OperationKey::simpleOperationName));

            byOperation.keySet().stream().
                    sorted().
                    forEach(anOperation -> {
                result.append("    ").append(anOperation).append(" {\n");
                byOperation.get(anOperation).stream().sorted().forEach(anOperationKey -> {
                    result.append("    ").append("    ").append(anOperationKey.objectClass.getName()).append(".class -> ");
                    Object lambda = operationsMap.get(anOperationKey);
                    if (lambda instanceof Function)
                        result.append(MessageFormat.format("T {0}()\n", anOperation));
                    else if (lambda instanceof BiFunction)
                        result.append(MessageFormat.format("T {0}(T)\n", anOperation));
                    else if (lambda instanceof Consumer)
                        result.append(MessageFormat.format("void {0}()\n", anOperation));
                    else if (lambda instanceof BiConsumer)
                        result.append(MessageFormat.format("void {0}(T)\n", anOperation));
                });
                result.append("    ").append("}\n");
            });
            result.append("}\n");
        });

        return result.toString();
    }

    private final ConcurrentHashMap<OperationKey, Object> operationsMap = new ConcurrentHashMap<>();
    private static final DynamicClassExtension dynamicClassExtension = new DynamicClassExtension();

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
     *
     * @return true if cache is empty; false otherwise
     */
    boolean cacheIsEmpty() {
        return extensionCache.isEmpty();
    }
    //endregion

    public static DynamicClassExtension sharedInstance() {
        return dynamicClassExtension;
    }

    public static <E> Builder<E> sharedBuilder(Class<E> aExtensionClass) {
        return dynamicClassExtension.builder(aExtensionClass);
    }

    public <E> Builder<E> builder(Class<E> anExtensionClass) {
        Objects.requireNonNull(anExtensionClass, "Extension interface is not specified");
        if (! anExtensionClass.isInterface())
            throw new IllegalArgumentException(anExtensionClass.getName() + " is not an interface");

        return new Builder<>(anExtensionClass, this);
    }

    /**
     * <p>Class {@code Builder} provides an ability to design class extensions (categories) by composing extensions
     * as a set of lambda operations.</p>
     *
     * @param <E> an interface to build an extension for
     */
    public static class Builder<E> {
        private final DynamicClassExtension dynamicClassExtension;
        private final Class<E> extensionClass;
        private String operationName;

        Builder(Class<E> aExtensionClass, DynamicClassExtension aDynamicClassExtension) {
            extensionClass = aExtensionClass;
            dynamicClassExtension = aDynamicClassExtension;
        }

        Builder(Class<E> aExtensionClass, String aOperationName, DynamicClassExtension aDynamicClassExtension) {
            extensionClass = aExtensionClass;
            operationName = aOperationName;
            dynamicClassExtension = aDynamicClassExtension;
        }

        /**
         * Specifies an operation name
         * @param anOperationName operation name. It should correspond to the name of a method defined by extension
         *                        interface
         * @return a copy of this {@code Builder}
         */
        public Builder<E> opName(String anOperationName) {
            return new Builder<>(extensionClass, anOperationName, dynamicClassExtension);
        }

        /**
         * Removes an operation
         * @param anObjectClass object class
         * @param aParameters arguments. Pass {@code null} or an empty array to define parameterless operation; otherwise pass
         *               an array of parameter types
         * @return a copy of this {@code Builder}
         */
        public <T1> Builder<E> removeOp(Class<T1> anObjectClass, Object[] aParameters) {
            dynamicClassExtension.removeExtensionOperation(anObjectClass, extensionClass, operationName, aParameters);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension);
        }

        /**
         * Adds a non-void parameterless operation
         * @param anObjectClass object class
         * @param anOperation lambda that defines an operation
         * @return a copy of this {@code Builder}
         */
        public <R, T1> Builder<E> op(Class<T1> anObjectClass, Function<T1, R> anOperation) {
            dynamicClassExtension.addExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension);
        }

        /**
         * Adds a non-void operation having one parameter
         * @param anObjectClass object class
         * @param anOperation lambda that defines an operation
         * @return a copy of this {@code Builder}
         */
        public <R, T1, U> Builder<E> op(Class<T1> anObjectClass, BiFunction<T1, U, R> anOperation) {
            dynamicClassExtension.addExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension);
        }

        /**
         * Adds a void parameterless operation
         * @param anObjectClass object class
         * @param anOperation lambda that defines an operation
         * @return a copy of this {@code Builder}
         */
        public <T1> Builder<E> voidOp(Class<T1> anObjectClass, Consumer<T1> anOperation) {
            dynamicClassExtension.addVoidExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension);
        }

        /**
         * Adds a void operation having one parameter
         * @param anObjectClass object class
         * @param anOperation lambda that defines an operation
         * @return a copy of this {@code Builder}
         */
        public <T1, U> Builder<E> voidOp(Class<T1> anObjectClass, BiConsumer<T1, U> anOperation) {
            dynamicClassExtension.addVoidExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension);
        }

        /**
         * Terminal operation (optional)
         * @return a {@code DynamicClassExtension} this builder was created for
         */
        public DynamicClassExtension build() {
            return dynamicClassExtension;
        }
    }
}
