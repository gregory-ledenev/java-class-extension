package com.gl.classext;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public abstract class AbstractClassExtension implements ClassExtension {
    //region Cache methods
    private boolean cacheEnabled = true;
    @SuppressWarnings("rawtypes")
    protected final ThreadSafeWeakCache extensionCache = new ThreadSafeWeakCache();

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
            if (!isCacheEnabled)
                cacheClear();
        }
    }

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
//endregion}

    protected List<Pointcut> pointcuts = Collections.synchronizedList(new ArrayList<>());

    protected void addPointcut(Pointcut aPointcut) {
        pointcuts.add(aPointcut);
    }

    protected Pointcut getPointcut(Class<?> anExtensionInterface, Class<?> anObjectClass,
                                         String anOperation, Class<?>[] anOperationParameterTypes,
                                         AdviceType anAdviceType) {
        Pointcut result = null;
        for (Pointcut pointcut : new ArrayList<>(pointcuts)) {
            if (! pointcut.accept(anOperation, anOperationParameterTypes, anAdviceType))
                continue;

            if (pointcut.accept(anExtensionInterface, anObjectClass, anOperation, anOperationParameterTypes, anAdviceType)) {
                result = pointcut;
                break;
            } else {
                List<Class<?>> objectClasses = new ArrayList<>(collectSuperClasses(anObjectClass));
                objectClasses.addAll(Arrays.asList(anObjectClass.getInterfaces()));

                all: for (Class<?> extensionInterface : collectSuperInterfaces(anExtensionInterface)) {
                    for (Class<?> objectClass : objectClasses) {
                        if (pointcut.accept(extensionInterface, objectClass)) {
                            result = pointcut;
                            break all;
                        }
                    }
                }
            }
        }
        return result;
    }

    protected static List<Class<?>> collectSuperInterfaces(Class<?> anObjectClass) {
        List<Class<?>> result = new ArrayList<>();
        result.add(anObjectClass);
        result.addAll(List.of(anObjectClass.getInterfaces()));
        return result;
    }

    protected static List<Class<?>> collectSuperClasses(Class<?> anObjectClass) {
        return Stream.iterate(anObjectClass.getSuperclass(), Objects::nonNull, (Class<?> aClass) -> aClass.getSuperclass()).
                toList();
    }

    public AspectBuilder aspectBuilder() {
        return new AspectBuilder(this);
    }

    public enum AdviceType {
        BEFORE, AFTER
    }

    public record Pointcut(Predicate<Class<?>> extensionInterface,
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
                    if (! (advice instanceof BiConsumer))
                        throw new IllegalArgumentException("Advice(BEFORE) is not a BiConsumer");
                break;
                case AFTER:
                    if (! (advice instanceof Consumer))
                        throw new IllegalArgumentException("Advice(AFTER) is not a Consumer");
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

        @SuppressWarnings("unchecked")
        public void before(Object anObject, Object[] anArguments) {
            ((BiConsumer<? super Object, Object[]>) advice).accept(anObject, anArguments);
        }

        @SuppressWarnings("unchecked")
        public void after(Object aResult) {
            ((Consumer<? super Object>) advice).accept(aResult);
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

            operation = (operation, parameterTypes) ->
                    operationNameMatches(operation, anOperation) &&
                    operationParameterTypesMatch(anOperation, parameterTypes);
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
            return matches(getOperationName(anOperation), getOperationName(anOperationPattern));
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
            }
            return result.length == 1 && "".equals(result[0]) ? EMPTY_PARAMETERS : result;
        }

        public AspectBuilder before(BiConsumer<? super Object, Object[]> aBefore) {
            Objects.requireNonNull(aBefore);
            checkPrerequisites();
            classExtension.addPointcut(new Pointcut(extensionInterface, objectClass, operation, AdviceType.BEFORE, aBefore));
            return this;
        }

        public AspectBuilder after(Consumer<? super Object> anAfter) {
            Objects.requireNonNull(anAfter);
            checkPrerequisites();
            classExtension.addPointcut(new Pointcut(extensionInterface, objectClass, operation, AdviceType.AFTER, anAfter));
            return this;
        }

        private void checkPrerequisites() {
            Objects.requireNonNull(objectClass, "Object class is not specified");
            Objects.requireNonNull(extensionInterface, "Extension interface is not specified");
            Objects.requireNonNull(operation, "Operation name is not specified");
        }

        private static Predicate<Class<?>> getClassPredicate(String aClassName) {
            return aClass -> matches(
                    aClassName,
                    aClassName.contains(".") ? aClass.getName() : aClass.getSimpleName());
        }

        private static boolean matches(String aPattern, String aString) {
            return Pattern.matches(
                    aPattern.replace("*", ".*").replace("?", "."),
                    aString);
        }

        private static Predicate<Class<?>> getClassPredicate(Class<?> anExtensionClass) {
            return anExtensionClass::equals;
        }

        private static Predicate<Class<?>> getClassPredicate(Class<?>[] anExtensionClasses) {
            return aClass -> Arrays.asList(anExtensionClasses).contains(aClass);
        }
    }
}
