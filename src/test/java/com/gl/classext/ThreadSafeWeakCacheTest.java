package com.gl.classext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadSafeWeakCacheTest {

    @Test
    public void testLRUBehavior() throws InterruptedException {
        ThreadSafeWeakCache<String, String> cache = new ThreadSafeWeakCache<>(2); // Cache size of 2

        cache.put("1", "one");
        Thread.sleep(100);
        cache.put("2", "two");
        Thread.sleep(100);
        assertEquals("one", cache.get("1")); // Access 1 to make it most recently used
        Thread.sleep(100);
        cache.put("3", "three"); // This should evict "2" as it's least recently used

        assertNull(cache.get("2")); // "2" should be evicted
        assertEquals("one", cache.get("1")); // "1" should still be present
        assertEquals("three", cache.get("3")); // "3" should be present
    }

}
