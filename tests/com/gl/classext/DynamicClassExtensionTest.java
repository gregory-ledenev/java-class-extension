package com.gl.classext;


import org.junit.jupiter.api.Test;

import java.text.MessageFormat;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
public class DynamicClassExtensionTest {
    interface ItemInterface {
        String getName();
    }

    static class Item implements ItemInterface {
        private final String name;

        public Item(String aName) {
            name = aName;
        }

        @Override
        public String toString() {
            return MessageFormat.format("{0}[\"{1}\"]", getClass().getSimpleName(), getName());
        }

        public String getName() {
            return name;
        }
    }

    static class Book extends Item {
        public Book(String aName) {
            super(aName);
        }
    }

    static class Furniture extends Item {
        public Furniture(String aName) {
            super(aName);
        }
    }

    static class ElectronicItem extends Item {
        public ElectronicItem(String aName) {
            super(aName);
        }
    }

    static class AutoPart extends Item {
        public AutoPart(String aName) {
            super(aName);
        }
    }

    record ShippingInfo(String result) {
    }

    record TrackingInfo(String result) {
    }

    interface Item_Shippable extends ItemInterface {
        ShippingInfo ship();
        TrackingInfo track(boolean isVerbose);
        TrackingInfo track();
        void log(boolean isVerbose);
        void log();

        float calculateShippingCost();
        @OptionalMethod
        float calculateShippingCost(String anInstructions);
    }


    @Test
    void operationTest() {
        DynamicClassExtension build = new DynamicClassExtension();
        build.addExtensionOperation(Item.class, Item_Shippable.class, "ship",
                item -> new ShippingInfo(item.getName() + " item NOT shipped"));
        build.addExtensionOperation(Book.class, Item_Shippable.class, "ship",
                book -> new ShippingInfo(book.getName() + " book shipped"));
        build.addExtensionOperation(Furniture.class, Item_Shippable.class, "ship",
                furniture -> new ShippingInfo(furniture.getName() + " furniture shipped"));
        build.addExtensionOperation(ElectronicItem.class, Item_Shippable.class, "ship",
                item -> new ShippingInfo(item.getName() + " electronic item shipped"));

        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        StringBuilder shippingInfos = new StringBuilder();
        for (Item item : items) {
            ShippingInfo shippingInfo = build.extension(item, Item_Shippable.class).ship();
            if (!shippingInfos.isEmpty())
                shippingInfos.append("\n");
            shippingInfos.append(shippingInfo);
            System.out.println(shippingInfo);
        }

        assertEquals("""
                     ShippingInfo[result=The Mythical Man-Month book shipped]
                     ShippingInfo[result=Sofa furniture shipped]
                     ShippingInfo[result=Soundbar electronic item shipped]
                     ShippingInfo[result=Tire item NOT shipped]""",
                shippingInfos.toString());
    }

    @Test
    void operationUsingBuilderTest() {

        StringBuilder shippingLog = new StringBuilder();

        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);
        dynamicClassExtension.setVerbose(true);
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        StringBuilder shippingInfos = new StringBuilder();
        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            extension.log();
            if (!shippingInfos.isEmpty())
                shippingInfos.append("\n");
            shippingInfos.append(extension.ship());
            shippingInfos.append("\n").append(extension.track());
            extension.getName();
        }
        System.out.println(shippingLog);
        System.out.println(shippingInfos);

        assertEquals("""
                     The Mythical Man-Month is about to be shipped
                     Sofa is about to be shipped
                     Soundbar is about to be shipped
                     Tire is about to be shipped""", shippingLog.toString());

        assertEquals("""
                     ShippingInfo[result=The Mythical Man-Month book shipped]
                     TrackingInfo[result=The Mythical Man-Month item on its way]
                     ShippingInfo[result=Sofa furniture shipped]
                     TrackingInfo[result=Sofa item on its way]
                     ShippingInfo[result=Soundbar electronic item shipped]
                     TrackingInfo[result=Soundbar item on its way]
                     ShippingInfo[result=Tire item NOT shipped]
                     TrackingInfo[result=Tire item on its way]""",
                shippingInfos.toString());
    }

    @Test
    void polymorphismTest() {

        StringBuilder shippingLog = new StringBuilder();

        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        StringBuilder names = new StringBuilder();
        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            assertEquals(extension, item);
            if (!names.isEmpty()) {
                names.append("\n");
            }
            names.append(extension.toString());
        }
        System.out.println(names);

        assertEquals("""
                     Book["The Mythical Man-Month"]
                     Furniture["Sofa"]
                     ElectronicItem["Soundbar"]
                     AutoPart["Tire"]""", names.toString());

        names.setLength(0);
        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            assertEquals(extension, item);
            if (!names.isEmpty()) {
                names.append("\n");
            }
            names.append(extension.getName());
        }
        System.out.println(names);

        assertEquals("""
                     The Mythical Man-Month
                     Sofa
                     Soundbar
                     Tire[OVERRIDDEN]""", names.toString());
    }

    @Test
    void toStringTest() {
        StringBuilder shippingLog = new StringBuilder();

        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);
        String string = dynamicClassExtension.toString();
        System.out.println(string);
        assertEquals("""
                     interface com.gl.classext.ClassExtension$DelegateHolder {
                         getDelegate {
                             java.lang.String -> T getDelegate()
                         }
                     }
                     interface com.gl.classext.DynamicClassExtensionTest$Item_Shippable {
                         getName {
                             com.gl.classext.DynamicClassExtensionTest$AutoPart -> T getName()
                         }
                         log {
                             com.gl.classext.DynamicClassExtensionTest$Item -> void log()
                             com.gl.classext.DynamicClassExtensionTest$Item -> void log(T)
                         }
                         ship {
                             com.gl.classext.DynamicClassExtensionTest$Book -> T ship()
                             com.gl.classext.DynamicClassExtensionTest$ElectronicItem -> T ship()
                             com.gl.classext.DynamicClassExtensionTest$Furniture -> T ship()
                             com.gl.classext.DynamicClassExtensionTest$Item -> T ship()
                         }
                         track {
                             com.gl.classext.DynamicClassExtensionTest$Item -> T track()
                             com.gl.classext.DynamicClassExtensionTest$Item -> T track(T)
                         }
                     }""", string);
    }

    @Test
    void toStringGroupedByObjectClassTest() {
        StringBuilder shippingLog = new StringBuilder();

        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);
        String string = dynamicClassExtension.toString(false);
        System.out.println(string);
        assertEquals("""
                     interface com.gl.classext.ClassExtension$DelegateHolder {
                         java.lang.String {
                             T getDelegate()
                         }
                     }
                     interface com.gl.classext.DynamicClassExtensionTest$Item_Shippable {
                         com.gl.classext.DynamicClassExtensionTest$AutoPart {
                             T getName()
                         }
                         com.gl.classext.DynamicClassExtensionTest$Book {
                             T ship()
                         }
                         com.gl.classext.DynamicClassExtensionTest$ElectronicItem {
                             T ship()
                         }
                         com.gl.classext.DynamicClassExtensionTest$Furniture {
                             T ship()
                         }
                         com.gl.classext.DynamicClassExtensionTest$Item {
                             void log()
                             void log(T)
                             T ship()
                             T track()
                             T track(T)
                         }
                     }""", string);
    }

    @Test
    void missingOperationTest() {
        StringBuilder shippingLog = new StringBuilder();

        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);
        try {
            Item_Shippable extension = dynamicClassExtension.extension(new Book("The Mythical Man-Month"), Item_Shippable.class);
            extension.calculateShippingCost();
            fail("Unexpectedly succeeded call: float calculateShippingCost()");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Test
    void removeOperationTest() {

        StringBuilder shippingLog = new StringBuilder();

        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            extension.log();
        }
        System.out.println(shippingLog);
        String expectedLog = """
                          The Mythical Man-Month is about to be shipped
                          Sofa is about to be shipped
                          Soundbar is about to be shipped
                          Tire is about to be shipped""";
        assertEquals(expectedLog, shippingLog.toString());

        // remove log(boolean)
        shippingLog.setLength(0);
        dynamicClassExtension.builder(Item_Shippable.class).
                opName("log").
                removeOp(Item.class, new Class<?>[]{Boolean.class});
        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            extension.log();
        }
        System.out.println(shippingLog);

        // remove log()
        dynamicClassExtension = setupDynamicClassExtension(shippingLog);
        shippingLog.setLength(0);
        dynamicClassExtension.builder(Item_Shippable.class).
                opName("log").
                removeOp(Item.class, null);
        try {
            for (Item item : items) {
                Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
                extension.log();
            }
            fail("Unexpectedly succeeded call: log()");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Test
    void noOperationNameTest() {
        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    op(Item.class, book -> null).
                    build();
            fail("Somehow created an operation with no name");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    void operationFailedToDetectDuplicateTest() {
        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    opName("ship").
                        op(Item.class, book -> null).
                        op(Item.class, book -> null).
                    build();
            fail("Failed to detect duplicated operation: T ship()");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    opName("ship").
                        op(Item.class, book -> null).
                        voidOp(Item.class, book -> {}).
                    build();
            fail("Failed to detect duplicated operation: void ship()");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    opName("ship").
                        voidOp(Item.class, book -> {}).
                        op(Item.class, book -> null).
                    build();
            fail("Failed to detect duplicated operation: ship()");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    opName("ship").
                    op(Item.class, (Item book, Boolean isVerbose) -> null).
                    op(Item.class, (Item book, Boolean isVerbose) -> null).
                    build();
            fail("Failed to detect duplicated operation: T ship(boolean)");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    opName("ship").
                        voidOp(Item.class, (Item book, Boolean isVerbose) -> {}).
                        op(Item.class, (Item book, Boolean isVerbose) -> null).
                    build();
            fail("Failed to detect duplicated operation: T ship(boolean)");
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Test
    void operationWronglyDetectedDuplicateTest() {
        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    opName("ship").
                        op(Item.class, book -> null).
                        op(Item.class, (Item book, Boolean isVerbose) -> null).
                    build();
        } catch (Exception ex) {
            fail("Wrongly detected duplicated operation: T ship(boolean)");
        }

        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    opName("ship").
                        op(Item.class, (Item book, Boolean isVerbose) -> null).
                        op(Item.class, book -> null).
                    build();
        } catch (Exception ex) {
            fail("Wrongly detected duplicated operation: T ship()");
        }

        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    opName("ship").
                        op(Item.class, book -> null).
                        voidOp(Item.class, (Item book, Boolean isVerbose) -> {}).
                    build();
        } catch (Exception ex) {
            fail("Wrongly detected duplicated operation: void ship(boolean)");
        }

        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    opName("ship").
                        voidOp(Item.class, (Item book, Boolean isVerbose) -> {}).
                        op(Item.class, book -> null).
                    build();
        } catch (Exception ex) {
            fail("Wrongly detected duplicated operation: T ship()");
        }
    }

    @Test
    void verboseOperationUsingBuilderTest() {

        StringBuilder shippingLog = new StringBuilder();

        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        StringBuilder shippingInfos = new StringBuilder();
        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            extension.log(true);
            if (!shippingInfos.isEmpty())
                shippingInfos.append("\n");
            shippingInfos.append(extension.ship());
            shippingInfos.append("\n").append(extension.track(true));
        }
        System.out.println(shippingLog);
        System.out.println(shippingInfos);

        assertEquals("""
                     The Mythical Man-Month is about to be shipped in 1 hour
                     Sofa is about to be shipped in 1 hour
                     Soundbar is about to be shipped in 1 hour
                     Tire is about to be shipped in 1 hour""", shippingLog.toString());

        assertEquals("""
                     ShippingInfo[result=The Mythical Man-Month book shipped]
                     TrackingInfo[result=The Mythical Man-Month item on its wayStatus: SHIPPED]
                     ShippingInfo[result=Sofa furniture shipped]
                     TrackingInfo[result=Sofa item on its wayStatus: SHIPPED]
                     ShippingInfo[result=Soundbar electronic item shipped]
                     TrackingInfo[result=Soundbar item on its wayStatus: SHIPPED]
                     ShippingInfo[result=Tire item NOT shipped]
                     TrackingInfo[result=Tire item on its wayStatus: SHIPPED]""",
                shippingInfos.toString());
    }

    private static DynamicClassExtension setupDynamicClassExtension(StringBuilder shippingLog) {
        DynamicClassExtension result = new DynamicClassExtension().builder(Item_Shippable.class).
                opName("ship").
                    op(Item.class, book -> new ShippingInfo(book.getName() + " item NOT shipped")).
                    op(Book.class, book -> new ShippingInfo(book.getName() + " book shipped")).async((Book aO, Throwable ex) -> {}).
                    op(Furniture.class, furniture -> new ShippingInfo(furniture.getName() + " furniture shipped")).
                    op(ElectronicItem.class, electronicItem -> new ShippingInfo(electronicItem.getName() + " electronic item shipped")).
                opName("log").
                    voidOp(Item.class, (Item item, Boolean isVerbose) -> {
                        if (!shippingLog.isEmpty())
                            shippingLog.append("\n");
                        shippingLog.append(item.getName()).append(" is about to be shipped in 1 hour");
                    }).
                    voidOp(Item.class, item -> {
                        if (!shippingLog.isEmpty())
                            shippingLog.append("\n");
                        shippingLog.append(item.getName()).append(" is about to be shipped");
                    }).
                opName("track").
                    op(Item.class, item -> new TrackingInfo(item.getName() + " item on its way")).
                    op(Item.class, (Item item, Boolean isVerbose) -> new TrackingInfo(item.getName() +
                            " item on its way" + (isVerbose ? "Status: SHIPPED" : ""))).
                opName("getName").
                    op(AutoPart.class, item -> item.getName()  + "[OVERRIDDEN]").
                build();

        result = result.builder(StaticClassExtension.DelegateHolder.class).
                opName("getDelegate").
                    op(String.class, item -> null).
                build();

        return result;
    }

    /**
     * Test for cached extension
     */
    @Test
    void cacheTest() {
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(new StringBuilder());
        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        assertSame(extension, dynamicClassExtension.extension(book, Item_Shippable.class));
    }

    /**
     * Test for cached extension
     */
    @SuppressWarnings("unchecked")
    @Test
    void cacheEntryRemovalTest() {
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(new StringBuilder());
        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        assertSame(extension, dynamicClassExtension.extension(book, Item_Shippable.class));
        dynamicClassExtension.extensionCache.remove(new ClassExtensionKey(book, Item_Shippable.class));
        assertNotSame(extension, dynamicClassExtension.extension(book, Item_Shippable.class));
    }

    /**
     * Test for not cached extension
     */
    @Test
    void nonCacheTest() {
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(new StringBuilder());
        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extensionNoCache(book, Item_Shippable.class);
        assertNotSame(extension, dynamicClassExtension.extensionNoCache(book, Item_Shippable.class));
    }

    /**
     * Tests for cached extension release by GC and cleaned manually
     */
    @Test
    void cacheCleanupTest() {
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(new StringBuilder());
        Book book = new Book("The Mythical Man-Month");

        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        assertSame(extension, dynamicClassExtension.extension(book, Item_Shippable.class));

        assertFalse(dynamicClassExtension.cacheIsEmpty());
        dynamicClassExtension.cacheCleanup();
        assertFalse(dynamicClassExtension.cacheIsEmpty());
    }

    /**
     * Tests for cached extension cleared
     */
    @Test
    void cacheClearTest() {
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(new StringBuilder());
        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        assertSame(extension, dynamicClassExtension.extension(book, Item_Shippable.class));
        dynamicClassExtension.cacheClear();
        assertTrue(dynamicClassExtension.cacheIsEmpty());
        assertNotSame(extension, dynamicClassExtension.extension(book, Item_Shippable.class));
    }

    /**
     * Tests for cached extension release by GC
     */
    @Test
    void scheduledCleanupCacheTest() {
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(new StringBuilder());
        Book book = new Book("The Mythical Man-Month");

        dynamicClassExtension.scheduleCacheCleanup();
        try {
            dynamicClassExtension.extension(book, Item_Shippable.class);
            System.gc();
            assertFalse(dynamicClassExtension.cacheIsEmpty());
            try {
                System.out.println("Waiting 1.5 minutes for automatic cache cleanup...");
                Thread.sleep(90000);
            } catch (InterruptedException aE) {
                // do nothing
            }
            assertTrue(dynamicClassExtension.cacheIsEmpty());
        } finally {
            dynamicClassExtension.shutdownCacheCleanup();
        }
    }

    @Test
    void performanceTestDynamic() {
        StringBuilder shippingLog = new StringBuilder();

        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            for (Item item : items) {
                Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
                extension.log();
            }
        }
        System.out.println("DYNAMIC - Elapsed time: " + ((System.currentTimeMillis()-startTime) / 1000f));
    }

    @Test
    void performanceTest() {
        for (int i = 0; i < 20; i++) {
            performanceTestDynamic();
            StaticClassExtensionTest.performanceTestStatic();
            System.out.println("-----------");
            System.gc();
        }
    }

        @Test
    void validationTest() {
        StringBuilder shippingLog = new StringBuilder();
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);

        try {
            dynamicClassExtension.checkValid(ElectronicItem.class, Item_Shippable.class);
            fail("Unexpectedly valid extension: " + Item_Shippable.class.getName());
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Test
    void checkOperationsImplementedTest() {
        StringBuilder shippingLog = new StringBuilder();
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);

        assertFalse(dynamicClassExtension.isPresent(ElectronicItem.class, Item_Shippable.class, "calculateShippingCost", null));
        assertTrue(dynamicClassExtension.isPresent(ElectronicItem.class, Item_Shippable.class, "log", new Class<?>[]{boolean.class}));
        assertTrue(dynamicClassExtension.isPresent(ElectronicItem.class, Item_Shippable.class, "log", null));
    }

    @Test
    void equalsTest() {
        StringBuilder shippingLog = new StringBuilder();
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);

        assertTrue(ClassExtension.equals(book, extension));
        assertTrue(ClassExtension.equals(extension, book));

        assertNotEquals(book, extension);
        assertEquals(extension, book);

        assertFalse(ClassExtension.equals(book, null));
        assertFalse(ClassExtension.equals(extension, null));
        assertFalse(ClassExtension.equals(null, book));
        assertFalse(ClassExtension.equals(null, extension));
    }

    @Test
    void delegateTest() {
        StringBuilder shippingLog = new StringBuilder();
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        assertSame(book, ClassExtension.getDelegate(extension));
    }

    @Test
    void checkToString() {
        StringBuilder shippingLog = new StringBuilder();
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        assertEquals(book.toString(), extension.toString());
    }

    @Test
    void checkHashCodeString() {
        StringBuilder shippingLog = new StringBuilder();
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        assertEquals(book.hashCode(), extension.hashCode());
    }

    final private StringBuilder ASPECT_LOG = new StringBuilder();
    void aspectLog(String aValue) {
        if (!ASPECT_LOG.isEmpty())
            ASPECT_LOG.append("\n");
        ASPECT_LOG.append(aValue);
    }

    @Test
    void testWrongAsyncPlacement1() {
        try {
            DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).async().
                    opName("hashCode").
                    op(Object.class, Object::hashCode).
                    build();
            fail("Wrong async() call right after the builder init");
        } catch (Exception ex) {
            // it is fine
        }
    }

    @Test
    void testWrongAsyncPlacement2() {
        try {
            DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
                    opName("hashCode").async().
                    op(Object.class, Object::hashCode).
                    build();
            fail("Wrong async() call right after operation name");
        } catch (Exception ex) {
            // it is fine
        }
    }

    @Test
    void testAsync() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
                opName("toString").
                    op(Object.class, Object::toString).async().
                opName("hashCode").
                    op(Object.class, Object::hashCode).
                        async((Integer hashCode, Throwable throwable) -> System.out.println(hashCode.toString())).
                build();

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        extension.toString();
        extension.hashCode();
    }

    @Test
    void testAspect() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
                opName("toString").
                    op(Object.class, Object::toString).
                        before((object, args) -> System.out.println("BEFORE: " + object + "-> toString()")).
                        after(result -> System.out.println("AFTER: result - " + result)).
                build();

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        extension.toString();
    }

    @Test
    void testAsyncAspect() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
                opName("toString").
                op(Object.class, Object::toString).
                    async().
                    before((object, args) -> System.out.println("BEFORE: " + object + "-> toString()")).
                    after(result -> System.out.println("AFTER: result - " + result)).
                build();

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        extension.toString();
    }
}


