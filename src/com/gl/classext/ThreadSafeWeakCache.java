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

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A thread-safe cache implementation that uses weak references to store values,
 * allowing them to be garbage collected when no longer referenced elsewhere.
 * The cache has a configurable maximum size and automatically cleans up stale entries.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class ThreadSafeWeakCache<K, V> implements Closeable {

    /**
     * The internal map storing key-value pairs with weak references
     */
    private final LinkedHashMap<K, WeakReference<V>> cache;
    /**
     * Queue for tracking garbage collected weak references
     */
    private final ReferenceQueue<V> queue;
    /**
     * Executor service for scheduling periodic cleanup tasks
     */
    private final ScheduledExecutorService cleanupExecutor;
    /**
     * Maximum number of entries the cache can hold
     */
    private int maxSize = 1000;

    /**
     * Creates a new ThreadSafeWeakCache with default maximum size of 1000 entries.
     */
    public ThreadSafeWeakCache() {
        this.cache = new LinkedHashMap<K, WeakReference<V>>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, WeakReference<V>> eldest) {
                return size() > maxSize;
            }
        };
        this.queue = new ReferenceQueue<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Creates a new ThreadSafeWeakCache with specified maximum size.
     *
     * @param maxSize maximum number of entries the cache can hold
     */
    public ThreadSafeWeakCache(int maxSize) {
        this();
        this.maxSize = maxSize;
    }

    @SuppressWarnings("unused")
    /**
     * Sets the maximum size of the cache.
     * @param aMaxSize new maximum size (must be >= 100)
     * @throws IllegalArgumentException if aMaxSize is less than 100
     */
    public void setMaxSize(int aMaxSize) {
        if (aMaxSize < 100)
            throw new IllegalArgumentException(MessageFormat.format("Invalid max cache size value: {0}. It must be >= 100", aMaxSize));

        maxSize = aMaxSize;
    }

    @SuppressWarnings("unused")
    /**
     * Returns the current maximum size of the cache.
     * @return maximum number of entries the cache can hold
     */
    public int getMaxSize() {
        return maxSize;
    }

    /**
     * Schedules periodic cleanup of stale cache entries.
     * Cleanup will run every minute.
     */
    public void scheduleCleanup() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }

    @SuppressWarnings("unchecked")
    /**
     * Performs immediate cleanup of stale cache entries.
     * Removes entries whose values have been garbage collected.
     */
    public void cleanup() {
        WeakReference<V> ref;
        synchronized (cache) {
            while ((ref = (WeakReference<V>) queue.poll()) != null) {
                final WeakReference<V> finalRef = ref;
                cache.entrySet().removeIf(entry -> entry.getValue() == finalRef);
            }
        }
    }

    /**
     * Associates the specified value with the specified key in this cache.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     */
    public void put(K key, V value) {
        synchronized (cache) {
            cache.put(key, new WeakReference<>(value, queue));
        }
    }

    /**
     * Returns the value associated with the specified key, or null if either
     * the key is not present or the value has been garbage collected.
     *
     * @param key the key whose associated value is to be returned
     * @return the value associated with the specified key, or null if not found
     */
    public V get(K key) {
        synchronized (cache) {
            WeakReference<V> ref = cache.get(key);
            return (ref != null) ? ref.get() : null;
        }
    }

    /**
     * Returns the value associated with the key if present, otherwise creates it
     * using the supplied function and associates it with the key.
     *
     * @param key       key with which the specified value is to be associated
     * @param aSupplier function to create new value if not present
     * @return the current (existing or computed) value associated with the key
     */
    public V getOrCreate(K key, Supplier<V> aSupplier) {
        V result = get(key);
        if (result == null) {
            synchronized (cache) {
                result = get(key);
                if (result == null) {
                    result = aSupplier.get();
                    put(key, result);
                }
            }
        }
        return result;
    }

    /**
     * Removes the entry for the specified key if present.
     *
     * @param key key whose mapping is to be removed from the cache
     */
    public void remove(K key) {
        synchronized (cache) {
            cache.remove(key);
        }
    }

    /**
     * Removes all entries from this cache.
     */
    public void clear() {
        synchronized (cache) {
            cache.clear();
        }
    }

    /**
     * Shuts down the cleanup executor service.
     */
    public void shutdownCleanup() {
        cleanupExecutor.shutdown();
    }

    /**
     * Returns true if this cache contains no entries.
     *
     * @return true if this cache contains no entries
     */
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    @Override
    public void close() throws IOException {
        shutdownCleanup();
    }

    /**
     * A record representing a key for class extension lookup.
     *
     * @param object             the object for which extension is requested
     * @param extensionInterface the interface of the requested extension
     */
    public record ClassExtensionKey(Object object, Class<?> extensionInterface) {
    }
}
