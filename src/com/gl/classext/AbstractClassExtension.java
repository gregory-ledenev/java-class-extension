package com.gl.classext;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static com.gl.classext.Aspects.*;
import static java.text.MessageFormat.format;

public abstract class AbstractClassExtension implements ClassExtension {
    //region Cache methods
    private boolean cacheEnabled = true;
    @SuppressWarnings("rawtypes")
    protected final ThreadSafeWeakCache extensionCache = new ThreadSafeWeakCache();

    protected static String formatAdvice(Object anObject, Object anAdvice, AdviceType anAdviceType) {
        return format("{0} -> {1} for {2}", anAdviceType, anAdvice, anObject);
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

    protected void setupLogger() {
        if (logger == null) {
            logger = Logger.getLogger(getClass().getName());
            logger.setLevel(Level.ALL);
        }
    }
//endregion}

    protected final List<SinglePointcut> pointcuts = Collections.synchronizedList(new ArrayList<>());

    public boolean isAspectsEnabled() {
        return aspectsEnabled;
    }

    public void setAspectsEnabled(boolean aAspectsEnabled) {
        aspectsEnabled = aAspectsEnabled;
    }

    private boolean aspectsEnabled = true;

    protected void addPointcut(SinglePointcut aPointcut) {
        pointcuts.add(aPointcut);
    }

    @SuppressWarnings("UnusedReturnValue")
    protected boolean removePointcut(SinglePointcut aPointcut) {
        int size = pointcuts.size();
        //noinspection StatementWithEmptyBody
        while (pointcuts.remove(aPointcut)) {}

        if (size == pointcuts.size() && isVerbose())
            logger.info("No pointcut to remove: " + aPointcut.toString());
        return size > pointcuts.size();
    }

    protected void setPointcutEnabled(Pointcut aPointcut, boolean isEnabled) {
        boolean completed = false;
        for (SinglePointcut pointcut : new ArrayList<>(pointcuts)) {
            if (pointcut.equals(aPointcut)) {
                pointcut.setEnabled(isEnabled);
                completed = true;
            }
        }
        if (! completed && isVerbose())
            logger.info("No pointcut(s) to toggle enabled: " + aPointcut.toString());
    }

    protected Pointcut getPointcut(Class<?> anExtensionInterface, Class<?> anObjectClass,
                                         String anOperation, Class<?>[] anOperationParameterTypes,
                                         AdviceType anAdviceType) {
        Pointcuts result = new Pointcuts();
        for (SinglePointcut pointcut : new ArrayList<>(pointcuts)) {
            if (! pointcut.isEnabled() || ! pointcut.accept(anOperation, anOperationParameterTypes, anAdviceType))
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
        return ! result.isEmpty() ? result : null;
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
}
