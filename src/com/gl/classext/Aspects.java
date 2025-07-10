package com.gl.classext;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;

/**
 * Defines classes to allow support of Aspects
 */
public class Aspects {
    /**
     * Defines Advice type
     */
    public enum AdviceType {
        BEFORE,
        AFTER,
        AROUND;
    }

    /**
     * Markup interface for all the advices
     */
    public interface Advice {
        /**
         * Returns {@code AdviceType} for this advice
         *
         * @return an {@code AdviceType}
         */
        default AdviceType getAdviceType() {
            throw new UnsupportedOperationException("getAdviceType()");
        }
    }

    /**
     * Functional interface for {@code AdviceType.BEFORE} advices.
     */
    @FunctionalInterface
    public interface BeforeAdvice extends Advice {
        /**
         * Applies this function to the given arguments
         *
         * @param operation an operation to be performed
         * @param object    object to perform the operation for
         * @param args      arguments for the operation
         */
        void accept(String operation, Object object, Object[] args);

        /**
         * {@inheritDoc}
         */
        default AdviceType getAdviceType() {
            return AdviceType.BEFORE;
        }
    }

    /**
     * Functional interface for {@code AdviceType.AFTER} advices.
     */
    @FunctionalInterface
    public interface AfterAdvice extends Advice {
        /**
         * Applies this function to the given arguments
         *
         * @param result    result of an operation
         * @param operation the operation to be performed
         * @param object    object to perform the operation for
         * @param args      arguments for the operation
         */
        void accept(Object result, String operation, Object object, Object[] args);

        /**
         * {@inheritDoc}
         */
        @Override
        default AdviceType getAdviceType() {
            return AdviceType.AFTER;
        }
    }

    /**
     * Functional interface for {@code AdviceType.AROUND} advices.
     */
    @FunctionalInterface
    public interface AroundAdvice extends Advice {
        /**
         * Applies this function to the given arguments. This function essentially replaces standard handling of the
         * operation. Use the {@code applyDefault} method to perform standard handling.
         *
         * @param performer performer object that is responsible for actual operation performing.  It is intentionally
         *                  passed as an {@code Object} to avoid its altering or hacking somehow because it must be
         *                  passed further to the {@code applyDefault} "as is".
         * @param operation the operation to be performed
         * @param object    object to perform the operation for
         * @param args      arguments for the operation
         */
        Object apply(Object performer, String operation, Object object, Object[] args);

        /**
         * {@inheritDoc}
         */
        @Override
        default AdviceType getAdviceType() {
            return AdviceType.AROUND;
        }

        /**
         * Performs default handling of an operation. An around lambda function should call this method to allow default
         * handling of an operation e.g. calling an underlying method, performing a dynamic operation, or transferring
         * control to the next around advice in the chain. Generally, this method should be called only once inside an
         * around lambda function to avoid side effects. Though some advice implementation like {@code RetryAdvice} may
         * call the {@code applyDefault()} several times in attempts to recover after some failure.
         * <pre><code>
         * DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
         *     aspectBuilder().
         *         extensionInterface(ItemInterface.class).
         *             objectClass(Item.class).
         *                 operation("toString()").
         *                     advices(builder -> {
         *                         builder.around((performer, operation, object, args) -> "BEFORE " + AroundAdvice.applyDefault(performer, operation, object, args) + " AFTER").
         *                     }).
         *     build();
         * </code></pre>
         *
         * @param performer performer object that is responsible for underlying operation performing.
         * @param operation the operation to be performed
         * @param object    object to perform the operation for
         * @param args      arguments for the operation
         */
        @SuppressWarnings("rawtypes")
        static Object applyDefault(Object performer, String operation, Object object, Object[] args) {
            Objects.requireNonNull(performer);
            Objects.requireNonNull(object);

            if (performer instanceof Performer)
                return ((Performer) performer).perform(operation, object, args);
            else
                throw new IllegalStateException("Unsupported performer for around advice: " + performer);
        }
    }

    protected interface Pointcut {
        void before(String anOperation, Object anObject, Object[] anArguments);

        void after(Object aResult, String anOperation, Object anObject, Object[] anArguments);

        Object around(Object aPerformer, String anOperation, Object anObject, Object[] anArguments);

        default Predicate<Class<?>> getObjectClass() {
            return null;
        }
    }

    protected static class Pointcuts implements Pointcut {
        private List<Pointcut> pointcuts = new ArrayList<>();

        public boolean isEmpty() {
            return pointcuts.isEmpty();
        }

        public void addPointcut(Pointcut aPointcut) {
            // keep out same pointcuts; walk though manually as Pointcut.equals() ignores advice lambda
            if (pointcuts.stream().noneMatch(pointcut -> pointcut == aPointcut))
                pointcuts.add(aPointcut);
        }

        @Override
        public void before(String anOperation, Object anObject, Object[] anArguments) {
            for (Pointcut pointcut : pointcuts)
                pointcut.before(anOperation, anObject, anArguments);
        }

        @Override
        public void after(Object aResult, String anOperation, Object anObject, Object[] anArguments) {
            for (Pointcut pointcut : pointcuts)
                pointcut.after(aResult, anOperation, anObject, anArguments);
        }

        private Object getChainedPerformer(int anIndex, Object aPerformer, String anOperation, Object anObject, Object[] anArguments) {
            return anIndex == pointcuts.size() - 1 ?
                    aPerformer :
                    (Performer<Object>) (operation1, object1, args1) ->
                            pointcuts.get(anIndex + 1).around(getChainedPerformer(anIndex + 1, aPerformer, anOperation, anObject, anArguments), operation1, object1, args1);
        }

        @Override
        public Object around(Object aPerformer, String anOperation, Object anObject, Object[] anArguments) {
            return pointcuts.getFirst().around(getChainedPerformer(0, aPerformer, anOperation, anObject, anArguments), anOperation, anObject, anArguments);
        }

        public void filterOutDuplicates() {
            if (pointcuts.size() > 1) { // speed optimization
                LinkedHashSet<Predicate<Class<?>>> objectClasses = new LinkedHashSet<>();
                for (Pointcut pointcut : pointcuts)
                    objectClasses.add(pointcut.getObjectClass());

                pointcuts = pointcuts.stream().filter(pointcut -> pointcut.getObjectClass().equals(objectClasses.getLast())).collect(Collectors.toList());
            }
        }
    }

    protected static class SinglePointcut implements Pointcut {
        private final Predicate<Class<?>> extensionInterface;

        private final Predicate<Class<?>> objectClass;

        private final BiPredicate<String, Class<?>[]> operation;
        private final AdviceType adviceType;
        private final Object advice;
        private boolean enabled = true;
        private Object id;

        public SinglePointcut(Predicate<Class<?>> anExtensionInterface,
                              Predicate<Class<?>> anObjectClass,
                              BiPredicate<String, Class<?>[]> anOperation,
                              AdviceType anAdviceType, Object anAdvice,
                              Object anID) {
            this.extensionInterface = anExtensionInterface;
            this.objectClass = anObjectClass;
            this.operation = anOperation;
            this.adviceType = anAdviceType;
            if (anAdvice != null) // can be null for matching purposes
                switch (this.adviceType) {
                    case BEFORE:
                        if (!(anAdvice instanceof BeforeAdvice))
                            throw new IllegalArgumentException("Advice(BEFORE) is not a BeforeAdvice");
                        break;
                    case AFTER:
                        if (!(anAdvice instanceof AfterAdvice))
                            throw new IllegalArgumentException("Advice(AFTER) is not a AfterAdvice");
                        break;
                    case AROUND:
                        if (!(anAdvice instanceof AroundAdvice))
                            throw new IllegalArgumentException("Advice(AROUND) is not a AroundAdvice");
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + this.adviceType);
                }
            this.advice = anAdvice;
            this.id = anID;
        }

        @Override
        public Predicate<Class<?>> getObjectClass() {
            return objectClass;
        }

        public Object getID() {
            return id;
        }

        public void setID(Object anID) {
            id = anID;
        }

        public boolean accept(Class<?> anExtensionInterface, Class<?> anObjectClass,
                              String anOperation, Class<?>[] anOperationParameterTypes,
                              AdviceType anAdviceType) {
            return adviceType == anAdviceType &&
                    operation.test(anOperation, anOperationParameterTypes) &&
                    extensionInterface.test(anExtensionInterface) &&
                    objectClass.test(anObjectClass);
        }

        public boolean accept(String anOperation, Class<?>[] anOperationParameterTypes,
                              AdviceType anAdviceType) {
            return adviceType == anAdviceType && operation.test(anOperation, anOperationParameterTypes);
        }

        public boolean accept(Class<?> anExtensionInterface, Class<?> anObjectClass) {
            return extensionInterface.test(anExtensionInterface) &&
                    objectClass.test(anObjectClass);
        }

        @Override
        public void before(String anOperation, Object anObject, Object[] anArguments) {
            ((BeforeAdvice) advice).accept(anOperation, anObject, anArguments);
        }

        @Override
        public void after(Object aResult, String anOperation, Object anObject, Object[] anArguments) {
            ((AfterAdvice) advice).accept(aResult, anOperation, anObject, anArguments);
        }

        @Override
        public Object around(Object aPerformer, String anOperation, Object anObject, Object[] anArguments) {
            return ((AroundAdvice) advice).apply(aPerformer, anOperation, anObject, anArguments);
        }

        @Override
        public boolean equals(Object aO) {
            if (aO == this)
                return true;

            if (aO == null || getClass() != aO.getClass()) return false;

            SinglePointcut pointcut = (SinglePointcut) aO;
            return adviceType == pointcut.adviceType &&
                    objectClass.equals(pointcut.objectClass) &&
                    extensionInterface.equals(pointcut.extensionInterface) &&
                    operation.equals(pointcut.operation) &&
                    Objects.equals(id, pointcut.getID());
        }

        @Override
        public int hashCode() {
            int result = extensionInterface.hashCode();
            result = 31 * result + objectClass.hashCode();
            result = 31 * result + operation.hashCode();
            result = 31 * result + adviceType.hashCode();
            return result;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean aEnabled) {
            enabled = aEnabled;
        }

        @Override
        public String toString() {
            return "Pointcut{" +
                    "extensionInterface=" + extensionInterface +
                    ", objectClass=" + objectClass +
                    ", operation=" + operation +
                    ", adviceType=" + adviceType +
                    '}';
        }
    }

    /**
     * Builder that allows adding Aspects to the operations
     *
     * @param <T>
     */
    public static class AspectBuilder<T extends AbstractClassExtension> {

        static class OperationPredicate implements BiPredicate<String, Class<?>[]> {
            private final String operation;

            public OperationPredicate(String aOperation) {
                operation = aOperation;
            }

            @Override
            public boolean test(String operation, Class<?>[] parameterTypes) {
                return operationNameMatches(operation, this.operation) &&
                        operationParameterTypesMatch(this.operation, parameterTypes);
            }

            @Override
            public String toString() {
                return format("OperationPredicate(\"{0}\")", this.operation);
            }

            @Override
            public boolean equals(Object aO) {
                if (aO == null || getClass() != aO.getClass()) return false;

                OperationPredicate that = (OperationPredicate) aO;
                return operation.equals(that.operation);
            }

            @Override
            public int hashCode() {
                return operation.hashCode();
            }
        }

        static class ClassNamePredicate implements Predicate<Class<?>> {
            private final String className;

            public ClassNamePredicate(String aClassName) {
                className = aClassName;
            }

            @Override
            public boolean test(Class<?> aClass) {
                return matches(
                        className,
                        className.contains(".") ? aClass.getName() : aClass.getSimpleName());
            }

            @Override
            public String toString() {
                return format("ClassNamePredicate(\"{0}\")", className);
            }

            @Override
            public boolean equals(Object aO) {
                if (aO == null || getClass() != aO.getClass()) return false;

                ClassNamePredicate that = (ClassNamePredicate) aO;
                return className.equals(that.className);
            }

            @Override
            public int hashCode() {
                return className.hashCode();
            }
        }

        static class ClassPredicate implements Predicate<Class<?>> {
            private final Class<?>[] classes;

            public ClassPredicate(Class<?>... aClasses) {
                classes = aClasses;
            }

            @Override
            public boolean test(Class<?> aClass) {
                return Arrays.asList(classes).contains(aClass);
            }

            @Override
            public String toString() {
                return format("ClassPredicate(\"{0}\")", Arrays.toString(classes));
            }

            @Override
            public boolean equals(Object aO) {
                if (aO == null || getClass() != aO.getClass()) return false;

                ClassPredicate that = (ClassPredicate) aO;
                return Arrays.equals(classes, that.classes);
            }

            @Override
            public int hashCode() {
                return Arrays.hashCode(classes);
            }
        }

        private final T classExtension;
        private Predicate<Class<?>> extensionInterface;
        private Predicate<Class<?>> objectClass;
        private OperationPredicate operation;

        /**
         * Creates a new instance of {@code AspectBuilder} and supplies a {@code ClassExtension} to build Aspects for
         *
         * @param aClassExtension a {@code ClassExtension} to build Aspects for
         */
        public AspectBuilder(T aClassExtension) {
            classExtension = aClassExtension;
        }

        /**
         * Optional terminal operation that simply returns a {@code ClassExtension}
         *
         * @return a {@code ClassExtension} to build Aspects for
         */
        public T build() {
            return classExtension;
        }

        /**
         * Specifies an extension interface the Aspects should be added fpr
         *
         * @param anInterfaceName name of an extension interface. It is possible to use * and ? wildcards to define a
         *                        pattern all extension interfaces should match.
         * @return this Builder
         */
        public AspectBuilder<T> extensionInterface(String anInterfaceName) {
            Objects.requireNonNull(anInterfaceName);

            extensionInterface = new ClassNamePredicate(anInterfaceName);
            return this;
        }

        /**
         * Specifies extension interfaces the Aspects should be added fpr
         *
         * @param anExtensionInterfaces extension interfaces
         * @return this Builder
         */
        public AspectBuilder<T> extensionInterface(Class<?>... anExtensionInterfaces) {
            Objects.requireNonNull(anExtensionInterfaces);

            extensionInterface = new ClassPredicate(anExtensionInterfaces);
            return this;
        }

        /**
         * Specifies an object class the Aspects should be added fpr
         *
         * @param anObjectClassName name of an object class. It is possible to use * and ? wildcards to define a *
         *                          pattern all object classes should match.
         * @return this Builder
         */
        public AspectBuilder<T> objectClass(String anObjectClassName) {
            Objects.requireNonNull(anObjectClassName);

            objectClass = new ClassNamePredicate(anObjectClassName);
            return this;
        }

        /**
         * Specifies an object classes the Aspects should be added fpr
         *
         * @param anObjectClasses an object classes
         * @return this Builder
         */
        public AspectBuilder<T> objectClass(Class<?>... anObjectClasses) {
            Objects.requireNonNull(anObjectClasses);

            objectClass = new ClassPredicate(anObjectClasses);
            return this;
        }

        /**
         * Specifies an operation the Aspects should be added fpr
         *
         * @param anOperation name of an operation. It is possible to use * and ? wildcards to define a * pattern all
         *                    operations should match. Tip: specify * to accept any operations.
         * @return this Builder
         */
        public AspectBuilder<T> operation(String anOperation) {
            Objects.requireNonNull(anOperation);

            operation = new OperationPredicate(anOperation);
            return this;
        }

        private static boolean operationParameterTypesMatch(String anOperation, Class<?>[] aParameterTypes) {
            String[] operationParameters = getOperationParameters(anOperation);
            boolean result = false;
            if ("*".equals(anOperation) || operationParameters.length == aParameterTypes.length) {
                result = operationParameters.length == 0;
                for (int i = 0; i < operationParameters.length && !result; i++)
                    result = matches(operationParameters[i], aParameterTypes[i].getName()) ||
                            matches(operationParameters[i], aParameterTypes[i].getSimpleName());
            }

            return result;
        }

        private static boolean operationNameMatches(String anOperation, String anOperationPattern) {
            return matches(getOperationName(anOperationPattern), getOperationName(anOperation));
        }

        private static String getOperationName(String anOperation) {
            int index = anOperation.indexOf("(");
            return index > 0 ? anOperation.substring(0, index) : anOperation;
        }

        static final String[] EMPTY_PARAMETERS = new String[0];

        private static String[] getOperationParameters(String anOperation) {
            String[] result = EMPTY_PARAMETERS;
            if (anOperation.contains("(")) {
                String params = anOperation.replaceAll(".*\\((.*)\\).*", "$1");
                result = params.split("\\s*,\\s*");
                for (int i = 0; i < result.length; i++)
                    result[i] = result[i].trim();
            }
            return result.length == 1 && "".equals(result[0]) ? EMPTY_PARAMETERS : result;
        }

        /**
         * Adds a {@code AspectType.BEFORE} advice for a previously specified combination of extension interface(s),
         * object class(es) and operation(s)
         *
         * @param aBefore advice to be added
         * @return this Builder
         */
        public AspectBuilder<T> before(BeforeAdvice aBefore) {
            return before(aBefore, null);
        }

        /**
         * Adds a {@code AspectType.BEFORE} advice for a previously specified combination of extension interface(s),
         * object class(es) and operation(s)
         *
         * @param aBefore advice to be added
         * @param anID    identifier
         * @return this Builder
         */
        public AspectBuilder<T> before(BeforeAdvice aBefore, Object anID) {
            Objects.requireNonNull(aBefore);
            checkPrerequisites();
            classExtension.addPointcut(new SinglePointcut(extensionInterface, objectClass, operation, AdviceType.BEFORE, aBefore, anID));
            return this;
        }

        /**
         * Adds a {@code AspectType.AFTER} advice for a previously specified combination of extension interface(s),
         * object class(es) and operation(s)
         *
         * @param anAfter advice to be added
         * @return this Builder
         */
        public AspectBuilder<T> after(AfterAdvice anAfter) {
            return after(anAfter, null);
        }

        /**
         * Adds a {@code AspectType.AFTER} advice for a previously specified combination of extension interface(s),
         * object class(es) and operation(s)
         *
         * @param anAfter advice to be added
         * @param anID    identifier
         * @return this Builder
         */
        public AspectBuilder<T> after(AfterAdvice anAfter, Object anID) {
            Objects.requireNonNull(anAfter);
            checkPrerequisites();
            classExtension.addPointcut(new SinglePointcut(extensionInterface, objectClass, operation, AdviceType.AFTER, anAfter, anID));
            return this;
        }

        /**
         * Adds a {@code AspectType.AROUND} advice for a previously specified combination of extension interface(s),
         * object class(es) and operation(s)
         *
         * @param anAround advice to be added
         * @return this Builder
         */
        public AspectBuilder<T> around(AroundAdvice anAround) {
            return around(anAround, null);
        }

        /**
         * Adds a {@code AspectType.AROUND} advice for a previously specified combination of extension interface(s),
         * object class(es) and operation(s)
         *
         * @param anAround advice to be added
         * @param anID     identifier
         * @return this Builder
         */
        public AspectBuilder<T> around(AroundAdvice anAround, Object anID) {
            Objects.requireNonNull(anAround);
            checkPrerequisites();
            classExtension.addPointcut(new SinglePointcut(extensionInterface, objectClass, operation, AdviceType.AROUND, anAround, anID));
            return this;
        }

        /**
         * Adds several advices for a previously specified combination of extension interface(s), object class(es) and
         * operation(s)
         *
         * @param anAdvicesSupplier a supplier function called to add several advices at once
         * @return this Builder
         */
        public AspectBuilder<T> advices(Consumer<AspectBuilder<T>> anAdvicesSupplier) {
            Objects.requireNonNull(anAdvicesSupplier);
            checkPrerequisites();
            anAdvicesSupplier.accept(this);
            return this;
        }

        /**
         * Adds several advices for a previously specified combination of extension interface(s), object class(es) and
         * operation(s)
         *
         * @param anAdvices advices to add
         * @return this Builder
         */
        public AspectBuilder<T> advices(Advice[] anAdvices) {
            Objects.requireNonNull(anAdvices);
            checkPrerequisites();

            for (Advice advice : anAdvices)
                classExtension.addPointcut(new SinglePointcut(extensionInterface, objectClass, operation,
                        advice.getAdviceType(), advice, null));

            return this;
        }

        /**
         * Removes advice(s) for a previously specified combination of extension interface(s), object class(es) and
         * operation(s)
         *
         * @param anAdvices advices to be removed
         * @return this Builder
         */
        public AspectBuilder<T> remove(AdviceType... anAdvices) {
            return remove(null, anAdvices);
        }

        /**
         * Removes advice(s) for a previously specified combination of extension interface(s), object class(es) and
         * operation(s)
         *
         * @param anID      identifier
         * @param anAdvices advices to be removed
         * @return this Builder
         */
        public AspectBuilder<T> remove(Object anID, AdviceType... anAdvices) {
            checkPrerequisites();
            for (AdviceType advice : anAdvices)
                classExtension.removePointcut(new SinglePointcut(extensionInterface, objectClass, operation, advice, null, anID));
            return this;
        }

        /**
         * Enables advice(s) for a previously specified combination of extension interface(s), object class(es) and
         * operation(s)
         *
         * @param isEnabled enabled value
         * @param anAdvices advices to be enabled
         * @return this Builder
         */
        public AspectBuilder<T> enabled(boolean isEnabled, AdviceType... anAdvices) {
            return enabled(null, isEnabled, anAdvices);
        }

        /**
         * Enables advice(s) for a previously specified combination of extension interface(s), object class(es) and
         * operation(s)
         *
         * @param anID      identifier
         * @param isEnabled enabled value
         * @param anAdvices advices to be enabled
         * @return this Builder
         */
        public AspectBuilder<T> enabled(Object anID, boolean isEnabled, AdviceType... anAdvices) {
            checkPrerequisites();
            for (AdviceType advice : anAdvices)
                classExtension.setPointcutEnabled(new SinglePointcut(extensionInterface, objectClass, operation, advice, null, anID), isEnabled);
            return this;
        }

        private void checkPrerequisites() {
            Objects.requireNonNull(objectClass, "Object class is not specified");
            Objects.requireNonNull(extensionInterface, "Extension interface is not specified");
            Objects.requireNonNull(operation, "Operation name is not specified");
        }

        private static boolean matches(String aPattern, String aString) {
            return Pattern.matches(
                    aPattern.replace("*", ".*").replace("?", "."),
                    aString);
        }
    }

    /**
     * Advice (before) that allows logging operations
     */
    public static class LogBeforeAdvice implements BeforeAdvice {
        final Logger logger;

        /**
         * Creates a new instance of {@code LogBeforeAdvice}
         */
        public LogBeforeAdvice() {
            logger = Logger.getLogger(getClass().getName());
        }

        /**
         * Creates a new instance of {@code LogBeforeAdvice} and supplies a {@code Logger} to it
         *
         * @param aLogger logger
         */
        public LogBeforeAdvice(Logger aLogger) {
            logger = aLogger;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void accept(String operation, Object object, Object[] args) {
            logger.info(format("BEFORE: {0} -> {1}({2})",
                    object,
                    operation, args != null ? Arrays.toString(args) : ""));
        }
    }

    /**
     * Advice (after) that allows logging results of operations
     */
    public static class LogAfterAdvice implements AfterAdvice {
        final Logger logger;

        /**
         * Creates a new instance of {@code LogAfterAdvice}
         */
        public LogAfterAdvice() {
            logger = Logger.getLogger(getClass().getName());
        }

        /**
         * Creates a new instance of {@code LogAfterAdvice} and supplies a {@code Logger} to it
         *
         * @param aLogger logger
         */
        public LogAfterAdvice(Logger aLogger) {
            logger = aLogger;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void accept(Object result, String operation, Object object, Object[] args) {
            logger.info(format("AFTER: {0} -> {1}({2}) = {3}",
                    object,
                    operation,
                    args != null ? Arrays.toString(args) : "",
                    result));
        }
    }

    /**
     * Advice (around) that allows logging perform times for operations
     */
    public static class LogPerformTimeAdvice implements AroundAdvice {
        final Logger logger;
        final String loggerInfoPattern;

        /**
         * Creates a new instance of {@code LogPerformTimeAdvice}
         */
        public LogPerformTimeAdvice() {
            this(null, null);
        }

        /**
         * Creates a new instance of {@code LogPerformTimeAdvice} and supplies a {@code Logger} to it
         *
         * @param aLogger logger
         */
        public LogPerformTimeAdvice(Logger aLogger) {
            this(aLogger, null);
        }

        /**
         * Creates a new instance of {@code LogPerformTimeAdvice}
         *
         * @param aLogger            logger
         * @param aLoggerInfoPattern pattern to format log messages in the {@code MessageFormat} format, where
         *                           {@code {0}} - operation and {@code {1}} - perform time
         */
        public LogPerformTimeAdvice(Logger aLogger, String aLoggerInfoPattern) {
            logger = aLogger != null ? aLogger : Logger.getLogger(getClass().getName());
            loggerInfoPattern = aLoggerInfoPattern != null ? aLoggerInfoPattern : "Perform time for \"{0}\" is {1} ms.";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object apply(Object performer, String operation, Object object, Object[] args) {
            long startTime = System.currentTimeMillis();

            Object result = AroundAdvice.applyDefault(performer, operation, object, args);
            logger.info(format(loggerInfoPattern, operation, System.currentTimeMillis() - startTime));
            return result;
        }
    }

    /**
     * Advice (around) that allows tracking all property changes.
     */
    public static class PropertyChangeAdvice implements AroundAdvice {
        /**
         * Creates a new instance of {@code PropertyChangeAdvice} and supplies it with a {@code PropertyChangeListener}
         *
         * @param aPropertyChangeListener property change listener
         */
        public PropertyChangeAdvice(PropertyChangeListener aPropertyChangeListener) {
            Objects.requireNonNull(aPropertyChangeListener);

            propertyChangeListener = aPropertyChangeListener;
        }

        private final PropertyChangeListener propertyChangeListener;

        private Object getPropertyValue(Object anObject, String aPropertyName, Object aDefaultValue) {
            Object result = aDefaultValue;
            Method method = null;

            try {
                method = anObject.getClass().getMethod("get" + aPropertyName);
            } catch (NoSuchMethodException aE) {
                // do nothing
            }
            if (method == null)
                try {
                    method = anObject.getClass().getMethod("is" + aPropertyName);
                } catch (NoSuchMethodException aE) {
                    // do nothing
                }

            if (method != null) {
                try {
                    result = method.invoke(anObject, (Object[]) null);
                } catch (Exception ex) {
                    // do nothing
                }
            }

            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object apply(Object performer, String operation, Object object, Object[] args) {
            final String prefix = "set";
            boolean isProperty = operation.startsWith(prefix) && operation.length() > prefix.length() &&
                    args != null && args.length == 1;
            String propertyName = null;
            Object oldPropertyValue = null;

            if (isProperty) {
                propertyName = operation.substring(prefix.length());
                oldPropertyValue = getPropertyValue(object, propertyName, null);
            }

            Object result = AroundAdvice.applyDefault(performer, operation, object, args);

            if (isProperty) {
                Object newPropertyValue = getPropertyValue(object, propertyName, args[0]);
                if (!Objects.equals(oldPropertyValue, newPropertyValue))
                    propertyChangeListener.propertyChange(new PropertyChangeEvent(this,
                            propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1),
                            oldPropertyValue,
                            newPropertyValue));
            }

            return result;
        }
    }

    /**
     * Advice (around) that automatically retries failed operations. Executes the operation multiple times upon
     * exception, up to a specified retry limit, before propagating the final failure. Default policy is "retry after
     * any exception" but it is possible to fine-tune that behavior to provide {@code resultChecker} that allows
     * checking whether results of exceptions are errors that can be recovered by retrying the operation.
     */
    public static class RetryAdvice implements AroundAdvice {
        private final int retryCount;
        private final long sleepTime;
        private final Logger logger;
        private final BiPredicate<Object, Throwable> resultChecker;

        /**
         * Creates new advice with 500ms sleep time, no logger, and no result checker
         *
         * @param aRetryCount retry count
         */
        public RetryAdvice(int aRetryCount) {
            this(aRetryCount, 500, null, null);
        }

        /**
         * Creates new advice with 500ms sleep time and no logger
         *
         * @param aRetryCount    retry count
         * @param aResultChecker result checking lambda function
         */
        public RetryAdvice(int aRetryCount, BiPredicate<Object, Throwable> aResultChecker) {
            this(aRetryCount, 500, null, aResultChecker);
        }

        /**
         * Creates a new advice
         *
         * @param aRetryCount    retry count
         * @param aSleepTime     sleep before the next try
         * @param aLogger        logger
         * @param aResultChecker result checking lambda function that allows checking whether operation results or
         *                       exceptions are errors that can be recovered by retrying the operation. The
         *                       {@code aResultChecker} must return {@code true} if operation is completed and should
         *                       not be retried. If no {@code aResultChecker} is specified - default "retry after any
         *                       exception" policy will be applied.
         */
        public RetryAdvice(int aRetryCount, long aSleepTime, Logger aLogger, BiPredicate<Object, Throwable> aResultChecker) {
            retryCount = aRetryCount;
            sleepTime = Math.max(0, aSleepTime);
            logger = aLogger;
            resultChecker = aResultChecker;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object apply(Object performer, String operation, Object object, Object[] args) {
            Object result = null;
            RuntimeException resultEx = null;
            for (int i = 0; i < retryCount; i++) {
                try {
                    result = AroundAdvice.applyDefault(performer, operation, object, args);
                    resultEx = null;
                    if (resultChecker != null && resultChecker.test(result, null))
                        break; // result is not an error - return
                    else if (logger != null) {
                        logger.severe(format("Failed operation: {0} with result: {1}", operation, result));
                    }
                } catch (Throwable ex) {
                    resultEx = new RuntimeException(ex);
                    if (logger != null)
                        logger.log(Level.SEVERE, "Failed operation: " + operation, ex);
                    if (resultChecker != null && resultChecker.test(null, ex))
                        break; // exception is not recoverable - return
                }
                if (i < retryCount - 1 && sleepTime > 0) {
                    try {
                        if (logger != null)
                            logger.info("Sleeping for: " + sleepTime);
                        Thread.sleep(sleepTime);
                        if (logger != null)
                            logger.info("Retrying operation: " + operation);
                    } catch (Exception ex) {
                        // do nothing
                    }
                }
            }
            if (resultEx != null)
                throw resultEx;

            return result;
        }
    }

    /**
     * Utility method that returns an extension with added performance logging for all operations. Note: returned
     * extensions will not be cached.
     *
     * @param anObject             object to return an extension object for
     * @param anExtensionInterface interface of extension object to be returned
     * @return an extension object
     */
    public static <T> T logPerformanceExtension(Object anObject, Class<T> anExtensionInterface) {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
                aspectBuilder().
                objectClass("*").
                extensionInterface("*").
                operation("*").
                around(new LogPerformTimeAdvice()).
                build();
        return dynamicClassExtension.extensionNoCache(anObject, null, anExtensionInterface);
    }

    /**
     * Utility method that returns an extension with logging added before and after all the operations. Note: returned
     * extensions will not be cached.
     *
     * @param anObject             object to return an extension object for
     * @param anExtensionInterface interface of extension object to be returned
     * @return an extension object
     */
    public static <T> T logBeforeAndAfterExtension(Object anObject, Class<T> anExtensionInterface) {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
                aspectBuilder().
                objectClass("*").
                extensionInterface("*").
                operation("*").
                before(new LogBeforeAdvice()).
                after(new LogAfterAdvice()).
                build();
        return dynamicClassExtension.extensionNoCache(anObject, null, anExtensionInterface);
    }


    /**
     * Utility method that returns an extension with the added ability to listen for property changes. Note: returned
     * extensions will not be cached.
     *
     * @param anObject                object to return an extension object for
     * @param anExtensionInterface    interface of extension object to be returned
     * @param aPropertyChangeListener property change listener
     * @return an extension object
     */
    public static <T> T propertyChangeExtension(Object anObject, Class<T> anExtensionInterface, PropertyChangeListener aPropertyChangeListener) {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
                aspectBuilder().
                objectClass("*").
                extensionInterface("*").
                operation("set*(*)").
                around(new PropertyChangeAdvice(aPropertyChangeListener)).
                build();
        return dynamicClassExtension.extensionNoCache(anObject, null, anExtensionInterface);
    }

    /**
     * A record that holds all the operation perform arguments. This record should be used by underlying cache
     * implementation to compose actual keys for cached values.
     *
     * @param operation operation
     * @param object    an object to perform the operation for
     * @param args      arguments
     */
    public record OperationPerformKey(String operation, Object object, Object[] args) {
    }

    /**
     * An interface a cache implementation should implement to allow it works with {@code CachedValueAdvice}. Or it can
     * be used as a lambda function to adopt existing cache implementation without the need to subclass
     */
    @FunctionalInterface
    public interface CachedValueProvider {
        /**
         * Gets a cached value or creates the value via the {@code aValueSupplier}, caches and returns it
         *
         * @param key            a key should be used by underlying cache implementation to compose actual keys for
         *                       cached values
         * @param aValueSupplier a supplier that will be called tu supply a value to be cached and returned
         * @return a value
         */
        Object getOrCreate(OperationPerformKey key, Supplier<?> aValueSupplier);
    }

    /**
     * Advice (around) that allows caching of operation results
     */
    public static class CachedValueAdvice implements AroundAdvice {
        private final Logger logger;
        private final CachedValueProvider cachedValueProvider;

        /**
         * Creates a new instance of {@code CachedValueAdvice}
         *
         * @param aCachedValueProvider a cached value provider that allows getting cached values or cache them if
         *                             needed
         */
        @SuppressWarnings("unused")
        public CachedValueAdvice(CachedValueProvider aCachedValueProvider) {
            this(aCachedValueProvider, null);
        }

        /**
         * Creates a new instance of {@code CachedValueAdvice}
         *
         * @param aCachedValueProvider a value provider that allows getting cached values or cache them if needed
         * @param aLogger              logger
         */
        public CachedValueAdvice(CachedValueProvider aCachedValueProvider, Logger aLogger) {
            cachedValueProvider = aCachedValueProvider;
            logger = aLogger;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object apply(Object performer, String operation, Object object, Object[] args) {
            return cachedValueProvider.getOrCreate(new OperationPerformKey(operation, object, args), () -> {
                if (logger != null)
                    logger.info(format("Not cached; getting value: {0} -> {1}({2})",
                            object,
                            operation, args != null ? Arrays.toString(args) : ""));
                return AroundAdvice.applyDefault(performer, operation, object, args);
            });
        }
    }

    /**
     * Advice (around) that allows turning all the Collection or Map results to their unmodifiable views
     */
    public static class ReadOnlyCollectionOrMapAdvice implements AroundAdvice {
        protected final Logger logger;

        /**
         * Creates a new {@code ReadOnlyCollectionOrMapAdvice} instance
         */
        public ReadOnlyCollectionOrMapAdvice() {
            logger = null;
        }

        /**
         * Creates a new {@code ReadOnlyCollectionOrMapAdvice} instance with a Logger
         *
         * @param aLogger logger
         */
        public ReadOnlyCollectionOrMapAdvice(Logger aLogger) {
            logger = aLogger;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object apply(Object performer, String operation, Object object, Object[] args) {
            Object result = AroundAdvice.applyDefault(performer, operation, object, args);

            Object unmodifiableValue = unmodifiableValue(result);
            if (unmodifiableValue != null)
                return unmodifiableValue;

            if (result != null && logger != null)
                logger.severe("Result is not a Collection or Map: " + result);

            return result;
        }

        protected Object unmodifiableValue(Object aValue) {
            return switch (aValue) {
                case List<?> list -> Collections.unmodifiableList(list);
                case Set<?> set -> Collections.unmodifiableSet(set);
                case Map<?, ?> map -> Collections.unmodifiableMap(map);
                case Collection<?> c -> Collections.unmodifiableCollection(c);
                case null, default -> null;
            };
        }
    }

    /**
     * Advice (around) that allows turning results to their unmodifiable views
     */
    public static class UnmodifiableValueAdvice extends ReadOnlyCollectionOrMapAdvice {
        Function<Object, Object> unmodifiableValueProvider;

        /**
         * Creates a new {@code ReadOnlyValueAdvice} instance with a read-only value provider
         * @param aUnmodifiableValueProvider read-only value provider
         */
        public UnmodifiableValueAdvice(Function<Object, Object> aUnmodifiableValueProvider) {
            this(aUnmodifiableValueProvider, null);
        }

        /**
         * Creates a new {@code ReadOnlyValueAdvice} instance with a read-only value provider and a logger
         *
         * @param aUnmodifiableValueProvider read-only value provider. If not specified - read-only views will be provided
         *                               for collections and maps only
         * @param aLogger logger
         */
        public UnmodifiableValueAdvice(Function<Object, Object> aUnmodifiableValueProvider, Logger aLogger) {
            super(aLogger);
            unmodifiableValueProvider = aUnmodifiableValueProvider;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object apply(Object performer, String operation, Object object, Object[] args) {
            Object result = AroundAdvice.applyDefault(performer, operation, object, args);

            Object unmodifiableValue = null;
            if (unmodifiableValueProvider != null) {
                unmodifiableValue = unmodifiableValueProvider.apply(result);
                if (unmodifiableValue != null)
                    return unmodifiableValue;
            }

            unmodifiableValue = unmodifiableValue(result);
            if (unmodifiableValue != null)
                return unmodifiableValue;

            if (result != null && logger != null)
                logger.severe("Can't obtain read-only value for: " + result);

            return result;
        }
    }
    /**
     * Advice (around) that allows catching all exceptions and return some value instead
     */
    public static class HandleThrowableAdvice<T> implements AroundAdvice {
        private final Supplier<T> supplier;
        private final Logger logger;

        /**
         * Creates a new {@code ReadOnlyCollectionOrMapAdvice} instance
         */
        public HandleThrowableAdvice(Supplier<T> aSupplier) {
            this(aSupplier, null);
        }

        /**
         * Creates a new {@code ReadOnlyCollectionOrMapAdvice} instance with a supplier and a Logger
         *
         * @param aSupplier supplier that provides a value to be returned when some exception occurs
         * @param aLogger   logger
         */
        public HandleThrowableAdvice(Supplier<T> aSupplier, Logger aLogger) {
            Objects.requireNonNull(aSupplier, "Supplier is not specified");
            supplier = aSupplier;
            logger = aLogger;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object apply(Object performer, String operation, Object object, Object[] args) {
            Object result = null;
            try {
                result = AroundAdvice.applyDefault(performer, operation, object, args);
            } catch (Throwable aThrowable) {
                result = supplier.get();
                if (logger != null)
                    logger.log(Level.SEVERE, "Exception occurred: " + aThrowable, aThrowable);
            }

            return result;
        }
    }

    /**
     * Advice (around) that adds a circuit breaker to an operation
     */
    public static class CircuitBreakerAdvice implements AroundAdvice {
        private final CircuitBreaker circuitBreaker;
        private final Logger logger;

        /**
         * Constructs an instance of CircuitBreakerAdvice with no logger.
         *
         * @param aFailureThreshold the number of allowed consecutive failures before the circuit breaker is opened
         * @param aTimeout          the duration for which the circuit breaker remains open after reaching the failure
         *                          threshold
         */
        public CircuitBreakerAdvice(int aFailureThreshold, Duration aTimeout) {
            this(aFailureThreshold, aTimeout, null);
        }

        /**
         * Constructs an instance of CircuitBreakerAdvice.
         *
         * @param aFailureThreshold the number of allowed consecutive failures before the circuit breaker is opened
         * @param aTimeout          the duration for which the circuit breaker remains open after reaching the failure
         *                          threshold
         * @param aLogger           the logger instance used to log exception information; can be null
         */
        public CircuitBreakerAdvice(int aFailureThreshold, Duration aTimeout, Logger aLogger) {
            circuitBreaker = new CircuitBreaker(aFailureThreshold, aTimeout);
            logger = aLogger;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object apply(Object performer, String operation, Object object, Object[] args) {
            try {
                return circuitBreaker.execute(() -> AroundAdvice.applyDefault(performer, operation, object, args));
            } catch (Exception ex) {
                if (logger != null)
                    logger.log(Level.SEVERE, "Exception occurred: " + ex, ex);
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Advice (around) that allows rate limiting for operations
     */
    public static class RateLimitedAdvice implements AroundAdvice {
        private final Logger logger;
        private final long maxTokens;
        private final long refillIntervalMillis;
        private final AtomicLong availableTokens = new AtomicLong(0);
        private volatile long lastRefillTimestamp;

        /**
         * Creates a RateLimitedAdvice. For example, {@code new RateLimitedAdvice(5, new Duration(1))} creates a
         * rate-limited advice that ensures an operation can be executed at most 5 times per second.
         *
         * @param maxTokens      the maximum number of tokens the bucket can hold (rate limit)
         * @param refillInterval the interval at which tokens are added to the bucket
         */
        public RateLimitedAdvice(long maxTokens, Duration refillInterval) {
            this(maxTokens, refillInterval, null);
        }

        /**
         * Creates a RateLimitedAdvice. For example, {@code new RateLimitedAdvice(5, new Duration(1), null)} creates a
         * rate-limited advice that ensures an operation can be executed at most 5 times per second.
         *
         * @param maxTokens      the maximum number of tokens the bucket can hold (rate limit)
         * @param refillInterval the interval at which tokens are added to the bucket
         * @param logger         optional logger
         */
        public RateLimitedAdvice(long maxTokens, Duration refillInterval, Logger logger) {
            if (maxTokens <= 0 || refillInterval.isNegative() || refillInterval.isZero()) {
                throw new IllegalArgumentException("Invalid rate-limiting parameters.");
            }
            this.maxTokens = maxTokens;
            this.refillIntervalMillis = refillInterval.toMillis();
            this.logger = logger;
            this.availableTokens.set(maxTokens);
            this.lastRefillTimestamp = System.currentTimeMillis();
        }

        private void refillTokens() {
            long now = System.currentTimeMillis();
            long elapsedTime = now - lastRefillTimestamp;

            if (elapsedTime >= refillIntervalMillis) {
                long tokensToAdd = Math.max(0, elapsedTime / refillIntervalMillis) * maxTokens;
                availableTokens.addAndGet(tokensToAdd);
                availableTokens.updateAndGet(current -> Math.min(current, maxTokens));
                lastRefillTimestamp = now;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object apply(Object performer, String operation, Object object, Object[] args)
                throws RateLimitedAdviceException {
            refillTokens();

            if (availableTokens.get() > 0) {
                availableTokens.decrementAndGet();
                if (logger != null)
                    logger.info(String.format("Rate limited advice invoked for operation: %s(%s)",
                            operation, DynamicClassExtension.parameterTypesAsString(args)));
                return AroundAdvice.applyDefault(performer, operation, object, args);
            } else {
                String message = String.format("Rate limit exceeded for operation: %s(%s) (%d per %d ms.)",
                        operation, DynamicClassExtension.parameterTypesAsString(args),
                        maxTokens, refillIntervalMillis);
                if (logger != null)
                    logger.warning(message);
                throw new RateLimitedAdviceException(message);
            }
        }
    }

    /**
     * Exception thrown in the context of rate-limited advice interception. This exception is typically used to signal
     * constraints or violations related to rate-limiting during method interception or advisory execution.
     */
    public static class RateLimitedAdviceException extends IllegalStateException {
        public RateLimitedAdviceException(String message) {
            super(message);
        }
    }

    /**
     * Represents a handler for managing quotas on operations. This interface defines methods
     * to retrieve available quota, decrease quota based on usage, and calculate costs associated
     * with operations and their results. Note: implementation of the interface must take care of proper synchronization
     * when getting and updating quota
     */
    public interface QuotaHandler {
        /**
         * Retrieves the available quota for a specified operation and context.
         *
         * @param anOperation the name of the operation for which the quota is being retrieved
         * @param anObject the object associated with the operation
         * @param anArgs arguments related to the operation
         * @return the remaining quota as a long value
         */
        long getQuota(String anOperation, Object anObject, Object[] anArgs);
        /**
         * Decreases the available quota for a specific operation.
         *
         * @param anAmount   the amount by which the quota should be decreased
         * @param anOperation  the name of the operation for which the quota is being reduced
         * @param anObject     the object related to the operation
         * @param anArgs       the arguments associated with the operation
         */
        void decreaseQuota(long anAmount, String anOperation, Object anObject, Object[] anArgs);
        /**
         * Calculates the cost associated with performing a specific operation.
         *
         * @param anOperation the name of the operation for which the cost is being calculated
         * @param anObject the object involved in the operation
         * @param anArgs the arguments related to the operation
         * @return the calculated cost of the operation as a long value
         */
        long calculateOperationCost(String anOperation, Object anObject, Object[] anArgs);
        /**
         * Calculates the cost associated with a given operation result based on the operation type,
         * result object, relevant context object, and operation arguments (({@code 0} if left default)).
         *
         * @param anOperation the name of the operation being performed
         * @param aResult   the result of the operation
         * @param anObject    the context object relevant to the operation
         * @param anArgs      the arguments with which the operation is executed
         * @return the calculated cost of the operation result
         */
        default long calculateOperationResultCost(String anOperation, Object aResult, Object anObject, Object[] anArgs) {
            return 0;
        }
    }

    /**
     * Represents a dynamic quota advice implementation that ensures quota constraints
     * are adhered to when performing operations. The class acts as around advice
     * that validates the available quota before allowing an operation to proceed.
     * Post-operation, it verifies the result cost against the remaining quota
     * and updates the quota appropriately. Runtime exceptions are thrown if the
     * operation or result exceeds the allocated quota.
     */
    public static class DynamicQuotaAdvice implements AroundAdvice {

        private final QuotaHandler quotaHandler;
        private final Logger logger;

        /**
         * Creates a new instance of DynamicQuotaAdvice.
         *
         * @param quotaHandler The QuotaHandler to manage operation and result costs.
         */
        public DynamicQuotaAdvice(QuotaHandler quotaHandler) {
            this(quotaHandler, null);
        }

        /**
         * Creates a new instance of DynamicQuotaAdvice.
         *
         * @param quotaHandler The QuotaHandler to manage operation and result costs.
         * @param logger       Optional Logger for logging purposes.
         */
        public DynamicQuotaAdvice(QuotaHandler quotaHandler, Logger logger) {
            this.quotaHandler = quotaHandler;
            this.logger = logger;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object apply(Object performer, String operation, Object object, Object[] args)
                throws DynamicQuotaAdviceException {
            long availableQuota = quotaHandler.getQuota(operation, object, args);

            // calculate and check operation quota
            long operationCost = quotaHandler.calculateOperationCost(operation, object, args);
            checkQuota(operation, operationCost, availableQuota);

            if (logger != null)
                logger.info(String.format("Quota check passed for operation %s(%s). Operation cost: %d, Available quota: %d",
                        operation, DynamicClassExtension.parameterTypesAsString(args), operationCost, availableQuota));

            // perform operation
            Object result = AroundAdvice.applyDefault(performer, operation, object, args);
            quotaHandler.decreaseQuota(operationCost, operation, object, args);

            // check result quota
            long resultCost = quotaHandler.calculateOperationResultCost(operation, result, object, args);

            if (availableQuota - operationCost- resultCost < 0) // rollback operation cost
                quotaHandler.decreaseQuota(-operationCost, operation, object, args);
            checkQuota(operation, resultCost, availableQuota - operationCost);
            quotaHandler.decreaseQuota(resultCost, operation, object, args);

            if (logger != null)
                logger.info(String.format("Operation %s(%s) completed. Result cost: %d, Remaining quota: %d",
                        operation, DynamicClassExtension.parameterTypesAsString(args),
                        resultCost, availableQuota - operationCost - resultCost));

            return result;
        }

        private void checkQuota(String operation, long cost, long availableQuota) {
            if (cost > availableQuota) {
                String message = String.format("Quota exceeded! Operation '%s' (cost: %d) cannot proceed. Available quota: %d",
                        operation, cost, availableQuota);
                if (logger != null)
                    logger.warning(message);
                throw new DynamicQuotaAdviceException(message);
            }
        }

        /**
         * Exception thrown to indicate specific advice related to dynamic quota management.
         * This exception is used to signal issues or errors within the context of
         * dynamic quota handling logic.
         */
        public static class DynamicQuotaAdviceException extends IllegalStateException {
            public DynamicQuotaAdviceException(String message) {
                super(message);
            }
        }
    }
}