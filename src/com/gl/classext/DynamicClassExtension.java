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
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Collectors;

import static com.gl.classext.Aspects.*;
import static java.text.MessageFormat.*;

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
public class DynamicClassExtension extends AbstractClassExtension {
    private final ConcurrentHashMap<OperationKey, PerformerHolder<?>> operationsMap = new ConcurrentHashMap<>();
    private static final DynamicClassExtension dynamicClassExtension = new DynamicClassExtension();

    public static DynamicClassExtension sharedInstance() {
        return dynamicClassExtension;
    }

    static class PerformerHolder<R> {
        private final Performer<R> performer;
        private boolean async;
        private BiConsumer<?, ? super Throwable> whenComplete;
        private BiConsumer<Object, Object[]> before;
        private Consumer<Object> after;
        private String operationKey;

        PerformerHolder(Performer<R> aPerformer) {
            performer = aPerformer;
        }

        public Performer<R> getPerformer() {
            return performer;
        }

        public boolean isAsync() {
            return async;
        }
        public void setAsync(boolean aAsync) {
            async = aAsync;
            if (! async)
                whenComplete = null;
        }

        public BiConsumer<?, ? super Throwable> getWhenComplete() {
            return whenComplete;
        }

        public void setWhenComplete(BiConsumer<?, ? super Throwable> aWhenComplete) {
            whenComplete = aWhenComplete;
        }

        public BiConsumer<? super Object, Object[]> getBefore() {
            return before;
        }

        public void setBefore(BiConsumer<? super Object, Object[]> aBefore) {
            before = aBefore;
        }

        public Consumer<? super Object> getAfter() {
            return after;
        }

        public void setAfter(Consumer<? super Object> anAfter) {
            after = anAfter;
        }

        public String getOperationKey() {
            return operationKey;
        }

        public void setOperationKey(String aOperationKey) {
            operationKey = aOperationKey;
        }

        @Override
        public String toString() {
            return format("PerformerHolder[operationKey={0}, before=[{1}, after=[{2}]]]", operationKey, before, after);
        }
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
        /**
         * Performs an operation explicitly defined by its arguments that returns some result.
         *
         * @param operation operation name
         * @param anObject  an object to perform the operation for
         * @param anArgs    arguments
         * @return operation result
         */
        @Override
        default Void perform(String operation, Object anObject, Object[] anArgs) {
            accept((T) anObject);
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
        /**
         * Performs an operation explicitly defined by its arguments that returns some result.
         *
         * @param operation operation name
         * @param anObject  an object to perform the operation for
         * @param anArgs    arguments
         * @return operation result
         */
        @Override
        default Void perform(String operation, Object anObject, Object[] anArgs) {
            if (anArgs == null)
                accept(null, null);
            else
                accept((T) anObject, anArgs.length > 0 ? (U) anArgs[0] : null);
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
        /**
         * Performs an operation explicitly defined by its arguments that returns some result.
         *
         * @param operation operation name
         * @param anObject  an object to perform the operation for
         * @param anArgs    arguments
         * @return operation result
         */
        @Override
        default R perform(String operation, Object anObject, Object[] anArgs) {
            return apply((T) anObject);
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
        /**
         * Performs an operation explicitly defined by its arguments that returns some result.
         *
         * @param operation operation name
         * @param anObject  an object to perform the operation for
         * @param anArgs    arguments
         * @return operation result
         */
        @Override
        default R perform(String operation, Object anObject, Object[] anArgs) {
            return ((anArgs == null) ?
                    apply(null, null) :
                    apply((T) anObject, anArgs.length > 0 ? (U) anArgs[0] : null));
        }
    }

    static final String SUFFIX_BI = "Bi";

    <R, T, E> PerformerHolder<R> addExtensionOperation(Class<T> aClass,
                                         Class<E> anExtensionInterface,
                                         String anOperationName,
                                         FunctionPerformer<T, R> anOperation) {
        checkAddOperationArguments(aClass, anExtensionInterface, anOperationName, anOperation);

        OperationKey key = new OperationKey(aClass, anExtensionInterface, operationName(anOperationName, null));
        if (operationsMap.containsKey(key))
            duplicateOperationError(displayOperationName(anOperationName, false, null));
        PerformerHolder<R> result = new PerformerHolder<>(anOperation);
        result.setOperationKey(key.toString());
        operationsMap.put(key, result);
        return result;
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

    <R, T, U, E> PerformerHolder<R> addExtensionOperation(Class<T> aClass,
                                            Class<E> anExtensionInterface,
                                            String anOperationName,
                                            BiFunctionPerformer<T, U, R> anOperation) {
        checkAddOperationArguments(aClass, anExtensionInterface, anOperationName, anOperation);

        OperationKey key = new OperationKey(aClass, anExtensionInterface, operationName(anOperationName, SINGLE_PARAMETERS));
        if (operationsMap.containsKey(key))
            duplicateOperationError(displayOperationName(anOperationName, false, SINGLE_PARAMETERS));
        PerformerHolder<R> result = new PerformerHolder<>(anOperation);
        result.setOperationKey(key.toString());
        operationsMap.put(key, result);
        return result;
    }

    <T, E> PerformerHolder<Void> addVoidExtensionOperation(Class<T> aClass,
                                          Class<E> anExtensionInterface,
                                          String anOperationName,
                                          ConsumerPerformer<T> anOperation) {
        checkAddOperationArguments(aClass, anExtensionInterface, anOperationName, anOperation);

        OperationKey key = new OperationKey(aClass, anExtensionInterface, operationName(anOperationName, null));
        if (operationsMap.containsKey(key))
            duplicateOperationError(displayOperationName(anOperationName, true, null));
        PerformerHolder<Void> result = new PerformerHolder<>(anOperation);
        result.setOperationKey(key.toString());
        operationsMap.put(key, result);
        return result;
    }

    <T, U, E> PerformerHolder<Void> addVoidExtensionOperation(Class<T> aClass,
                                             Class<E> anExtensionInterface,
                                             String anOperationName,
                                             BiConsumerPerformer<T, U> anOperation) {
        checkAddOperationArguments(aClass, anExtensionInterface, anOperationName, anOperation);

        OperationKey key = new OperationKey(aClass, anExtensionInterface, operationName(anOperationName, SINGLE_PARAMETERS));
        if (operationsMap.containsKey(key))
            duplicateOperationError(displayOperationName(anOperationName, true, SINGLE_PARAMETERS));
        PerformerHolder<Void> result = new PerformerHolder<>(anOperation);
        result.setOperationKey(key.toString());
        operationsMap.put(key, result);
        return result;
    }

    <T, E> PerformerHolder<?> getExtensionOperation(Class<T> aClass,
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
            return (T) Proxy.newProxyInstance(getClass().getClassLoader(),
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
     * @param anObject             object to return an extension object for
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

    <T> Object performOperation(DynamicClassExtension aClassExtension, Object anObject, Class<T> anExtensionClass, Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Object result;

        ExtensionOperationResult extensionOperation = findExtensionOperation(anObject, anExtensionClass, method, args);
        PerformerHolder<?> performerHolder = extensionOperation.operation();
        result = performerHolder != null ?
                performDynamicOperation(aClassExtension, anObject, anExtensionClass, method, args, performerHolder) :
                performStaticOperation(aClassExtension, anObject, anExtensionClass, method, args);

        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> Object performDynamicOperation(DynamicClassExtension aClassExtension, Object anObject, Class<T> anExtensionClass, Method method, Object[] args, PerformerHolder<?> performerHolder) {
        Object result;
        if (aClassExtension.isVerbose())
            aClassExtension.logger.info(format("Performing dynamic operation for delegate \"{0}\" -> {1}", anObject, method));

        Pointcut aroundPointcut = isAspectsEnabled() && performerHolder.before == null ?
                getPointcut(anExtensionClass, anObject.getClass(), method.getName(), method.getParameterTypes(), AdviceType.AROUND) : null;
        Pointcut beforePointcut = isAspectsEnabled() && performerHolder.before == null && aroundPointcut == null ?
                getPointcut(anExtensionClass, anObject.getClass(), method.getName(), method.getParameterTypes(), AdviceType.BEFORE) : null;
        Pointcut afterPointcut = isAspectsEnabled() && performerHolder.after == null && aroundPointcut == null ?
                getPointcut(anExtensionClass, anObject.getClass(), method.getName(), method.getParameterTypes(), AdviceType.AFTER) : null;

        if (performerHolder.isAsync()) {
            CompletableFuture<?> future = CompletableFuture.supplyAsync(() -> performOperation(aClassExtension,
                    method.getName(), anObject, args,
                    performerHolder,
                    beforePointcut, afterPointcut, aroundPointcut));
            if (performerHolder.getWhenComplete() != null)
                future.whenComplete((BiConsumer) performerHolder.getWhenComplete());
            result = dummyReturnValue(method);
        } else {
            result = performOperation(aClassExtension,
                    method.getName(), anObject, args,
                    performerHolder,
                    beforePointcut, afterPointcut, aroundPointcut);
        }
        return result;
    }

    private <T> Object performStaticOperation(DynamicClassExtension aClassExtension, Object anObject, Class<T> anExtensionClass, Method method, Object[] args) throws IllegalAccessException, InvocationTargetException {
        Object result;

        Pointcut aroundPointcut = null;
        Pointcut beforePointcut = null;
        Pointcut afterPointcut = null;

        if (aClassExtension.isAspectsEnabled()) {
            aroundPointcut = getPointcut(anExtensionClass, anObject.getClass(), method.getName(), method.getParameterTypes(), AdviceType.AROUND);
            beforePointcut = aroundPointcut == null ? getPointcut(anExtensionClass, anObject.getClass(), method.getName(), method.getParameterTypes(), AdviceType.BEFORE) : null;
            afterPointcut = aroundPointcut == null ? getPointcut(anExtensionClass, anObject.getClass(), method.getName(), method.getParameterTypes(), AdviceType.AFTER) : null;
        }

        if (method.getDeclaringClass().isAssignableFrom(anObject.getClass())) {
            if (aClassExtension.isVerbose())
                aClassExtension.logger.info(format("Performing operation for delegate \"{0}\" -> {1}", anObject, method));
            if (beforePointcut != null) {
                if (aClassExtension.isVerbose())
                    aClassExtension.logger.info(formatAdvice(anObject, beforePointcut, AdviceType.BEFORE));
                beforePointcut.before(method.getName(), anObject, args);
            }

            if (aroundPointcut != null) {
                if (aClassExtension.isVerbose())
                    aClassExtension.logger.info(formatAdvice(anObject, aroundPointcut, AdviceType.AROUND));
                result = aroundPointcut.around((Performer<Object>) (operation, anObject1, anArgs) -> {
                    try {
                        return method.invoke(anObject1, anArgs);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }, method.getName(), anObject, args);
            } else {
                result = method.invoke(anObject, args);
            }

            if (afterPointcut != null) {
                if (aClassExtension.isVerbose())
                    aClassExtension.logger.info(formatAdvice(anObject, afterPointcut, AdviceType.AFTER));
                afterPointcut.after(result, method.getName(), anObject, args);
            }
        } else if (method.getDeclaringClass().isAssignableFrom(PrivateDelegateHolder.class)) {
            result = method.invoke((PrivateDelegateHolder)() -> anObject, args);
        } else {
            throw new IllegalArgumentException(format("No \"{0}\" operation for \"{1}\"",
                    displayOperationName(method.getName(), void.class.equals(method.getReturnType()), args), anObject));
        }
        return result;
    }

    private static Object performOperation(DynamicClassExtension aClassExtension,
                                           String anOperation, Object anObject, Object[] args,
                                           PerformerHolder<?> performerHolder,
                                           Pointcut aBeforePointcut, Pointcut anAfterPointcut, Pointcut anAroundPointcut) {
        Object result;

        if (aClassExtension.isAspectsEnabled()) {
            if (performerHolder.getBefore() != null) {
                if (aClassExtension.isVerbose())
                    aClassExtension.logger.info(formatAdvice(anObject, performerHolder, AdviceType.BEFORE));
                performerHolder.getBefore().accept(anObject, args);
            } else if (aBeforePointcut != null) {
                if (aClassExtension.isVerbose())
                    aClassExtension.logger.info(formatAdvice(anObject, aBeforePointcut, AdviceType.BEFORE));
                aBeforePointcut.before(anOperation, anObject, args);
            }
        }

        if (aClassExtension.isAspectsEnabled() && anAroundPointcut != null) {
            if (aClassExtension.isVerbose())
                aClassExtension.logger.info(formatAdvice(anObject, anAroundPointcut, AdviceType.AROUND));
            result = anAroundPointcut.around(performerHolder.getPerformer(), anOperation, anObject, args);
        } else {
            result = performerHolder.getPerformer().perform(anOperation, anObject, args);
        }

        if (aClassExtension.isAspectsEnabled()) {
            if (performerHolder.getAfter() != null) {
                if (aClassExtension.isVerbose())
                    aClassExtension.logger.info(formatAdvice(anObject, performerHolder, AdviceType.AFTER));
                performerHolder.getAfter().accept(result);
            } else if (anAfterPointcut != null) {
                if (aClassExtension.isVerbose())
                    aClassExtension.logger.info(formatAdvice(anObject, anAfterPointcut, AdviceType.AFTER));
                anAfterPointcut.after(result, anOperation, anObject, args);
            }
        }

        return result;
    }

    private static String formatAdvice(Object anObject, Object anAdvice, AdviceType anAdviceType) {
        return format("{0} -> {1} for {2}", anAdviceType, anAdvice, anObject);
    }

    private Object dummyReturnValue(Method aMethod) {
        Class<?> returnType = aMethod.getReturnType();
        if (returnType.isPrimitive() || Number.class.isAssignableFrom(returnType) || Boolean.class.isAssignableFrom(returnType))
            return 0;
        else
            return null;
    }

    /**
     * Checks if there is a valid extension defined for a passed object.An extension is considered valid if all its
     * methods meet one of the following criteria:
     * <ol>
     * <li>Annotated by {@code @OptionalMethod}</li>
     * <li>Correspond to registered operations</li>
     * <li>Match suitable methods in the {@code aClass} class</li>
     * </ol>
     *
     * This validation ensures that every method in the extension can be properly mapped and executed, maintaining
     * consistency between the extension interface and the underlying implementation.
     *
     * @param aClass         object to check an extension for
     * @param anExtensionInterface interface of extension
     * @throws IllegalArgumentException if an extension is invalid
     */
    public <T> void checkValid(Class<?> aClass, Class<T> anExtensionInterface) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionInterface);

        List<String> undefinedOperations = listUndefinedOperations(aClass, anExtensionInterface);
        if (! undefinedOperations.isEmpty())
            throw new IllegalArgumentException(format("No \"{0}\" operation for {1} in \"{2}\" extension",
                    undefinedOperations.getFirst(),
                    aClass,
                    anExtensionInterface.getName()));
    }

    /**
     * Lists all undefined operations. An operation is considered undefined if it is not annotated by {@code @OptionalMethod}
     * and meets one of the following criteria:
     * <ol>
     * <li>Not correspond to a registered operation</li>
     * <li>Do not match to a suitable method in the {@code aClass} class</li>
     * </ol>
     *
     * @param aClass         object to check an extension for
     * @param anExtensionInterface interface of extension
     * @return a list of all undefined operations
     */
    public <T> List<String> listUndefinedOperations(Class<?> aClass, Class<T> anExtensionInterface) {
        Objects.requireNonNull(aClass);
        Objects.requireNonNull(anExtensionInterface);

        List<String> result = new ArrayList<>();

        for (Method method : anExtensionInterface.getMethods()) {
            if (method.isAnnotationPresent(OptionalMethod.class))
                continue;
            ExtensionOperationResult extensionOperation = findExtensionOperation(aClass, anExtensionInterface, method, method.getParameterTypes());
            PerformerHolder<?> operation = extensionOperation.operation;
            if (operation == null && ! classHasMethod(aClass, method))
                result.add(displayOperationName(method.getName(), void.class.equals(method.getReturnType()), method.getParameterTypes()));
        }

        return result;
    }

    boolean classHasMethod(Class<?> aClass, Method aMethod) {
        boolean result = false;

        for (Method method : aClass.getMethods()) {
            if (method.getName().equals(aMethod.getName()) &&
                    method.getReturnType().equals(aMethod.getReturnType()) &&
                    Arrays.equals(method.getParameterTypes(), aMethod.getParameterTypes())) {
                result = true;
                break;
            }
        }

        return result;
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

    record ExtensionOperationResult(PerformerHolder<?> operation, Class<?> inClass) {}

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
                PerformerHolder<?> operation = getExtensionOperation(objectClass, extensionInterface, operationName);
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
        PerformerHolder<?> result;
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
        return format("{0} {1}({2})",
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

    String operationKeyToString(OperationKey anOperationKey, PerformerHolder<?> aPerformerHolder) {
        String operationName = anOperationKey.simpleOperationName();
        String result = null;
        Performer<?> performer = aPerformerHolder.performer;
        if (performer instanceof Function)
            result = format("T {0}()\n", operationName);
        else if (performer instanceof BiFunction)
            result = format("T {0}(T)\n", operationName);
        else if (performer instanceof Consumer)
            result = format("void {0}()\n", operationName);
        else if (performer instanceof BiConsumer)
            result = format("void {0}(T)\n", operationName);
        return result;
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
        private final String operationName;
        private final PerformerHolder<?> performerHolder;

        Builder(Class<E> anExtensionClass, DynamicClassExtension aDynamicClassExtension) {
            this(anExtensionClass, null, aDynamicClassExtension, null);
        }

        Builder(Class<E> anExtensionClass, String anOperationName, DynamicClassExtension aDynamicClassExtension) {
            this(anExtensionClass, anOperationName, aDynamicClassExtension, null);
        }

        Builder(Class<E> aExtensionClass, String aOperationName, DynamicClassExtension aDynamicClassExtension, PerformerHolder<?> aPerformerHolder) {
            extensionClass = aExtensionClass;
            operationName = aOperationName;
            dynamicClassExtension = aDynamicClassExtension;
            performerHolder = aPerformerHolder;
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
         * Alters an operation. Use following async(), before() and after() methods to alter the operation behaviour
         * @param anObjectClass object class
         * @param aParameterTypes arguments. Pass {@code null} or an empty array to define parameterless operation; otherwise pass
         *               an array of parameter types
         * @return a copy of this {@code Builder}
         */
        @SuppressWarnings("unused")
        public <T1> Builder<E> alterOp(Class<T1> anObjectClass, Class<?>[] aParameterTypes) {
            PerformerHolder<?> performerHolder = dynamicClassExtension.operationsMap.get(new OperationKey(anObjectClass, extensionClass,
                    dynamicClassExtension.operationName(operationName, aParameterTypes)));
            return new Builder<>(extensionClass, operationName, dynamicClassExtension, performerHolder);
        }

        /**
         * Adds a non-void parameterless operation
         * @param anObjectClass object class
         * @param anOperation lambda that defines an operation
         * @return a copy of this {@code Builder}
         */
        public <R, T1> Builder<E> op(Class<T1> anObjectClass, FunctionPerformer<T1, R> anOperation) {
            PerformerHolder<R> performerHolder = dynamicClassExtension.addExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension, performerHolder);
        }

        /**
         * Adds a non-void operation having one parameter
         * @param anObjectClass object class
         * @param anOperation lambda that defines an operation
         * @return a copy of this {@code Builder}
         */
        public <R, T1, U> Builder<E> op(Class<T1> anObjectClass, BiFunctionPerformer<T1, U, R> anOperation) {
            PerformerHolder<R> performerHolder = dynamicClassExtension.addExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension, performerHolder);
        }

        /**
         * Adds a void parameterless operation
         * @param anObjectClass object class
         * @param anOperation lambda that defines an operation
         * @return a copy of this {@code Builder}
         */
        public <T1> Builder<E> voidOp(Class<T1> anObjectClass, ConsumerPerformer<T1> anOperation) {
            PerformerHolder<Void> performerHolder = dynamicClassExtension.addVoidExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension, performerHolder);
        }

        /**
         * Adds a void operation having one parameter
         * @param anObjectClass object class
         * @param anOperation lambda that defines an operation
         * @return a copy of this {@code Builder}
         */
        public <T1, U> Builder<E> voidOp(Class<T1> anObjectClass, BiConsumerPerformer<T1, U> anOperation) {
            PerformerHolder<Void> performerHolder = dynamicClassExtension.addVoidExtensionOperation(anObjectClass, extensionClass, operationName, anOperation);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension, performerHolder);
        }

        /**
         * Specifies that current operation (previously defined by calling {@code op()} or {@code voidOp()}) must be run
         * asynchronously.<br/><br/>
         *
         * Asynchronous operations will not block the caller thread. For non-void operations - {@code 0} or
         * {@code null} will be returned immediately depending on operation return type.
         *
         * @return a copy of this {@code Builder}
         */
        public Builder<E> async() {
            return async(true);
        }

        /**
         * Specifies whether current operation (previously defined by calling {@code op()} or {@code voidOp()}) must be run
         * asynchronously or synchronously.<br/><br/>
         *
         * Asynchronous operations will not block the caller thread. For non-void operations - {@code 0} or
         * {@code null} will be returned immediately depending on operation return type.
         *
         * @param isAsync {@code true} if an operation must be run asynchronously; {@code false} otherwise
         * @return a copy of this {@code Builder}
         */
        public Builder<E> async(boolean isAsync) {
            checkPerformerHolder();

            performerHolder.setAsync(isAsync);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension, performerHolder);
        }

        /**
         * Specifies that current operation (previously defined by calling {@code op()} or {@code voidOp()}) must be run
         * asynchronously. Such an operation will not block the caller thread. For non-void operations - {@code 0} or
         * {@code null} will be returned immediately depending on operation return type.
         *
         * @param aWhenComplete a lambda to be called when an operation completes. It provides an operation result and
         *                      an exception if the operation fails.
         * @return a copy of this {@code Builder}
         */
        public <R> Builder<E> async(BiConsumer<? super R, ? super Throwable> aWhenComplete) {
            checkPerformerHolder();

            performerHolder.setAsync(true);
            performerHolder.setWhenComplete(aWhenComplete);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension, performerHolder);
        }

        /**
         * Specifies a code which must be performed before running current operation (previously defined by calling {@code op()}
         * or {@code voidOp()}).
         *
         * @param aBefore a lambda function should be performed before running current operation. It takes an object the operation
         *                should be performed for and an array of arguments.
         * @return a copy of this {@code Builder}
         */
        public Builder<E> before(BiConsumer<? super Object, Object[]> aBefore) {
            checkPerformerHolder();

            performerHolder.setBefore(aBefore);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension, performerHolder);
        }

        /**
         * Specifies a code which must be performed before running current operation (previously defined by calling {@code op()}
         * or {@code voidOp()}).
         *
         * @param anAfter a lambda function should be performed after running current operation. It takes a result of the operation
         * @return a copy of this {@code Builder}
         */
        public Builder<E> after(Consumer<? super Object> anAfter) {
            checkPerformerHolder();

            performerHolder.setAfter(anAfter);
            return new Builder<>(extensionClass, operationName, dynamicClassExtension, performerHolder);
        }

        private void checkPerformerHolder() {
            Objects.requireNonNull(performerHolder, format("No \"{0}\" operation is specified",
                    operationName != null ? operationName: "<any>"));
        }

        /**
         * Terminal operation (optional)
         * @return a {@code DynamicClassExtension} this builder was created for
         */
        public DynamicClassExtension build() {
            return dynamicClassExtension;
        }
    }

    /**
     * Returns a new instance of {@code AspectBuilder} used to build aspects for extensions
     * @return a new instance of {@code AspectBuilder}
     */
    public AspectBuilder<DynamicClassExtension> aspectBuilder() {
        return new AspectBuilder<>(this);
    }

    /**
     * Returns an extension (wrapper) for a lambda function that associates a custom description with it. Lambda functions
     * in Java cannot customize their {@code toString()} method output, which can be inconvenient for debugging. This
     * method allows specifying a textual description that will be returned by the {@code toString()} method of the
     * wrapper.
     *
     * @param aLambdaFunction      lambda function
     * @param aFunctionalInterface functional interface of lambda function
     * @param aDescription         description to be returned by {@code toString()}
     * @return description associated with the lambda function
     */
    public static <T> T lambdaWithDescription(Object aLambdaFunction, Class<T> aFunctionalInterface, String aDescription) {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
                builder(aFunctionalInterface).
                opName("toString").
                op(Object.class, o -> aDescription).
                build();
        dynamicClassExtension.setCacheEnabled(false);
        return dynamicClassExtension.extension(aLambdaFunction, aFunctionalInterface);
    }
}

