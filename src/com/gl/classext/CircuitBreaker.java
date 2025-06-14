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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Implementation of the Circuit Breaker pattern that helps to prevent repeated failures when interacting with a failing
 * component. It monitors failures and temporarily blocks operations when a threshold is exceeded.
 * <p>
 * The circuit breaker has three states: - Closed: Operations are allowed to proceed normally - Open: Operations are
 * blocked and fail fast - Half-Open: After a timeout period, allows a single operation to test if the failing component
 * has recovered
 */
public class CircuitBreaker {
    private final int failureThreshold;
    private final Duration timeout;
    private final AtomicInteger failureCount;
    private LocalDateTime lastFailureTime;
    private boolean open;

    /**
     * Creates a new CircuitBreaker instance.
     *
     * @param aFailureThreshold the number of consecutive failures that will trigger the circuit to open
     * @param aTimeout          the duration for which the circuit stays open before allowing another attempt
     */
    public CircuitBreaker(int aFailureThreshold, Duration aTimeout) {
        this.failureThreshold = aFailureThreshold;
        this.timeout = aTimeout;
        this.failureCount = new AtomicInteger(0);
        this.lastFailureTime = LocalDateTime.now();
        this.open = false;
    }

    /**
     * Executes the given operation with circuit breaker protection.
     *
     * @param operation the operation to execute
     * @param <T>       the type of the operation's result
     * @return the result of the operation
     * @throws CircuitBreakerException if the circuit is open
     * @throws RuntimeException        if the operation fails
     */
    public <T> T execute(Supplier<T> operation) throws CircuitBreakerException {
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

    /**
     * Resets the circuit breaker to its initial closed state. Clears the failure count and closes the circuit.
     */
    private void reset() {
        open = false;
        failureCount.set(0);
    }

    /**
     * Records a failure and opens the circuit if the failure threshold is reached. Updates the last failure time when
     * the circuit opens.
     */
    private void registerFailure() {
        if (failureCount.incrementAndGet() >= failureThreshold) {
            open = true;
            lastFailureTime = LocalDateTime.now();
        }
    }

    /**
     * Checks if the circuit breaker is in the open state.
     *
     * @return true if the circuit breaker is open, false otherwise
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * Exception thrown when an operation is attempted while the circuit is open.
     */
    public static class CircuitBreakerException extends IllegalStateException {
        public CircuitBreakerException(String s) {
            super(s);
        }
    }
}
