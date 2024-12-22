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

/**
 * This interface provides core methods to allow getting class extensions, manage cache and logging. The main method is
 * {@code extension(Object, Class)} that allows to find and return an extension objects according to a supplied
 * interfaces.
 */
public interface ClassExtension {

    /**
     * Defines instantiation strategy choices. Currently:
     * <ul>
     *    <li>{@link DynamicClassExtension} always uses PROXY.</li>
     *    <li>{@link StaticClassExtension} supports both PROXY and DIRECT, configurable by use of {@link ExtensionInterface} annotation.</li>
     * </ul>
     */
    enum InstantiationStrategy {
        /**
         * Specifies that dynamic proxies should be instantiated with extension instances inside
         */
        PROXY,
        /**
         * Specifies that extension instances should be instantiated directly
         */
        DIRECT
    }

    /**
     * The interface all the class extensions must implement. It defines the 'delegate' property which is used to supply
     * extension objects with values to work with
     *
     * @param <T> defines a class of delegate objects to work with
     */
    interface DelegateHolder<T> {
        /**
         * Returns a delegate object an extension works with
         * @return a delegate object
         */
        T getDelegate();

        /**
         * Specifies a delegate object an extension should work with
         * @param aDelegate a delegate object
         */
        void setDelegate(T aDelegate);
    }

    /**
     * An interface used internally by proxy implementations to allow querying for a delegate object an
     * extensions works with
     */
    interface PrivateDelegateHolder {
        /**
         * Returns a delegate object an extension works with
         * @return a delegate object
         */
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

    /**
     * Finds and returns an extension object according to a supplied interface. NOTE: It is an optional interface so not all
     * implementation may support it. It uses cache to avoid redundant objects creation. If no cache should be used -
     * turn it OFF via the {@code setCacheEnabled(false)} call
     *
     * @param anObject                 object to return an extension object for
     * @param anExtensionInterface     interface of extension object to be returned
     * @param aSupplementaryInterfaces supplementary interfaces of extension object to be returned
     * @return an extension object
     */
    default <T> T extension(Object anObject, Class<T> anExtensionInterface, Class<?>... aSupplementaryInterfaces) {
        throw new UnsupportedOperationException("Extensions with supplementary interfaces are not supported");
    }

    /**
     * Checks that an extension represents particular object. So for example,
     * {@code equals(book, StaticClassExtension.sharedExtension(book, Shippable.class))} is {@code true}
     *
     * @param anObject    object
     * @param anExtension extension
     * @return {@code true} if the extension represents particular object; {@code false} otherwise.
     */
    static boolean equals(Object anObject, Object anExtension) {
        if (anObject == anExtension)
            return true;
        else if (anObject != null && anExtension != null) {
            if (anObject.equals(anExtension) || anExtension.equals(anObject))
                return true;
            if ((anExtension instanceof PrivateDelegateHolder delegate) && anObject.equals(delegate.__getDelegate()))
                return true;
            if ((anExtension instanceof DelegateHolder<?> delegate) && anObject.equals(delegate.getDelegate()))
                return true;
            if ((anObject instanceof PrivateDelegateHolder delegate) && delegate.__getDelegate().equals(anObject))
                return true;
            return (anObject instanceof DelegateHolder<?> delegate) && delegate.getDelegate().equals(anObject);
        }

        return false;
    }

    /**
     * Returns a delegate object an extension was obtained for, So for example, {@code getDelegate(StaticClassExtension.sharedExtension(book, Shippable.class)) == book} is {@code true}
     *
     * @param anExtension extension
     * @return delegate object
     */
    static Object getDelegate(Object anExtension) {
        return anExtension instanceof PrivateDelegateHolder privateDelegate ? privateDelegate.__getDelegate() : null;
    }

    /**
     * Checks if verbose mode it turned ON
     * @return {@code true} if verbose mode it turned ON; {@code false} otherwise.
     */
    boolean isVerbose();

    /**
     * Turns verbose mode ON/OFF
     * @param isVerbose {@code true} if verbose mode should be turned ON; {@code false} otherwise.
     */
    void setVerbose(boolean isVerbose);

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

    /**
     * An interface for objects holding an identity
     */
    interface IdentityHolder {
        /**
         * Returns an identity
         * @return identity
         */
        Object getID();
    }
}
