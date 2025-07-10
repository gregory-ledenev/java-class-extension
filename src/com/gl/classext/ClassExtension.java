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

import java.util.Arrays;
import java.util.List;

/**
 * This interface provides core methods to allow getting class extensions, manage cache and logging. The main method is
 * {@code extension(Object, Class)} that allows to find and return an extension objects according to a supplied
 * interfaces.
 */
public interface ClassExtension {

    /**
     * Determines if the aspects functionality is enabled for this extension.
     *
     * @return {@code true} if aspects are enabled, {@code false} otherwise
     */
    boolean isAspectsEnabled();

    /**
     * Toggles the activation of the aspects functionality for this extension.
     *
     * @param aAspectsEnabled {@code true} to enable aspects, or {@code false} to disable aspects
     */
    void setAspectsEnabled(boolean aAspectsEnabled);

    /**
     * Enum representing the caching policy for the {@code ClassExtension} class. It defines
     * whether caching is enabled, disabled, or determined based on a default behavior.
     */
    enum CachePolicy {
        /**
         * Default caching policy, honoring {@code ClassExtension.cacheEnabled} property
         */
        DEFAULT,
        /**
         * Cache is disabled
         */
        DISABLED,
        /**
         * Cache is enabled
         */
        ENABLED,
    }

    /**
     * Enumeration representing the policy for enabling or disabling aspects in the {@code ClassExtension}.
     * It is used to control whether aspects functionality is enabled, disabled, or determined by the default setting.
     */
    enum AspectsPolicy {
        /**
         * Default aspects policy, honoring {@code ClassExtension.aspectsEnabled} property
         */
        DEFAULT,
        /**
         * Aspects are disabled
         */
        DISABLED,
        /**
         * Aspects are enabled
         */
        ENABLED,
    }
    /**
     * Defines a type of class extension
     */
    enum Type {
        /**
         * Unknown type; can be used to instruct using the most suitable type
         */
        UNKNOWN,
        /**
         * Dynamic extension
         */
        DYNAMIC,
        /**
         * Static extension using dynamic proxy access to an extension instance
         */
        STATIC_PROXY,
        /**
         * Static extension using direct access to an extension instance
         */
        STATIC_DIRECT
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
     * Finds and returns an extension object according to a supplied interfaces. NOTE: It is an optional interface so not all
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
     * Check if class extension is compatible with an extension type
     * @param aType type to check
     * @return {@code true} if class extension is compatible with an extension type; {@code false} otherwise
     */
    boolean compatible(Type aType);
    /**
     * Finds and returns an extension object according to a supplied interface. It creates either dynamic or static
     * extensions according to the {@code @ExtensionInterface.type} annotation for {@code anExtensionInterface} argument
     *
     * @param anObject             object to return an extension object for
     * @param anExtensionInterface interface of extension object to be returned
     * @return an extension object
     * @throws IllegalArgumentException if extension interface is not annotated with {@code @ExtensionInterface} annotation
     */
    static <T> T sharedExtension(Object anObject, Class<T> anExtensionInterface) {
        ExtensionInterface annotation = anExtensionInterface.getAnnotation(ExtensionInterface.class);
        if (annotation != null) {
            return sharedExtension(anObject, anExtensionInterface, annotation.type());
        } else {
            throw new IllegalArgumentException("Extension interface is not annotated with @ExtensionInterface");
        }
    }

    /**
     * Finds and returns an extension object according to a supplied type.
     *
     * @param anObject             object to return an extension object for
     * @param anExtensionInterface interface of extension object to be returned
     * @param type                 class extension type
     * @return an extension object
     */
    static <T> T sharedExtension(Object anObject, Class<T> anExtensionInterface, Type type) {
        switch (type) {
            case DYNAMIC:
                return DynamicClassExtension.sharedExtension(anObject, anExtensionInterface);
            case STATIC_PROXY:
            case STATIC_DIRECT:
                return StaticClassExtension.sharedExtension(anObject, anExtensionInterface);
            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }
    }

    /**
     * Finds and returns an extension object according to a supplied interfaces. Note: currently, only dynamic class extensions
     * support interfaces composition.
     *
     * @param anObject                 object to return an extension object for
     * @param anExtensionInterface     interface of extension object to be returned
     * @param aSupplementaryInterfaces supplementary interfaces of extension object to be returned
     * @return an extension object
     */
    static <T> T sharedExtension(Object anObject, Class<T> anExtensionInterface, Class<?>... aSupplementaryInterfaces) {
        ExtensionInterface annotation = anExtensionInterface.getAnnotation(ExtensionInterface.class);
        if (annotation != null) {
            switch (annotation.type()) {
                case DYNAMIC:
                    return DynamicClassExtension.sharedInstance().extension(anObject, anExtensionInterface, aSupplementaryInterfaces);
                case STATIC_PROXY:
                case STATIC_DIRECT:
                    return StaticClassExtension.sharedInstance().extension(anObject, anExtensionInterface, aSupplementaryInterfaces);
                default:
                    throw new IllegalStateException("Unexpected value: " + annotation.type());
            }
        } else {
            throw new IllegalArgumentException("Extension interface is not annotated with @ExtensionInterface");
        }
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
    @SuppressWarnings("unused")
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

    /**
     * Represents a composition that encapsulates a list of objects.
     */
    record Composition(List<?> objects) {
        /**
         * Constructs a Composition with the specified objects.
         *
         * @param objects the objects to be encapsulated in the composition
         */
        public Composition(Object... objects) {
            this(Arrays.asList(objects));
        }
    }
}
