package com.gl.classext;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A thread-safe lazy value holder that initializes its value only when first accessed.
 * This class is useful for creating singletons or deferred initialization patterns.
 *
 * @param <T> the type of the value to be lazily initialized
 */
public class LazyValue<T> {
    private final AtomicReference<T> atomicReference = new AtomicReference<>();
    private final Supplier<T> initialValueSupplier;

    /**
     * Constructs a LazyValue with a supplier that provides the initial value.
     *
     * @param initialValueSupplier a supplier that provides the initial value when first accessed
     */
    public LazyValue(Supplier<T> initialValueSupplier) {
        this.initialValueSupplier = Objects.requireNonNull(initialValueSupplier);
    }

    /**
     * Retrieves the value, initializing it if it has not been set yet.
     * The initial value is provided by the supplied function.
     *
     * @return the lazily initialized value
     */
    public T get() {
        return atomicReference.updateAndGet(v -> v == null ? initialValueSupplier.get() : v);
    }

    /**
     * Resets the value to null and closes it if it implements AutoCloseable.
     * This is useful for cleanup or reinitialization.
     *
     * @return the old value before reset, or null if it was not set
     */
    public T reset() {
        T oldValue = atomicReference.getAndSet(null);
        if (oldValue instanceof AutoCloseable) {
            try {
                ((AutoCloseable) oldValue).close();
            } catch (Exception e) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error closing value in LazyValue", e);
            }
        }
        return oldValue;
    }
}
