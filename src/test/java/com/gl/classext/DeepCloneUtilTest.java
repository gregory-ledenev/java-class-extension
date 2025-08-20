package com.gl.classext;

import org.junit.jupiter.api.Test;

import java.util.*;

import static com.gl.classext.DeepCloneUtil.deepClone;
import static org.junit.jupiter.api.Assertions.*;

public class DeepCloneUtilTest {
    @Test
    void testBasicListClone() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        var copiedList = deepClone(list);
        assertEquals(list, copiedList);
    }

    @Test
    void testSimpleListCloneWithModification() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        var copiedList = deepClone(list);
        assertThrows(UnsupportedOperationException.class, () -> copiedList.add("d"));
    }

    record TestObject(String name, int value) implements Cloneable {
        public Object clone() {
            try {
                super.clone();
                return new TestObject(name, value);
            } catch (CloneNotSupportedException aE) {
                throw new RuntimeException(aE);
            }
        }
    }

    @Test
    void testComplexMapClone() {
        Map<String, Object> complexMap = new HashMap<>();
        complexMap.put("numbers", Arrays.asList(1, 2, 3, new TestObject("test", 42)));
        complexMap.put("strings", Arrays.asList("one", "two", "three", new ArrayList<Object>(Arrays.asList(100, 200, 300))));
        var copiedMap = deepClone(complexMap);
        assertEquals(complexMap, copiedMap);
    }

    @Test
    void testCircularReferencesClone() {
        Map<String, Object> complexMap = new HashMap<>();
        complexMap.put("numbers", Arrays.asList(1, 2, 3, new TestObject("test", 42)));
        ArrayList<Object> circularRefList = new ArrayList<>(Arrays.asList(100, 200, 300));
        circularRefList.add(circularRefList);
        complexMap.put("strings", Arrays.asList("one", "two", "three", circularRefList));
        assertThrows(IllegalArgumentException.class, () -> deepClone(complexMap));
    }

    record User(String name) {}

    @Test
    void testNotCloneable() {
        List<User> users = Arrays.asList(new User("Alice"), new User("Bob"));
        assertThrows(UnsupportedOperationException.class, () -> deepClone(users));
    }

    @Test
    void testCloner() {
        System.out.println(new Integer[] {1, 2, 3}.equals(new Integer[] {1, 2, 3}));
        List<?> users = Arrays.asList(
                Map.of("user", new User("Kate")),
                Arrays.asList(new User("Alice"), new User("Bob"))
        );
        List<?> clone = deepClone(users, o -> o instanceof User user ? new User((user).name()) : null);
        assertNotSame(users, clone);
        assertEquals(users, clone);
    }

    record UserCloneable(String name) implements Cloneable {
        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }

    @Test
    void testCloneable() {
        List<UserCloneable> users = Arrays.asList(new UserCloneable("Alice"), new UserCloneable("Bob"));
        List<UserCloneable> clone = deepClone(users);
        assertNotSame(users, clone);
        assertEquals(users.toString(), clone.toString());
    }

    record UserWithCopyConstructor(String name) {
        public UserWithCopyConstructor(UserWithCopyConstructor other) {
            this(other.name);
        }
    }

    @Test
    void testCopyConstructor() {
        List<UserWithCopyConstructor> users = Arrays.asList(new UserWithCopyConstructor("Alice"), new UserWithCopyConstructor("Bob"));
        List<UserWithCopyConstructor> clone = deepClone(users);
        assertNotSame(users, clone);
        assertEquals(users.toString(), clone.toString());
    }
}
