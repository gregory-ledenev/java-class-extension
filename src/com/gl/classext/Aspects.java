package com.gl.classext;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
        AROUND
    }

    /**
     * Functional interface for {@code AdviceType.BEFORE} advices.
     */
    @FunctionalInterface
    public interface BeforeAdvice {
        /**
         * Applies this function to the given arguments
         *
         * @param operation an operation to be performed
         * @param object    object to perform the operation for
         * @param args      arguments for the operation
         */
        void accept(String operation, Object object, Object[] args);
    }

    /**
     * Functional interface for {@code AdviceType.AFTER} advices.
     */
    @FunctionalInterface
    public interface AfterAdvice {
        /**
         * Applies this function to the given arguments
         *
         * @param result    result of an operation
         * @param operation the operation to be performed
         * @param object    object to perform the operation for
         * @param args      arguments for the operation
         */
        void accept(Object result, String operation, Object object, Object[] args);
    }

    /**
     * Functional interface for {@code AdviceType.AROUND} advices.
     */
    @FunctionalInterface
    public interface AroundAdvice {
        /**
         * Applies this function to the given arguments. This function essentially replaces standard handling of the
         * operation. Use the {@code applyDefault} method to perform standard handling.
         *
         * @param performer performer object that is responsible for actual operation performing
         * @param operation the operation to be performed
         * @param object    object to perform the operation for
         * @param args      arguments for the operation
         */
        Object apply(Object performer, String operation, Object object, Object[] args);

        /**
         * Performs default handling of an operation.
         *
         * @param performer performer object that is responsible for actual operation performing
         * @param operation the operation to be performed
         * @param object    object to perform the operation for
         * @param args      arguments for the operation
         */
        @SuppressWarnings("rawtypes")
        static Object applyDefault(Object performer, String operation, Object object, Object[] args) {
            Objects.requireNonNull(performer);
            Objects.requireNonNull(object);

            if (performer instanceof AbstractClassExtension.Performer)
                return ((AbstractClassExtension.Performer) performer).perform(operation, object, args);
            else
                throw new IllegalStateException("Unsupported performer for around advice: " + performer);
        }
    }

    protected record Pointcut(Predicate<Class<?>> extensionInterface,
                           Predicate<Class<?>> objectClass,
                           BiPredicate<String, Class<?>[]> operation,
                           AdviceType adviceType, Object advice) {

        public Pointcut(Predicate<Class<?>> extensionInterface, Predicate<Class<?>> objectClass, BiPredicate<String, Class<?>[]> operation, AdviceType adviceType, Object advice) {
            this.extensionInterface = extensionInterface;
            this.objectClass = objectClass;
            this.operation = operation;
            this.adviceType = adviceType;
            switch (this.adviceType) {
                case BEFORE:
                    if (!(advice instanceof BeforeAdvice))
                        throw new IllegalArgumentException("Advice(BEFORE) is not a BeforeAdvice");
                    break;
                case AFTER:
                    if (!(advice instanceof AfterAdvice))
                        throw new IllegalArgumentException("Advice(AFTER) is not a AfterAdvice");
                    break;
                case AROUND:
                    if (!(advice instanceof AroundAdvice))
                        throw new IllegalArgumentException("Advice(AROUND) is not a AroundAdvice");
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + this.adviceType);
            }
            this.advice = advice;
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

        public void before(String anOperation, Object anObject, Object[] anArguments) {
            ((BeforeAdvice) advice).accept(anOperation, anObject, anArguments);
        }

        public void after(Object aResult, String anOperation, Object anObject, Object[] anArguments) {
            ((AfterAdvice) advice).accept(aResult, anOperation, anObject, anArguments);
        }

        public Object around(Object aPerformer, String operation, Object anObject, Object[] anArguments) {
            return ((AroundAdvice) advice).apply(aPerformer, operation, anObject, anArguments);
        }
    }

    /**
     * Builder that allows adding Aspects to the operations
     * @param <T>
     */
    public static class AspectBuilder<T extends AbstractClassExtension> {
        private final T classExtension;
        private Predicate<Class<?>> extensionInterface;
        private Predicate<Class<?>> objectClass;
        private BiPredicate<String, Class<?>[]> operation;

        /**
         * Creates a new instance of {@code AspectBuilder} and supplies a {@code ClassExtension} to build Aspects for
         * @param aClassExtension a {@code ClassExtension} to build Aspects for
         */
        public AspectBuilder(T aClassExtension) {
            classExtension = aClassExtension;
        }

        /**
         * Optional terminal operation that simply returns a {@code ClassExtension}
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

            extensionInterface = getClassPredicate(anInterfaceName);
            return this;
        }

        /**
         * Specifies an extension interface the Aspects should be added fpr
         * @param anExtensionInterface an extension interface
         * @return this Builder
         */
        public AspectBuilder<T> extensionInterface(Class<?> anExtensionInterface) {
            Objects.requireNonNull(anExtensionInterface);

            extensionInterface = getClassPredicate(anExtensionInterface);
            return this;
        }

        /**
         * Specifies extension interfaces the Aspects should be added fpr
         * @param anExtensionInterfaces extension interfaces
         * @return this Builder
         */
        public AspectBuilder<T> extensionInterface(Class<?>[] anExtensionInterfaces) {
            Objects.requireNonNull(anExtensionInterfaces);

            extensionInterface = getClassPredicate(anExtensionInterfaces);
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

            objectClass = getClassPredicate(anObjectClassName);
            return this;
        }

        /**
         * Specifies an object class the Aspects should be added fpr
         *
         * @param anObjectClass an object class
         * @return this Builder
         */
        public AspectBuilder<T> objectClass(Class<?> anObjectClass) {
            Objects.requireNonNull(anObjectClass);

            objectClass = getClassPredicate(anObjectClass);
            return this;
        }

        /**
         * Specifies an object classes the Aspects should be added fpr
         *
         * @param anObjectClasses an object classes
         * @return this Builder
         */
        public AspectBuilder<T> objectClass(Class<?>[] anObjectClasses) {
            Objects.requireNonNull(anObjectClasses);

            objectClass = getClassPredicate(anObjectClasses);
            return this;
        }

        /**
         * Specifies an operation the Aspects should be added fpr
         *
         * @param anOperation name of an operation. It is possible to use * and ? wildcards to define a * pattern all
         *                    operations should match.
         * @return this Builder
         */
        public AspectBuilder<T> operation(String anOperation) {
            Objects.requireNonNull(anOperation);

            operation = new BiPredicate<>() {
                @Override
                public boolean test(String operation, Class<?>[] parameterTypes) {
                    return AspectBuilder.this.operationNameMatches(operation, anOperation) &&
                            AspectBuilder.this.operationParameterTypesMatch(anOperation, parameterTypes);
                }

                @Override
                public String toString() {
                    return format("OperationPredicate(\"{0}\")", anOperation);
                }
            };
            return this;
        }

        private boolean operationParameterTypesMatch(String anOperation, Class<?>[] aParameterTypes) {
            String[] operationParameters = getOperationParameters(anOperation);
            boolean result = false;
            if (operationParameters.length == aParameterTypes.length) {
                result = operationParameters.length == 0;
                for (int i = 0; i < operationParameters.length && ! result; i++)
                    result = matches(operationParameters[i], aParameterTypes[i].getName()) ||
                            matches(operationParameters[i], aParameterTypes[i].getSimpleName());
            }

            return result;
        }

        private boolean operationNameMatches(String anOperation, String anOperationPattern) {
            return matches(getOperationName(anOperationPattern), getOperationName(anOperation));
        }

        private String getOperationName(String anOperation) {
            int index = anOperation.indexOf("(");
            return index > 0 ? anOperation.substring(0, index) : anOperation;
        }

        static final String[] EMPTY_PARAMETERS = new String[0];

        private String[] getOperationParameters(String anOperation) {
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
         * Adds a {@code AspectType.BEFORE} advice for a previously specified combination of extension interface(s), object
         * class(es) and operation(s)
         * @param aBefore advice to be added
         * @return this Builder
         */
        public AspectBuilder<T> before(BeforeAdvice aBefore) {
            Objects.requireNonNull(aBefore);
            checkPrerequisites();
            classExtension.addPointcut(new Pointcut(extensionInterface, objectClass, operation, AdviceType.BEFORE, aBefore));
            return this;
        }

        /**
         * Adds a {@code AspectType.AFTER} advice for a previously specified combination of extension interface(s), object
         * class(es) and operation(s)
         * @param anAfter advice to be added
         * @return this Builder
         */
        public AspectBuilder<T> after(AfterAdvice anAfter) {
            Objects.requireNonNull(anAfter);
            checkPrerequisites();
            classExtension.addPointcut(new Pointcut(extensionInterface, objectClass, operation, AdviceType.AFTER, anAfter));
            return this;
        }

        /**
         * Adds a {@code AspectType.AROUND} advice for a previously specified combination of extension interface(s), object
         * class(es) and operation(s)
         * @param anAround advice to be added
         * @return this Builder
         */
        public AspectBuilder<T> around(AroundAdvice anAround) {
            Objects.requireNonNull(anAround);
            checkPrerequisites();
            classExtension.addPointcut(new Pointcut(extensionInterface, objectClass, operation, AdviceType.AROUND, anAround));
            return this;
        }

        private void checkPrerequisites() {
            Objects.requireNonNull(objectClass, "Object class is not specified");
            Objects.requireNonNull(extensionInterface, "Extension interface is not specified");
            Objects.requireNonNull(operation, "Operation name is not specified");
        }

        private static Predicate<Class<?>> getClassPredicate(String aClassName) {
            return new Predicate<>() {
                @Override
                public boolean test(Class<?> aClass) {
                    return matches(
                            aClassName,
                            aClassName.contains(".") ? aClass.getName() : aClass.getSimpleName());
                }

                @Override
                public String toString() {
                    return format("ClassPredicate(\"{0}\")", aClassName);
                }
            };
        }

        private static Predicate<Class<?>> getClassPredicate(Class<?> anExtensionClass) {
            return new Predicate<>() {
                @Override
                public boolean test(Class<?> obj) {
                    return anExtensionClass.equals(obj);
                }

                @Override
                public String toString() {
                    return format("ClassPredicate(\"{0}\")", anExtensionClass);
                }
            };
        }

        private static Predicate<Class<?>> getClassPredicate(Class<?>[] anExtensionClasses) {
            return new Predicate<>() {
                @Override
                public boolean test(Class<?> aClass) {
                    return Arrays.asList(anExtensionClasses).contains(aClass);
                }

                @Override
                public String toString() {
                    return format("ClassPredicate(\"{0}\")", Arrays.toString(anExtensionClasses));
                }
            };
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

        /**
         * Creates a new instance of {@code LogPerformTimeAdvice}
         */
        public LogPerformTimeAdvice() {
            logger = Logger.getLogger(getClass().getName());
        }

        /**
         * Creates a new instance of {@code LogPerformTimeAdvice} and supplies a {@code Logger} to it
         * @param aLogger logger
         */
        public LogPerformTimeAdvice(Logger aLogger) {
            logger = aLogger;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object apply(Object performer, String operation, Object object, Object[] args) {
            long startTime = System.currentTimeMillis();

            Object result = AroundAdvice.applyDefault(performer, operation, object, args);
            logger.info(MessageFormat.format("Perform time for \"{0}\" is {1} ms.", operation, System.currentTimeMillis()-startTime));
            return result;
        }
    }

    /**
     * Advice (around) that allows tracking all property changes.
     */
    public static class PropertyChangeAdvice implements AroundAdvice {
        /**
         * Creates a new instance of {@code PropertyChangeAdvice} and supplies it with a {@code PropertyChangeListener}
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
                if (! Objects.equals(oldPropertyValue, newPropertyValue))
                    propertyChangeListener.propertyChange(new PropertyChangeEvent(this,
                            propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1),
                            oldPropertyValue,
                            newPropertyValue));
            }

            return result;
        }
    }

    /**
     * Utility methods that returns an extension with added perform time logging for all the operations. Note: returned
     * extensions will not be cached.
     * @param anObject             object to return an extension object for
     * @param anExtensionInterface interface of extension object to be returned
     * @return an extension object
     */
    public static <T> T logPerformTimeExtension(Object anObject, Class<T> anExtensionInterface) {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
                aspectBuilder().
                    objectClass("*").
                    extensionInterface("*").
                    operation("*").
                        around(new LogPerformTimeAdvice()).
                build();
        dynamicClassExtension.setCacheEnabled(false);
        return dynamicClassExtension.extension(anObject, anExtensionInterface);
    }

    /**
     * Utility methods that returns an extension with added logging of before and after all the operations. Note: returned
     * extensions will not be cached.
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
        dynamicClassExtension.setCacheEnabled(false);
        return dynamicClassExtension.extension(anObject, anExtensionInterface);
    }
}
