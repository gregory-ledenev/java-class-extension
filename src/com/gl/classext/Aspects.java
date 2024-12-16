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

public class Aspects {
    public enum AdviceType {
        BEFORE,
        AFTER,
        AROUND
    }

    @FunctionalInterface
    public interface BeforeAdvice {
        void accept(String operation, Object object, Object[] args);
    }

    @FunctionalInterface
    public interface AfterAdvice {
        void accept(Object result, String operation, Object object, Object[] args);
    }

    @FunctionalInterface
    public interface AroundAdvice {
        Object apply(Object performer, String operation, Object object, Object[] args);

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

    public static class AspectBuilder {
        private final AbstractClassExtension classExtension;
        private Predicate<Class<?>> extensionInterface;
        private Predicate<Class<?>> objectClass;
        private BiPredicate<String, Class<?>[]> operation;

        public AspectBuilder(AbstractClassExtension aClassExtension) {
            classExtension = aClassExtension;
        }

        public AspectBuilder extensionInterface(String anInterfaceName) {
            Objects.requireNonNull(anInterfaceName);

            extensionInterface = getClassPredicate(anInterfaceName);
            return this;
        }

        public AspectBuilder extensionInterface(Class<?> anExtensionInterface) {
            Objects.requireNonNull(anExtensionInterface);

            extensionInterface = getClassPredicate(anExtensionInterface);
            return this;
        }

        public AspectBuilder extensionInterface(Class<?>[] anExtensionInterface) {
            Objects.requireNonNull(anExtensionInterface);

            extensionInterface = getClassPredicate(anExtensionInterface);
            return this;
        }

        public AspectBuilder objectClass(String aClassName) {
            Objects.requireNonNull(aClassName);

            objectClass = getClassPredicate(aClassName);
            return this;
        }

        public AspectBuilder objectClass(Class<?> anObjectClass) {
            Objects.requireNonNull(anObjectClass);

            objectClass = getClassPredicate(anObjectClass);
            return this;
        }

        public AspectBuilder objectClass(Class<?>[] anObjectClasses) {
            Objects.requireNonNull(anObjectClasses);

            objectClass = getClassPredicate(anObjectClasses);
            return this;
        }

        public AspectBuilder operation(String anOperation) {
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

        public AspectBuilder before(BeforeAdvice aBefore) {
            Objects.requireNonNull(aBefore);
            checkPrerequisites();
            classExtension.addPointcut(new Pointcut(extensionInterface, objectClass, operation, AdviceType.BEFORE, aBefore));
            return this;
        }

        public AspectBuilder after(AfterAdvice anAfter) {
            Objects.requireNonNull(anAfter);
            checkPrerequisites();
            classExtension.addPointcut(new Pointcut(extensionInterface, objectClass, operation, AdviceType.AFTER, anAfter));
            return this;
        }

        public AspectBuilder around(AroundAdvice anAround) {
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

    public static class LogBeforeAdvice implements BeforeAdvice {
        final Logger logger;

        public LogBeforeAdvice() {
            logger = Logger.getLogger(getClass().getName());
        }

        public LogBeforeAdvice(Logger aLogger) {
            logger = aLogger;
        }

        @Override
        public void accept(String operation, Object object, Object[] args) {
            logger.info(format("BEFORE: {0} -> {1}({2})",
                    object,
                    operation, args != null ? Arrays.toString(args) : ""));
        }
    }

    public static class LogAfterAdvice implements AfterAdvice {
        final Logger logger;

        public LogAfterAdvice() {
            logger = Logger.getLogger(getClass().getName());
        }

        public LogAfterAdvice(Logger aLogger) {
            logger = aLogger;
        }

        @Override
        public void accept(Object result, String operation, Object object, Object[] args) {
            logger.info(format("AFTER: {0} -> {1}({2}) = {3}",
                    object,
                    operation,
                    args != null ? Arrays.toString(args) : "",
                    result));
        }
    }

    public static class LogPerformTimeAdvice implements AroundAdvice {
        final Logger logger;

        public LogPerformTimeAdvice() {
            logger = Logger.getLogger(getClass().getName());
        }

        public LogPerformTimeAdvice(Logger aLogger) {
            logger = aLogger;
        }

        @Override
        public Object apply(Object performer, String operation, Object object, Object[] args) {
            long startTime = System.currentTimeMillis();

            Object result = AroundAdvice.applyDefault(performer, operation, object, args);
            logger.info(MessageFormat.format("Perform time for \"{0}\" is {1} ms.", operation, System.currentTimeMillis()-startTime));
            return result;
        }
    }

    public static class PropertyChangeAdvice implements AroundAdvice {
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

    public static <T> T logPerformTimeExtension(Object anObject, Class<T> anExtensionInterface) {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension();
        dynamicClassExtension.aspectBuilder().
                objectClass("*").
                extensionInterface("*").
                operation("*").
                    around(new LogPerformTimeAdvice());
        return dynamicClassExtension.extension(anObject, anExtensionInterface);
    }
}
