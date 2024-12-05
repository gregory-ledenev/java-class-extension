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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.text.MessageFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ThreadSafeWeakCache<K, V> {

    private final ConcurrentHashMap<K, WeakReference<V>> cache;
    private final ReferenceQueue<V> queue;
    private final ScheduledExecutorService cleanupExecutor;
    private int maxSize = 1000;

    public ThreadSafeWeakCache() {
        this.cache = new ConcurrentHashMap<>();
        this.queue = new ReferenceQueue<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    public void setMaxSize(int aMaxSize) {
        if (aMaxSize < 100)
            throw new IllegalArgumentException(MessageFormat.format("Invalid max cache size value: {0}. It must be >= 100", aMaxSize));

        maxSize = aMaxSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void scheduleCleanup() {
        cleanupExecutor.scheduleAtFixedRate(this::cleanup, 1, 1, TimeUnit.MINUTES);
    }

    @SuppressWarnings("unchecked")
    public void cleanup() {
        WeakReference<V> ref;
        while ((ref = (WeakReference<V>) queue.poll()) != null) {
            final WeakReference<V> finalRef = ref;
            cache.entrySet().removeIf(entry -> entry.getValue() == finalRef);
        }
    }

    public void put(K key, V value) {
        // not ideal but sufficient so far
        while (cache.size() >= maxSize)
            cache.remove(cache.keySet().iterator().next());

        cache.put(key, new WeakReference<>(value, queue));
    }

    public V get(K key) {
        WeakReference<V> ref = cache.get(key);
        return (ref != null) ? ref.get() : null;
    }

    public V getOrCreate(K key, Supplier<V> aSupplier) {
        V result = get(key);
        if (result == null) {
            synchronized (this) {
                result = get(key);
                if (result == null) {
                    result = aSupplier.get();
                    put(key, result);
                }
            }
        }
        return result;
    }

    public void remove(K key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }

    public void shutdownCleanup() {
        cleanupExecutor.shutdown();
    }

    public boolean isEmpty() {
        return cache.isEmpty();
    }
}

record ClassExtensionKey(Object object, Class<?> extensionInterface) {}