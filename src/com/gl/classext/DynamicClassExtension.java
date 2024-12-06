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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * <p>Class {@code DynamicClassExtension} provides a way to simulate class extensions (categories) by composing extensions
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
 * @see <a href="https://github.com/gregory-ledenev/java-class-extension/blob/main/doc/dynamic-class-extensions.md">More details</a>
 * @author Gregory Ledenev
 */
public class DynamicClassExtension implements ClassExtension {

    private boolean cacheEnabled;

    @FunctionalInterface
    interface Performer<R> {
        R perform(Object[] anArgs);
    }


    /**
     * Represents an operation that accepts a single input argument and returns no
     * result. Unlike most other functional interfaces, {@code Consumer} is expected
     * to operate via side effects.
     *
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #accept(Object)}.
     *
     * @param <T> the type of the input to the operation
     */
    @FunctionalInterface
    @SuppressWarnings({"unchecked"})
    public interface ConsumerPerformer<T> extends Performer<Void>, Consumer<T> {
        @Override
        default Void perform(Object[] anArgs) {
            accept(anArgs != null && anArgs.length > 0 ? (T) anArgs[0] : null);
            return null;
        }
    }

    /**
     * Represents an operation that accepts two input arguments and returns no
     * result.  This is the two-arity specialization of {@link Consumer}.
     * Unlike most other functional interfaces, {@code BiConsumer} is expected
     * to operate via side effects.
     *
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #accept(Object, Object)}.
     *
     * @param <T> the type of the first argument to the operation
     * @param <U> the type of the second argument to the operation
     *
     * @see Consumer
     */
    @FunctionalInterface
    @SuppressWarnings({"unchecked"})
    public interface BiConsumerPerformer<T, U> extends Performer<Void>, BiConsumer<T, U> {
        @Override
        default Void perform(Object[] anArgs) {
            if (anArgs == null)
                accept(null, null);
            else
                accept(anArgs.length > 0 ? (T) anArgs[0] : null, anArgs.length > 1 ? (U) anArgs[1] : null);
            return null;
        }
    }

    /**
     * Represents a function that accepts one argument and produces a result.
     *
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #apply(Object)}.
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     */
    @FunctionalInterface
    @SuppressWarnings({"unchecked"})
    public interface FunctionPerformer<T, R> extends Performer<R>, Function<T, R> {
        @Override
        default R perform(Object[] anArgs) {
            return apply(anArgs != null && anArgs.length > 0 ? (T) anArgs[0] : null);
        }
    }

    /**
     * Represents a function that accepts two arguments and produces a result.
     * This is the two-arity specialization of {@link Function}.
     *
     * <p>This is a <a href="package-summary.html">functional interface</a>
     * whose functional method is {@link #apply(Object, Object)}.
     *
     * @param <T> the type of the first argument to the function
     * @param <U> the type of the second argument to the function
     * @param <R> the type of the result of the function
     *
     * @see Function
     */
    @FunctionalInterface
    @SuppressWarnings({"unchecked"})
    public interface BiFunctionPerformer<T, U, R> extends Performer<R>, BiFunction<T, U, R> {
        @Override
        default R perform(Object[] anArgs) {
            return ((anArgs == null) ?
                    apply(null, null) :
                    apply(anArgs.length > 0 ? (T) anArgs[0] : null, anArgs.length > 1 ? (U) anArgs[1] : null));
        }
    }

    static final String SUFFIX_BI = "Bi";

    <R, T, E> void addExtensionOperation(Class<T> aClass,
                                         Class<E> anExtensionInterface,
                                         String anOperationName,
                                         FunctionPerformer<T, R> anOperation) {
        checkAddOperationArguments(aClass, anExtensionInterface, anOperationName, anOperation);

        OperationKey key = new OperationKey(aClass, anExtensionInterface, operationName(anOperationName, null));
        if (operationsMap.containsKey(key))
            duplicateOperationError(displayOperationName(anOperationName, false, null));
        operationsMap.put(key, anOperation);
    }

    private static <T, E> void checkAddOperationArguments(Class<T> aClass, Class<E> anExtensionInterface, String anOperationName, Object anOperation) {
        Objects.requireNonNull(aClass, "Object class is not specified");
        Objects.requireNonNull(anExtensionInterface, "Extension interface is not specified");
        Objects.requireNonNull(anOperationName, "Operation name is not specified");
        Objects.requireNonNull(anOperation, "Operation is not specified");
    }

    void duplicateOperationError(String anOperationName) {
        throw new IllegalArgumentException("Duplicate operation: " + anOperationName);
    }

    static final private Class<?>[] SINGLE_PARAMETERS = {Object.class};

    <R, T, U, E> void addExtensionOperation(Class<T> aClass,
                                            Class<E> anExtensionInterface,
                                            String anOperationName,
                                            BiFunctionPerformer<T, U, R> anOperation) {
        checkAddOperationArguments(aClass, anExtensionInterface, anOperationName, anOperation);

        OperationKey key = new OperationKey(aClass, anExtensionInterface, operationName(anOperationName, SINGLE_PARAMETERS));
        if (operationsMap.containsKey(key))
            duplicateOperationError(displayOperationName(anOperationName, false, SINGLE_PARAMETERS));
        operationsMap.put(key, anOperation);
    }

    <T, E> void addVoidExtensionOperation(Class<T> aClass,
                                          Class<E> anExtensionInterface,
                                          String anOperationName,
                                          ConsumerPerformer<T> anOperation) {
        checkAddOperationArguments(aClass, anExtensionInterface, anOperationName, anOperation);

        OperationKey key = new OperationKey(aClass, anExtensionInterface, operationName(anOperationName, null));
        if (operationsMap.containsKey(key))
            duplicateOperationError(displayOperationName(anOperationName, true, null));
        operationsMap.put(key, anOperation);
    }

    <T, U, E> void addVoidExtensionOperation(Class<T> aClass,
                                             Class<E> anExtensionInterface,
                                             String anOperationName,
                                             BiConsumerPerformer<T, U> anOperation) {
        checkAddOperationArguments(aClass, anExtensionInterface, anOperationName, anOperation);

        OperationKey key = new OperationKey(aClass, anExtensionInterface, operationName(anOperationName, SINGLE_PARAMETERS));
        if (operationsMap.containsKey(key))
            duplicateOperationError(displayOperationName(anOperationName, true, SINGLE_PARAMETERS));
        operationsMap.put(key, anOperation);
    }

    <T, E> Object getExtensionOperation(Class<T> aClass,
                                        Class<E> anExtensionInterface,
                                        String anOperationName) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionInterface);
        Objects.requireNonNull(anOperationName);

        return operationsMap.get(new OperationKey(aClass, anExtensionInterface, anOperationName));
    }

    <T, E> void removeExtensionOperation(Class<T> aClass,
                                         Class<E> anExtensionClass,
                                         String anOperationName,
                                         Class<?>[] aParameterTypes) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionClass);
        Objects.requireNonNull(anOperationName);

        operationsMap.remove(new OperationKey(aClass, anExtensionClass, operationName(anOperationName, aParameterTypes)));
    }

    /**
     * Finds and returns an extension object according to a supplied class. You should use calls of
     * {@code addExtensionOperation()} to compose dynamic extensions before calling the {@code dynamicExtension} method.
     * Otherwise, an empty dynamic extension having no operations will be returned.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionInterface class of extension object to be returned
     * @return an extension object
     */
    @SuppressWarnings({"unchecked"})
    public <T> T extensionNoCache(Object anObject, Class<T> anExtensionInterface) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionInterface);

        try {
            return (T) Proxy.newProxyInstance(anExtensionInterface.getClassLoader(),
                    new Class<?>[]{anExtensionInterface, PrivateDelegateHolder.class},
                    (proxy, method, args) -> performOperation(this, anObject, anExtensionInterface, method, args));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Finds and returns a shared extension object according to a supplied class. You should use calls of
     * {@code addExtensionOperation()} to compose dynamic extensions before calling the {@code dynamicExtension} method.
     * Otherwise, an empty dynamic extension having no operations will be returned.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionInterface class of extension object to be returned
     * @return an extension object
     */
    @SuppressWarnings("unused")
    public <T> T sharedExtensionNoCache(Object anObject, Class<T> anExtensionInterface) {
        return sharedInstance().extensionNoCache(anObject, anExtensionInterface);
    }

    /**
     * Finds and returns an extension object according to a supplied class. You should use calls of
     * {@code addExtensionOperation()} to compose dynamic extensions before calling the {@code dynamicExtension} method.
     * Otherwise, an empty dynamic extension having no operations will be returned. It uses cache to avoid redundant
     * objects creation.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionInterface class of extension object to be returned
     * @return an extension object
     */
    @SuppressWarnings({"unchecked"})
    public <T> T extension(Object anObject, Class<T> anExtensionInterface) {
        Objects.requireNonNull(anObject);
        Objects.requireNonNull(anExtensionInterface);

        return (T) extensionCache.getOrCreate(new ClassExtensionKey(anObject, anExtensionInterface), () -> extensionNoCache(anObject, anExtensionInterface));
    }

    /**
     * Finds and returns a shared extension object according to a supplied class. You should use calls of
     * {@code addExtensionOperation()} to compose dynamic extensions before calling the {@code dynamicExtension} method.
     * Otherwise, an empty dynamic extension having no operations will be returned. It uses cache to avoid redundant
     * objects creation.
     *
     * @param anObject         object to return an extension object for
     * @param anExtensionInterface class of extension object to be returned
     * @return an extension object
     */
    @SuppressWarnings("unused")
    public static <T> T sharedExtension(Object anObject, Class<T> anExtensionInterface) {
        return sharedInstance().extension(anObject, anExtensionInterface);
    }

    @SuppressWarnings("unused")
    // keep it for a while; maybe it will be useful
    public static Method findMethod(Class<?> aClass, String aMethodName, Class<?>[] aParameterTypes) {
        Method result = null;
        all: for (Method method : aClass.getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getName().equals(aMethodName) &&
                    (parameterTypes == aParameterTypes ||
                    (aParameterTypes != null && parameterTypes.length == aParameterTypes.length))) {
                if (aParameterTypes.length == 0) {
                    result = method;
                    break;
                } else {
                    for (int i = 0; i < aParameterTypes.length; i++) {
                        Class<?> parameterType = aParameterTypes[i];
                        if (parameterTypes[i].isAssignableFrom(parameterType)) {
                            result = method;
                            break all;
                        }
                    }
                }
            }
        }
        return result;
    }

    @SuppressWarnings({"rawtypes"})
    <T> Object performOperation(DynamicClassExtension aClassExtension, Object anObject, Class<T> anExtensionClass, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Object result;

        ExtensionOperationResult extensionOperation = findExtensionOperation(anObject, anExtensionClass, method, args);
        Performer operation = (Performer) extensionOperation.operation();
        if (operation != null) {
            if (aClassExtension.isVerbose())
                aClassExtension.logger.info(MessageFormat.format("Performing dynamic operation for delegate \"{0}\": ({1}) -> {2}",
                        anObject,
                        extensionOperation.inClass().getName(),
                        operationKeyToString(new OperationKey(anObject.getClass(), anExtensionClass, method.getName()), operation)));
            List<Object> arguments = new ArrayList<>();
            arguments.add(anObject);
            if (args != null)
                arguments.addAll(Arrays.asList(args));
            result = operation.perform(arguments.toArray());
        } else {
            if (method.getDeclaringClass().isAssignableFrom(anObject.getClass())) {
                if (aClassExtension.isVerbose())
                    aClassExtension.logger.info(MessageFormat.format("Performing operation for delegate \"{0}\" -> {1}", anObject, method));
                result = method.invoke(anObject, args);
            } else if (method.getDeclaringClass().isAssignableFrom(PrivateDelegateHolder.class)) {
                result = method.invoke((PrivateDelegateHolder)() -> anObject, args);
            } else {
                throw new IllegalArgumentException(MessageFormat.format("No \"{0}\" operation for \"{1}\"",
                        displayOperationName(method.getName(), void.class.equals(method.getReturnType()), args), anObject));
            }
        }

        return result;
    }

    /**
     * Checks if there is a valid extension defined for a passed object. An extension is considered valid if there are
     * corresponding operations registered for all its methods
     *
     * @param aClass         object to check an extension for
     * @param anExtensionClass class of extension
     * @throws IllegalArgumentException if an extension is invalid
     */
    @SuppressWarnings({"rawtypes"})
    public <T> void checkValid(Class<?> aClass, Class<T> anExtensionClass) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionClass);

        for (Method method : anExtensionClass.getMethods()) {
            if (method.isAnnotationPresent(OptionalMethod.class))
                continue;
            ExtensionOperationResult extensionOperation = findExtensionOperation(aClass, anExtensionClass, method, method.getParameterTypes());
            Performer operation = (Performer) extensionOperation.operation;
            if (operation == null)
                throw new IllegalArgumentException(MessageFormat.format("No \"{0}\" operation for {1} in \"{2}\" extension",
                        displayOperationName(method.getName(), void.class.equals(method.getReturnType()), method.getParameterTypes()),
                        aClass,
                        anExtensionClass.getName()));
        }
    }

    /**
     * Checks if an operation is present in an extension for a passed object.
     *
     * @param anObjectClass    object class to check an extension for
     * @param anExtensionClass class of extension
     * @param anOperation      operation to check
     * @param aParameterTypes  parameter types of an operation to check
     * @throws IllegalArgumentException if an extension is invalid
     */
    public boolean isPresent(Class<?> anObjectClass, Class<?> anExtensionClass, String anOperation, Class<?>[] aParameterTypes) {
        Objects.requireNonNull(anOperation);
        Objects.requireNonNull(anObjectClass);
        Objects.requireNonNull(anExtensionClass);

        try {
            Method method = anExtensionClass.getMethod(anOperation, aParameterTypes);
            return findExtensionOperation(anObjectClass, anExtensionClass, method, method.getParameterTypes()).operation != null;
        } catch (NoSuchMethodException aE) {
            return false;
        }
    }

    record ExtensionOperationResult(Object operation, Class<?> inClass) {}

    <T> ExtensionOperationResult findExtensionOperation(Object anObject, Class<T> anExtensionInterface, Method method, Object[] anArgs) {
        return findExtensionOperation(anObject.getClass(), anExtensionInterface, method, anArgs);
    }

     <T> ExtensionOperationResult findExtensionOperation(Class<?> anObjectClass, Class<T> anExtensionInterface, Method method, Object[] anArgs) {
        ExtensionOperationResult result = findExtensionOperationByClass(anObjectClass, anExtensionInterface, method, anArgs);
        if (result.operation == null)
            result = findExtensionOperationByInterface(anObjectClass, anExtensionInterface, method, anArgs);
        return result;
    }

    <T> ExtensionOperationResult findExtensionOperationByInterface(Class<?> anObjectClass, Class<T> anExtensionInterface, Method method, Object[] anArgs) {
        ExtensionOperationResult result = null;

        List<Class<?>> objectClasses = new ArrayList<>(Arrays.asList(anObjectClass.getInterfaces()));
        objectClasses.addFirst(anObjectClass);

        List<Class<?>> extensionInterfaces = new ArrayList<>(Arrays.asList(anExtensionInterface.getInterfaces()));
        extensionInterfaces.addFirst(anExtensionInterface);

        String operationName = operationName(method.getName(), parameterTypes(anArgs));

        all: for (Class<?> objectClass : objectClasses) {
            for (Class<?> extensionInterface : extensionInterfaces) {
                Object operation = getExtensionOperation(objectClass, extensionInterface, operationName);
                if (operation != null) {
                    result = new ExtensionOperationResult(operation, objectClass);
                    break all;
                }
            }
        }

        return result != null ? result : new ExtensionOperationResult(null, anObjectClass);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    <T> ExtensionOperationResult findExtensionOperationByClass(Class<?> anObjectClass, Class<T> anExtensionInterface, Method method, Object[] anArgs) {
        Object result;
        Class current = anObjectClass;
        do {
            result = getExtensionOperation(current, anExtensionInterface, operationName(method.getName(), parameterTypes(anArgs)));
            if (result != null)
                break;
            current = current.getSuperclass();
        } while (current != null);
        return new ExtensionOperationResult(result, current);
    }

    public static Class<?>[] parameterTypes(Object[] anArgs) {
        if (anArgs == null)
            return new Class<?>[0];

        Class<?>[] result = new Class<?>[anArgs.length];
        for (int i = 0; i < anArgs.length; i++) {
            result[i] = anArgs[i] != null ? anArgs[i].getClass() : Object.class;
        }
        return result;
    }


    String operationName(String anOperationName, Class<?>[] aParameterTypes) {
        return aParameterTypes == null || aParameterTypes.length == 0 ? anOperationName : anOperationName + SUFFIX_BI;
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

        public String objectClassName() {
            return objectClass.getName();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return toString(true);
    }

    /**
     * Returns string representation of registered operations
     * @param groupByOperation {@code true} if output should be grouped by extension interface, <i>operation</i> and object class;
     *                                     otherwise output will be grouped by extension interface, <i>object class</i> and operation
     * @return string representation
     */
    public String toString(boolean groupByOperation) {
        StringBuilder result = new StringBuilder();

        // circle through extension classes
        new HashMap<>(operationsMap).keySet().stream().
                collect(Collectors.groupingBy(OperationKey::extensionClass,
                        Collectors.groupingBy(groupByOperation ? OperationKey::simpleOperationName : OperationKey::objectClassName))).
                entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(Class::getName))).
                forEach(anEntryByClass -> {
                    result.append(anEntryByClass.getKey()).append(" {\n");

                    // circle through extension operation names
                    anEntryByClass.getValue().entrySet().stream().sorted(Map.Entry.comparingByKey()).
                            forEach(anEntryByOperationName -> {

                                result.append("    ").append(anEntryByOperationName.getKey()).append(" {\n");

                                // circle through operation keys
                                anEntryByOperationName.getValue().stream().sorted(
                                                Comparator.comparing(OperationKey::objectClassName).
                                                        thenComparing(OperationKey::operationName)).
                                        forEach(anOperationKey -> {
                                            result.append("        ");
                                            if (groupByOperation)
                                                result.append(anOperationKey.objectClass.getName()).append(" -> ");
                                            result.append(operationKeyToString(anOperationKey));
                                        });
                                result.append("    }\n");
                            });
                    result.append("}\n");
                });

        String resultStr = result.toString();
        return resultStr.endsWith("\n") ? resultStr.substring(0, result.toString().length() - 1) : resultStr;
    }

    String operationKeyToString(OperationKey anOperationKey) {
        return operationKeyToString(anOperationKey, operationsMap.get(anOperationKey));
    }

    String operationKeyToString(OperationKey anOperationKey, Object lambda) {
        String operationName = anOperationKey.simpleOperationName();
        String result = null;
        if (lambda instanceof Function)
            result = MessageFormat.format("T {0}()\n", operationName);
        else if (lambda instanceof BiFunction)
            result = MessageFormat.format("T {0}(T)\n", operationName);
        else if (lambda instanceof Consumer)
            result = MessageFormat.format("void {0}()\n", operationName);
        else if (lambda instanceof BiConsumer)
            result = MessageFormat.format("void {0}(T)\n", operationName);
        return result;
    }

    private final ConcurrentHashMap<OperationKey, Object> operationsMap = new ConcurrentHashMap<>();
    private static final DynamicClassExtension dynamicClassExtension = new DynamicClassExtension();

    //region Cache methods

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
    final ThreadSafeWeakCache extensionCache = new ThreadSafeWeakCache();

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

    public static DynamicClassExtension sharedInstance() {
        return dynamicClassExtension;
    }

    @SuppressWarnings("unused")
    public static <E> Builder<E> sharedBuilder(Class<E> aExtensionClass) {
        return dynamicClassExtension.builder(aExtensionClass);
    }

    public <E> Builder<E> builder(Class<E> anExtensionInterface) {
        Objects.requireNonNull(anExtensionInterface, "Extension interface is not specified");
        if (! anExtensionInterface.isInterface())
            throw new IllegalArgumentException(anExtensionInterface.getName() + " is not an interface");

        return new Builder<>(anExtensionInterface, this);
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
         * @param aParameterTypes arguments. Pass {@code null} or an empty array to define parameterless operation; otherwise pass
         *               an array of parameter types
         * @return a copy of this {@code Builder}
         */
        @SuppressWarnings("unused")
        public <T1> Builder<E> removeOp(Class<T1> anObjectClass, Class<?>[] aParameterTypes) {
            dynamicClassExtension.removeExtensionOperation(anObjectClass, extensionClass, operationName, aParameterTypes);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension);
        }

        /**
         * Adds a non-void parameterless operation
         * @param anObjectClass object class
         * @param anOperation lambda that defines an operation
         * @return a copy of this {@code Builder}
         */
        public <R, T1> Builder<E> op(Class<T1> anObjectClass, FunctionPerformer<T1, R> anOperation) {
            dynamicClassExtension.addExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension);
        }

        /**
         * Adds a non-void operation having one parameter
         * @param anObjectClass object class
         * @param anOperation lambda that defines an operation
         * @return a copy of this {@code Builder}
         */
        public <R, T1, U> Builder<E> op(Class<T1> anObjectClass, BiFunctionPerformer<T1, U, R> anOperation) {
            dynamicClassExtension.addExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension);
        }

        /**
         * Adds a void parameterless operation
         * @param anObjectClass object class
         * @param anOperation lambda that defines an operation
         * @return a copy of this {@code Builder}
         */
        public <T1> Builder<E> voidOp(Class<T1> anObjectClass, ConsumerPerformer<T1> anOperation) {
            dynamicClassExtension.addVoidExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension);
        }

        /**
         * Adds a void operation having one parameter
         * @param anObjectClass object class
         * @param anOperation lambda that defines an operation
         * @return a copy of this {@code Builder}
         */
        public <T1, U> Builder<E> voidOp(Class<T1> anObjectClass, BiConsumerPerformer<T1, U> anOperation) {
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

    //region Verbose Mode support methods
    boolean verbose;

    @Override
    public boolean isVerbose() {
        return verbose;
    }

    @Override
    public void setVerbose(boolean isVerbose) {
        if (verbose != isVerbose) {
            verbose = isVerbose;
            if (isVerbose())
                setupLogger();
        }
    }

    Logger logger;
    protected void setupLogger() {
        if (logger == null) {
            logger = Logger.getLogger(getClass().getName());
            logger.setLevel(Level.ALL);
        }
    }
    //endregion
}

