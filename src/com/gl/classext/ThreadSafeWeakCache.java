package com.gl.classext;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ThreadSafeWeakCache<K, V> {

    private final ConcurrentHashMap<K, WeakReference<V>> cache;
    private final ReferenceQueue<V> queue;
    private final ScheduledExecutorService cleanupExecutor;

    public ThreadSafeWeakCache() {
        this.cache = new ConcurrentHashMap<>();
        this.queue = new ReferenceQueue<>();
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
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
}