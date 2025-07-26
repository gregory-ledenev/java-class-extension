package com.gl.classext;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.*;

import static com.gl.classext.Aspects.AroundAdvice.applyDefault;
import static com.gl.classext.DeepCloneUtil.deepClone;
import static java.lang.System.out;


public class DeepCloneUtilTest {
    @Test
    void testBasicListClone() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        var copiedList = deepClone(list, false);
        assertEquals(list, copiedList);
    }

    @Test
    void testSimpleListCloneWithModification() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        var copiedList = deepClone(list, false);
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
        var copiedMap = deepClone(complexMap, false);
        assertEquals(complexMap, copiedMap);
    }

    @Test
    void testCircularReferencesClone() {
        Map<String, Object> complexMap = new HashMap<>();
        complexMap.put("numbers", Arrays.asList(1, 2, 3, new TestObject("test", 42)));
        ArrayList<Object> circularRefList = new ArrayList<>(Arrays.asList(100, 200, 300));
        circularRefList.add(circularRefList);
        complexMap.put("strings", Arrays.asList("one", "two", "three", circularRefList));
        assertThrows(IllegalArgumentException.class, () -> deepClone(complexMap, false));
    }

    @Test
    void testDeepCloneAroundAspect() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(DynamicClassExtensionTest.Item_Shippable.class).
                operationName("toString").
                operation(Object.class, Object::toString).
                before((operation, object, args) -> out.println("BEFORE: " + object + "-> toString()")).
                after((result, operation, object, args) -> out.println("AFTER: result - " + result)).
                around((performer, operation, object, args) -> "AROUND " + applyDefault(performer, operation, object, args)).
                build();

        DynamicClassExtensionTest.Book book = new DynamicClassExtensionTest.Book("The Mythical Man-Month");
        DynamicClassExtensionTest.Item_Shippable extension = dynamicClassExtension.extension(book, DynamicClassExtensionTest.Item_Shippable.class);
        String result =  extension.toString();
        out.println("RESULT: " + result);
        assertEquals("AROUND Book[\"The Mythical Man-Month\"]", result);
    }
}
