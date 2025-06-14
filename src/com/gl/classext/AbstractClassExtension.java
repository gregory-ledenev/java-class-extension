package com.gl.classext;

import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static com.gl.classext.Aspects.*;
import static java.text.MessageFormat.format;

/**
 * Abstract base class that provides a skeletal implementation of the {@link ClassExtension} interface.
 * This class includes utilities and functionality for managing caching, verbose mode logging,
 * pointcut handling, and aspects support.
 *
 * Subclasses are expected to override or extend the provided methods to implement specific
 * behaviors or extensions as required.
 */
public abstract class AbstractClassExtension implements ClassExtension {
    //region Cache methods
    private boolean cacheEnabled = true;
    @SuppressWarnings("rawtypes")
    protected final ThreadSafeWeakCache extensionCache = new ThreadSafeWeakCache();

    protected static String formatAdvice(Object anObject, Object anAdvice, AdviceType anAdviceType) {
        return format("{0} -> {1} for {2}", anAdviceType, anAdvice, anObject);
    }

    protected static Object classExtensionForOperationResult(ClassExtension aClassExtension, Method aMethod, Object aResult) {
        Object result = aResult;

        if (aResult != null) {
            ObtainExtension annotation = aMethod.getAnnotation(ObtainExtension.class);
            if (annotation != null) {
                Type type = annotation.type();
                Class<?> extensionInterface = aMethod.getReturnType();
                ExtensionInterface extensionInterfaceAnnotation = extensionInterface.getAnnotation(ExtensionInterface.class);
                if (extensionInterfaceAnnotation!= null)
                    type = extensionInterfaceAnnotation .type();
                if (type == Type.UNKNOWN || aClassExtension.compatible(type))
                    result = aClassExtension.extension(result, extensionInterface);
                else
                    result = ClassExtension.sharedExtension(result, extensionInterface, type);
            }
        }

        return result;
    }

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

    
    /**
     * Configures the logger for this class if it's not already set. The logger is configured to log all levels of
     * messages.
     */
    protected void setupLogger() {
        if (logger == null) {
            logger = Logger.getLogger(getClass().getName());
            logger.setLevel(Level.ALL);
        }
    }
//endregion}

    protected final List<SinglePointcut> pointcuts = Collections.synchronizedList(new ArrayList<>());

    /**
     * Determines if the aspects functionality is enabled for this extension.
     *
     * @return {@code true} if aspects are enabled, {@code false} otherwise
     */
    public boolean isAspectsEnabled() {
        return aspectsEnabled;
    }

    /**
     * Toggles the activation of the aspects functionality for this extension.
     *
     * @param aAspectsEnabled {@code true} to enable aspects, or {@code false} to disable aspects
     */
    public void setAspectsEnabled(boolean aAspectsEnabled) {
        aspectsEnabled = aAspectsEnabled;
    }

    private boolean aspectsEnabled = true;

    /**
     * Adds a single pointcut to the list of managed pointcuts.
     *
     * @param aPointcut the pointcut to be added
     */
    protected void addPointcut(SinglePointcut aPointcut) {
        synchronized (pointcuts) {
            pointcuts.add(aPointcut);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    /**
     * Removes a specific pointcut from the list of managed pointcuts.
     *
     * @param aPointcut the pointcut to be removed
     * @return {@code true} if the pointcut was found and removed, {@code false} otherwise
     */
    protected boolean removePointcut(SinglePointcut aPointcut) {
        int removedCount = 0;
        synchronized (pointcuts) {
            Iterator<SinglePointcut> iterator = pointcuts.iterator();
            while (iterator.hasNext()) {
                if (iterator.next().equals(aPointcut)) {
                    iterator.remove();
                    removedCount++;
                }
            }
        }
        if (removedCount == 0 && isVerbose()) {
            logger.info("No pointcut to remove: " + aPointcut);
        }
        return removedCount > 0;
    }

    /**
     * Enables or disables a specific pointcut.
     *
     * @param aPointcut the pointcut to be toggled
     * @param isEnabled the new enabled state of the pointcut
     */
    protected void setPointcutEnabled(Pointcut aPointcut, boolean isEnabled) {
        boolean completed = false;
        for (SinglePointcut pointcut : new ArrayList<>(pointcuts)) {
            if (pointcut.equals(aPointcut)) {
                pointcut.setEnabled(isEnabled);
                completed = true;
            }
        }
        if (!completed && isVerbose())
            logger.info("No pointcut(s) to toggle enabled: " + aPointcut.toString());
    }

    /**
     * Retrieves pointcuts that match the specified criteria, including extension interfaces, object classes,
     * operations, and advice types.
     *
     * @param anExtensionInterface      the extension interface to match
     * @param anObjectClass             the class of the object to match
     * @param anOperation               the name of the operation to match
     * @param anOperationParameterTypes the parameter types of the operation to match
     * @param anAdviceType              the type of advice to match
     * @return the matching pointcut(s) or {@code null} if none found
     */
    protected Pointcut getPointcut(Class<?> anExtensionInterface, Class<?> anObjectClass,
                                   String anOperation, Class<?>[] anOperationParameterTypes,
                                   AdviceType anAdviceType) {
        if (!pointcuts.isEmpty()) {
            Pointcuts result = new Pointcuts();
            for (SinglePointcut pointcut : new ArrayList<>(pointcuts)) {
                if (!pointcut.isEnabled() || !pointcut.accept(anOperation, anOperationParameterTypes, anAdviceType))
                    continue;

                if (pointcut.accept(anExtensionInterface, anObjectClass, anOperation, anOperationParameterTypes, anAdviceType)) {
                    result.addPointcut(pointcut);
                } else {
                    List<Class<?>> objectClasses = new ArrayList<>(collectSuperClasses(anObjectClass));
                    objectClasses.addAll(Arrays.asList(anObjectClass.getInterfaces()));

                    for (Class<?> extensionInterface : collectSuperInterfaces(anExtensionInterface)) {
                        for (Class<?> objectClass : objectClasses) {
                            if (pointcut.accept(extensionInterface, objectClass)) {
                                result.addPointcut(pointcut);
                            }
                        }
                    }
                }
            }

            result.filterOutDuplicates();
            return !result.isEmpty() ? result : null;
        } else {
            return null;
        }
    }

    /**
     * Collects all interfaces implemented by the given class, including the class itself.
     *
     * @param anObjectClass the class whose interfaces are to be collected
     * @return a list of interfaces and the class itself
     */
    protected static List<Class<?>> collectSuperInterfaces(Class<?> anObjectClass) {
        List<Class<?>> result = new ArrayList<>();
        result.add(anObjectClass);
        result.addAll(List.of(anObjectClass.getInterfaces()));
        return result;
    }

    /**
     * Collects all superclasses of the given class in a linear hierarchy.
     *
     * @param anObjectClass the class whose superclasses are to be collected
     * @return a list of all superclasses of the given class
     */
    protected static List<Class<?>> collectSuperClasses(Class<?> anObjectClass) {
        return Stream.iterate(anObjectClass.getSuperclass(), Objects::nonNull, (Class<?> aClass) -> aClass.getSuperclass()).
                toList();
    }

    /**
     * Determines whether caching is enabled for the given extension interface.
     * If the extension interface is annotated with {@code ExtensionInterface}, its specified
     * caching policy will be considered. Otherwise, the default caching policy applies.
     *
     * @param <T> the type of the extension interface
     * @param anExtensionInterface the extension interface to check for caching policy
     * @return {@code true} if caching is enabled, {@code false} otherwise
     */
    public <T> boolean isCacheEnabled(Class<T> anExtensionInterface) {
        boolean result = isCacheEnabled();
        if (anExtensionInterface.isAnnotationPresent(ExtensionInterface.class)) {
            CachePolicy cachePolicy = anExtensionInterface.getAnnotation(ExtensionInterface.class).cachePolicy();
            if (cachePolicy != CachePolicy.DEFAULT)
                result = cachePolicy == CachePolicy.ENABLED;
        }
        return result;
    }
}
