package com.gl.classext;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class CircuitBreaker {
    private final int failureThreshold;
    private final Duration timeout;
    private final AtomicInteger failureCount;
    private LocalDateTime lastFailureTime;
    private boolean open;

    public CircuitBreaker(int aFailureThreshold, Duration aTimeout) {
        this.failureThreshold = aFailureThreshold;
        this.timeout = aTimeout;
        this.failureCount = new AtomicInteger(0);
        this.lastFailureTime = LocalDateTime.now();
        this.open = false;
    }

    public <T> T execute(Supplier<T> operation) throws Exception {
        if (open && LocalDateTime.now().isBefore(lastFailureTime.plus(timeout))) {
            throw new CircuitBreakerException("Circuit is open. Retry after: " + lastFailureTime.plus(timeout));
        }

        try {
            T result = operation.get();
            reset();
            return result;
        } catch (Exception e) {
            registerFailure();
            throw new RuntimeException("Operation failed. CircuitBreaker engaged: " + e.getMessage(), e);
        }
    }

    private void reset() {
        open = false;
        failureCount.set(0);
    }

    private void registerFailure() {
        if (failureCount.incrementAndGet() >= failureThreshold) {
            open = true;
            lastFailureTime = LocalDateTime.now();
        }
    }

    public boolean isOpen() {
        return open;
    }

    public static class CircuitBreakerException extends IllegalStateException {
        public CircuitBreakerException(String s) {
            super(s);
        }
    }
}
