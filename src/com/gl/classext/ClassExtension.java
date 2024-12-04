package com.gl.classext;

public interface ClassExtension {

    interface PrivateDelegate {
        Object __getDelegate();
    }

    /**
     * Finds and returns an extension object according to a supplied interface. It uses cache to avoid redundant objects
     * creation. If no cache should be used - turn it OFF via the {@code setCacheEnabled(false)} call
     *
     * @param anObject             object to return an extension object for
     * @param anExtensionInterface interface of extension object to be returned
     * @return an extension object
     */
    <T> T extension(Object anObject, Class<T> anExtensionInterface);

    static boolean equals(Object anObject, Object anExtension) {
        if (anObject == anExtension)
            return true;
        else if (anObject != null && anExtension != null)
            return (anObject.equals(anExtension) || ((anExtension instanceof PrivateDelegate delegate) && anObject.equals(delegate.__getDelegate()))) ||
                    (anExtension.equals(anObject) || ((anObject instanceof PrivateDelegate delegate) && delegate.__getDelegate().equals(anObject)));

        return false;
    }

    static Object getDelegate(Object anExtension) {
        return anExtension instanceof PrivateDelegate privateDelegate ? privateDelegate.__getDelegate() : null;
    }

    /**
     * Checks if cache is enabled
     * @return {@code true} if cache is enabled; {@code false} otherwise
     */
    @SuppressWarnings("unused")
    boolean isCacheEnabled();

    /**
     * Specifies that cache should be enabled
     * @param isCacheEnabled {@code true} if cache should be enabled; {@code false} otherwise
     */
    void setCacheEnabled(boolean isCacheEnabled);

    /**
     * Cleanups cache by removing keys for all already garbage collected values
     */
    void cacheCleanup();

    /**
     * Clears cache
     */
    void cacheClear();

    /**
     * Schedules automatic cache cleanup that should be performed once a minute
     */
    void scheduleCacheCleanup();

    /**
     * Shutdowns automatic cache cleanup
     */
    void shutdownCacheCleanup();

    /**
     * Check if cache is empty
     * @return true if cache is empty; false otherwise
     */
    boolean cacheIsEmpty();
}
