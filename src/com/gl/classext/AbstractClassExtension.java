package com.gl.classext;

import java.util.logging.Level;
import java.util.logging.Logger;

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
}
//endregion}
