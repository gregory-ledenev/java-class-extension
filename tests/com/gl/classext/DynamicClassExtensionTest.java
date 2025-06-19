package com.gl.classext;


import org.junit.jupiter.api.Test;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.*;

import static com.gl.classext.Aspects.*;
import static com.gl.classext.Aspects.AroundAdvice.applyDefault;
import static com.gl.classext.DynamicClassExtension.lambdaWithDescription;
import static java.lang.System.out;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("unused")
public class DynamicClassExtensionTest {

    interface ItemInterface {
        String getName();
        void setName(String name);
    }

    static class Item implements ItemInterface {
        private String name;

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

        @Override
        public void setName(String name) {
            this.name = name;
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

    interface Accountable {
        int getCost();
    }

    static class AccountableImpl implements Accountable {
        private final int cost;

        @Override
        public int getCost() {
            return cost;
        }

        public AccountableImpl(int aCost) {
            cost = aCost;
        }
    }

    interface Shippable {
        ShippingInfo ship();
        TrackingInfo track(boolean isVerbose);
        TrackingInfo track();
    }

    interface Item_Shippable extends ItemInterface, Shippable {
        @ObtainExtension
        Accountable getAccountable();
        void log(boolean isVerbose);
        void log();

        float calculateShippingCost();
        @OptionalMethod
        float calculateShippingCost(String anInstructions);
        String throwException();
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
            out.println(shippingInfo);
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
            ShippingInfo ship = extension.ship();
            shippingInfos.append(ship);
            shippingInfos.append("\n").append(extension.track());
            extension.getName();
        }
        out.println(shippingLog);
        out.println(shippingInfos);

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
    void operationUsingBuilderWithDifferentCompositionTest() {

        StringBuilder shippingLog = new StringBuilder();

        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtensionWithDifferentComposition(shippingLog);
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
            ShippingInfo ship = extension.ship();
            shippingInfos.append(ship);
            shippingInfos.append("\n").append(extension.track());
            shippingInfos.append("\nCOST: ").append(extension.getAccountable().getCost());
            extension.getName();
        }
        out.println(shippingLog);
        out.println(shippingInfos);

        assertEquals("""
                     The Mythical Man-Month is about to be shipped
                     Sofa is about to be shipped
                     Soundbar is about to be shipped
                     Tire is about to be shipped""", shippingLog.toString());

        assertEquals("""
                ShippingInfo[result=The Mythical Man-Month book shipped]
                TrackingInfo[result=The Mythical Man-Month item on its way]
                COST: 223
                ShippingInfo[result=Sofa furniture shipped]
                TrackingInfo[result=Sofa item on its way]
                COST: 223
                ShippingInfo[result=Soundbar electronic item shipped]
                TrackingInfo[result=Soundbar item on its way]
                COST: 223
                ShippingInfo[result=Tire item NOT shipped]
                TrackingInfo[result=Tire item on its way]
                COST: 223""",
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
        out.println(names);

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
        out.println(names);

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
        out.println(string);
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
                             com.gl.classext.DynamicClassExtension$Null -> T ship()
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
        out.println(string);
        assertEquals("""
                     interface com.gl.classext.ClassExtension$DelegateHolder {
                         java.lang.String {
                             T getDelegate()
                         }
                     }
                     interface com.gl.classext.DynamicClassExtensionTest$Item_Shippable {
                         com.gl.classext.DynamicClassExtension$Null {
                             T ship()
                         }
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
            out.println(ex.getMessage());
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
        out.println(shippingLog);
        String expectedLog = """
                          The Mythical Man-Month is about to be shipped
                          Sofa is about to be shipped
                          Soundbar is about to be shipped
                          Tire is about to be shipped""";
        assertEquals(expectedLog, shippingLog.toString());

        // remove log(boolean)
        shippingLog.setLength(0);
        dynamicClassExtension.builder(Item_Shippable.class).
                operationName("log").
                removeOperation(Item.class, new Class<?>[]{Boolean.class});
        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            extension.log();
        }
        out.println(shippingLog);

        // remove log()
        dynamicClassExtension = setupDynamicClassExtension(shippingLog);
        shippingLog.setLength(0);
        dynamicClassExtension.builder(Item_Shippable.class).
                operationName("log").
                removeOperation(Item.class, null);
        try {
            for (Item item : items) {
                Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
                extension.log();
            }
            fail("Unexpectedly succeeded call: log()");
        } catch (Exception ex) {
            out.println(ex.getMessage());
        }
    }

    @Test
    void noOperationNameTest() {
        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    operation(Item.class, book -> null).
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
                    operationName("ship").
                    operation(Item.class, book -> null).
                    operation(Item.class, book -> null).
                    build();
            fail("Failed to detect duplicated operation: T ship()");
        } catch (Exception ex) {
            out.println(ex.getMessage());
        }

        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    operationName("ship").
                    operation(Item.class, book -> null).
                    voidOperation(Item.class, book -> {}).
                    build();
            fail("Failed to detect duplicated operation: void ship()");
        } catch (Exception ex) {
            out.println(ex.getMessage());
        }

        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    operationName("ship").
                    voidOperation(Item.class, book -> {}).
                    operation(Item.class, book -> null).
                    build();
            fail("Failed to detect duplicated operation: ship()");
        } catch (Exception ex) {
            out.println(ex.getMessage());
        }

        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    operationName("ship").
                    operation(Item.class, (Item book, Boolean isVerbose) -> null).
                    operation(Item.class, (Item book, Boolean isVerbose) -> null).
                    build();
            fail("Failed to detect duplicated operation: T ship(boolean)");
        } catch (Exception ex) {
            out.println(ex.getMessage());
        }

        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    operationName("ship").
                    voidOperation(Item.class, (Item book, Boolean isVerbose) -> {}).
                    operation(Item.class, (Item book, Boolean isVerbose) -> null).
                    build();
            fail("Failed to detect duplicated operation: T ship(boolean)");
        } catch (Exception ex) {
            out.println(ex.getMessage());
        }
    }

    @Test
    void operationWronglyDetectedDuplicateTest() {
        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    operationName("ship").
                    operation(Item.class, book -> null).
                    operation(Item.class, (Item book, Boolean isVerbose) -> null).
                    build();
        } catch (Exception ex) {
            fail("Wrongly detected duplicated operation: T ship(boolean)");
        }

        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    operationName("ship").
                    operation(Item.class, (Item book, Boolean isVerbose) -> null).
                    operation(Item.class, book -> null).
                    build();
        } catch (Exception ex) {
            fail("Wrongly detected duplicated operation: T ship()");
        }

        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    operationName("ship").
                    operation(Item.class, book -> null).
                    voidOperation(Item.class, (Item book, Boolean isVerbose) -> {}).
                    build();
        } catch (Exception ex) {
            fail("Wrongly detected duplicated operation: void ship(boolean)");
        }

        try {
            new DynamicClassExtension().builder(Item_Shippable.class).
                    operationName("ship").
                    voidOperation(Item.class, (Item book, Boolean isVerbose) -> {}).
                    operation(Item.class, book -> null).
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
        out.println(shippingLog);
        out.println(shippingInfos);

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
        return setupDynamicClassExtension(new DynamicClassExtension(), shippingLog);
    }

    private static DynamicClassExtension setupDynamicClassExtension(DynamicClassExtension aDynamicClassExtension, StringBuilder aShippingLog) {
        return aDynamicClassExtension.builder().
                extensionInterface(Item_Shippable.class).
                    operationName("ship").
                        operation(Item.class, book -> new ShippingInfo(book.getName() + " item NOT shipped")).
                        operation(Book.class, book -> new ShippingInfo(book.getName() + " book shipped")).
                        operation(Furniture.class, furniture -> new ShippingInfo(furniture.getName() + " furniture shipped")).
                        operation(ElectronicItem.class, electronicItem -> new ShippingInfo(electronicItem.getName() + " electronic item shipped")).
                        operation((Class<?>) null, e -> new ShippingInfo("Nothing to ship")).
                    operationName("log").
                        voidOperation(Item.class, (Item item, Boolean isVerbose) -> {
                                if (!aShippingLog.isEmpty())
                                    aShippingLog.append("\n");
                                aShippingLog.append(item.getName()).append(" is about to be shipped in 1 hour");
                            }).
                        voidOperation(Item.class, item -> {
                                if (!aShippingLog.isEmpty())
                                    aShippingLog.append("\n");
                                aShippingLog.append(item.getName()).append(" is about to be shipped");
                            }).
                    operationName("track").
                        operation(Item.class, item -> new TrackingInfo(item.getName() + " item on its way")).
                        operation(Item.class, (Item item, Boolean isVerbose) -> new TrackingInfo(item.getName() +
                                    " item on its way" + (isVerbose ? "Status: SHIPPED" : ""))).
                    operationName("getName").
                        operation(AutoPart.class, item -> item.getName()  + "[OVERRIDDEN]").
                build().builder(ClassExtension.DelegateHolder.class).
                    operationName("getDelegate").
                        operation(String.class, item -> null).
                build();
    }

    private static DynamicClassExtension setupDynamicClassExtensionWithDifferentComposition(StringBuilder shippingLog) {
        DynamicClassExtension result = new DynamicClassExtension().builder().
                extensionInterface(Item_Shippable.class).
                    objectClass(Item.class).
                        operation("getAccountable", (Item item) -> new AccountableImpl(100)).
                        operation("ship", (Item item) -> new ShippingInfo(item.getName() + " item NOT shipped")).
                        voidOperation("log", (Item item, Boolean isVerbose) -> {
                    if (!shippingLog.isEmpty())
                        shippingLog.append("\n");
                    shippingLog.append(item.getName()).append(" is about to be shipped in 1 hour");
                }).
                        voidOperation("log", (Item item) -> {
                    if (!shippingLog.isEmpty())
                        shippingLog.append("\n");
                    shippingLog.append(item.getName()).append(" is about to be shipped");
                }).
                        operation("track", (Item item) -> new TrackingInfo(item.getName() + " item on its way")).
                        operation("track", (Item item, Boolean isVerbose) -> new TrackingInfo(item.getName() +
                                " item on its way" + (isVerbose ? "Status: SHIPPED" : ""))).
                objectClass(Book.class).
                        operation("ship", (Book book) -> new ShippingInfo(book.getName() + " book shipped")).
                    objectClass(Furniture.class).
                        operation("ship", (Furniture furniture) -> new ShippingInfo(furniture.getName() + " furniture shipped")).
                    objectClass(ElectronicItem.class).
                        operation("ship", (ElectronicItem electronicItem) -> new ShippingInfo(electronicItem.getName() + " electronic item shipped")).
                    objectClass(AutoPart.class).
                        operation("getName", (Item item) -> item.getName()  + "[OVERRIDDEN]").
                extensionInterface(Accountable.class).
                    objectClass(AccountableImpl.class).
                        operation("getCost", (Accountable accountable) -> accountable.getCost() + 123).

                build();

        result = result.builder(ClassExtension.DelegateHolder.class).
                operationName("getDelegate").
                operation(String.class, item -> null).
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

    @ExtensionInterface(cachePolicy = ClassExtension.CachePolicy.DISABLED)
    interface NonCachedItem_Shippable extends Item_Shippable {
    }

    /**
     * Test for optionally cached extension
     */
    @Test
    void cacheOptionalTest() {
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(new StringBuilder());
        Book book = new Book("The Mythical Man-Month");

        dynamicClassExtension.setCacheEnabled(false);
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        assertNotSame(extension, dynamicClassExtension.extension(book, Item_Shippable.class));

        dynamicClassExtension.setCacheEnabled(true);
        extension = dynamicClassExtension.extension(book, NonCachedItem_Shippable.class);
        assertNotSame(extension, dynamicClassExtension.extension(book, NonCachedItem_Shippable.class));
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
        Item_Shippable extension = dynamicClassExtension.extensionNoCache(book, null, Item_Shippable.class);
        assertNotSame(extension, dynamicClassExtension.extensionNoCache(book, null, Item_Shippable.class));
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
                out.println("Waiting 1.5 minutes for automatic cache cleanup...");
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
        out.println("DYNAMIC - Elapsed time: " + ((System.currentTimeMillis()-startTime) / 1000f));
    }

    @Test
    void performanceTest() {
        for (int i = 0; i < 5; i++) {
            performanceTestDynamic();
            StaticClassExtensionTest.performanceTestStatic();
            out.println("-----------");
            System.gc();
        }
    }

    @Test
    void ensureInvalidTest() {
        StringBuilder shippingLog = new StringBuilder();
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);

        try {
            dynamicClassExtension.checkValid(ElectronicItem.class, Item_Shippable.class, true);
            fail("Unexpectedly valid extension: " + Item_Shippable.class.getName());
        } catch (IllegalArgumentException ex) {
            out.println(ex.getMessage());
        }
    }

    @Test
    void callUndefinedOperationTest() {
        StringBuilder shippingLog = new StringBuilder();
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable itemShippable = dynamicClassExtension.extension(book,
                aMethod -> 100f, // optional and missing methods handler
                Item_Shippable.class);
        // must succeed as it is annotated with @OptionalMethod and the missing methods handler will be called
        assertEquals(100f, itemShippable.calculateShippingCost("asap"));
        try {
            assertFalse(dynamicClassExtension.isPresentOperation(Book.class, Item_Shippable.class,
                    "calculateShippingCost", null));
            // must fail as it is not annotated with @OptionalMethod
            itemShippable.calculateShippingCost();
            fail("Unexpectedly succeeded call: calculateShippingCost()");
        } catch (IllegalArgumentException ex) {
            out.println(ex.getMessage());
        }
    }

    @Test
    void listUndefinedOperationsTest() {
        StringBuilder shippingLog = new StringBuilder();
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(shippingLog);
        String result = String.join("\n", dynamicClassExtension.listUndefinedOperations(ElectronicItem.class, Item_Shippable.class, true));
        out.println(result);
        assertEquals("""
                     T calculateShippingCost()
                     T getAccountable()
                     T throwException()""", result);
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
                    operationName("hashCode").
                    operation(Object.class, Object::hashCode).
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
                    operationName("hashCode").async().
                    operation(Object.class, Object::hashCode).
                    build();
            fail("Wrong async() call right after operation name");
        } catch (Exception ex) {
            // it is fine
        }
    }

    @Test
    void testAsync() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
                operationName("toString").
                operation(Object.class, Object::toString).async().
                operationName("hashCode").
                operation(Object.class, Object::hashCode).
                        async((Integer hashCode, Throwable throwable) -> out.println(hashCode.toString())).
                build();

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        extension.toString();
        extension.hashCode();
    }

    @Test
    void testAspect() {
        List<String> out = new ArrayList<>();
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
                operationName("toString").
                operation(Object.class, Object::toString).
                        before((operation, object, args) -> out.add("BEFORE: " + object + "-> toString()")).
                        after((result, operation, object, args) -> out.add("AFTER: result - " + result)).
                build();

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        System.out.println(extension.toString());
        String result = String.join("\n", out);
        System.out.println(result);
        assertEquals("""
                     BEFORE: Book["The Mythical Man-Month"]-> toString()
                     AFTER: result - Book["The Mythical Man-Month"]""", result);
    }

    @Test
    void testAroundAspect() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
                operationName("toString").
                operation(Object.class, Object::toString).
                        before((operation, object, args) -> out.println("BEFORE: " + object + "-> toString()")).
                        after((result, operation, object, args) -> out.println("AFTER: result - " + result)).
                        around((performer, operation, object, args) -> "AROUND " + applyDefault(performer, operation, object, args)).
                build();

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        String result =  extension.toString();
        out.println("RESULT: " + result);
        assertEquals("AROUND Book[\"The Mythical Man-Month\"]", result);
    }

    @Test
    void testAsyncAspect() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
                operationName("toString").
                operation(Object.class, Object::toString).
                    async().
                    before((operation, object, args) -> out.println("BEFORE: " + object + "-> toString()")).
                    after((result, operation, object, args) -> out.println("AFTER: result - " + result)).
                build();

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        extension.toString();
    }

    @Test
    void testAlterAspectsOperation() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
                operationName("toString").
                operation(Object.class, Object::toString).
                build();

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        out.println("RESULT: " + extension.toString());

        // add aspects
        StringBuilder stringBuilder = new StringBuilder();
        dynamicClassExtension.builder(Item_Shippable.class).
                operationName("toString").
                alterOperation(Object.class,new Class<?>[0]).
                        before((operation, object, args) -> stringBuilder.append("BEFORE: ").append(object).append("-> toString()\n")).
                        after((result, operation, object, args) -> stringBuilder.append("AFTER: result - ").append(result).append("\n")).
                build();
        extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        stringBuilder.append("RESULT: " + extension.toString());
        out.println(stringBuilder.toString());
        assertEquals("""
                     BEFORE: Book["The Mythical Man-Month"]-> toString()
                     AFTER: result - Book["The Mythical Man-Month"]
                     RESULT: Book["The Mythical Man-Month"]""", stringBuilder.toString());

        // remove aspects
        stringBuilder.setLength(0);
        dynamicClassExtension.builder(Item_Shippable.class).
                operationName("toString").
                alterOperation(Object.class,new Class<?>[0]).
                        before(null).
                        after(null).
                build();
        extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        stringBuilder.append("RESULT: " + extension.toString());
        out.println(stringBuilder.toString());
        assertEquals("""
                     RESULT: Book["The Mythical Man-Month"]""", stringBuilder.toString());
    }

    @Test
    void testAlterAsync() {
        StringBuilder stringBuilder = new StringBuilder();
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
                operationName("toString").
                operation(Object.class, Object::toString).
                build();

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        stringBuilder.append("RESULT: " + extension.toString());
        out.println(stringBuilder.toString());
        assertEquals("""
                     RESULT: Book["The Mythical Man-Month"]""", stringBuilder.toString());

        // alter to async
        stringBuilder.setLength(0);
        dynamicClassExtension.builder(Item_Shippable.class).
                operationName("toString").
                alterOperation(Object.class, new Class<?>[0]).
                        async((o, ex) -> stringBuilder.append("ASYNC RESULT: " + o + "\n")).
                build();
        String result = extension.toString();
        sleep();
        stringBuilder.append("RESULT: " + result);
        out.println(stringBuilder.toString());
        assertEquals("""
                     ASYNC RESULT: Book["The Mythical Man-Month"]
                     RESULT: null""", stringBuilder.toString());

        // clear async
        stringBuilder.setLength(0);
        dynamicClassExtension.builder(Item_Shippable.class).
                operationName("toString").
                alterOperation(Object.class, new Class<?>[0]).
                        async(false).
                build();
        result = extension.toString();
        sleep();
        stringBuilder.append("RESULT: " + result);
        out.println(stringBuilder.toString());
        assertEquals("""
                    RESULT: Book["The Mythical Man-Month"]""", stringBuilder.toString());
    }

    @ExtensionInterface(aspectsPolicy = ClassExtension.AspectsPolicy.DISABLED)
    interface Item_ShippableNoAspects extends Item_Shippable {}

    @Test
    void testAspectUsingBuilder() {
        List<String> out = new ArrayList<>();
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
//                opName("toString").
//                    op(Object.class, Object::toString).
        operationName("log").
                voidOperation(Item.class, (Item item, Boolean isVerbose) -> out.add(item.getName() + " is about to be shipped in 1 hour")).
                voidOperation(Item.class, item -> out.add(item.getName() +" is about to be shipped")).
                build();
//        dynamicClassExtension.setVerbose(true);

        dynamicClassExtension.aspectBuilder().
                extensionInterface("*").
                    operation("toString()").
                        objectClass(Object.class).
                            before((operation, object, args) -> out.add("BEFORE: " + object + "-> toString()")).
                            after((result, operation, object, args) -> out.add("AFTER: result - " + result)).
                        objectClass(Book.class).
                            before((operation,object, args) -> out.add("BOOK BEFORE: " + object + "-> toString()")).
                            after((result, operation, object, args) -> out.add("BOOK AFTER: result - " + result)).
                        objectClass(AutoPart.class).
                            around((performer, operation, object, args) -> "ALTERED AUTO PART: " + applyDefault(performer, operation, object, args)).
                    objectClass(Item.class).
                        operation("log()").
                            around(AroundAdvice::applyDefault).
                        operation("log(boolean)").
                            after((result, operation, object, args) -> out.add("AFTER log(boolean): result - " + result));

        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            System.out.println(extension.toString());
            extension.log();
            extension.log(true);
        }
        System.out.println("----------- Aspects Enabled");
        String outString = String.join("\n", out);
        System.out.println(outString);
        assertEquals("""
                     BOOK BEFORE: Book["The Mythical Man-Month"]-> toString()
                     BOOK AFTER: result - Book["The Mythical Man-Month"]
                     The Mythical Man-Month is about to be shipped
                     The Mythical Man-Month is about to be shipped in 1 hour
                     AFTER log(boolean): result - null
                     BEFORE: Furniture["Sofa"]-> toString()
                     AFTER: result - Furniture["Sofa"]
                     Sofa is about to be shipped
                     Sofa is about to be shipped in 1 hour
                     AFTER log(boolean): result - null
                     BEFORE: ElectronicItem["Soundbar"]-> toString()
                     AFTER: result - ElectronicItem["Soundbar"]
                     Soundbar is about to be shipped
                     Soundbar is about to be shipped in 1 hour
                     AFTER log(boolean): result - null
                     Tire is about to be shipped
                     Tire is about to be shipped in 1 hour
                     AFTER log(boolean): result - null""", outString);

        dynamicClassExtension.setAspectsEnabled(false);
        out.clear();
        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            System.out.println(extension.toString());
            extension.log();
            extension.log(true);
        }
        System.out.println("----------- Aspects Disabled");
        outString = String.join("\n", out);
        System.out.println(outString);
        assertEquals("""
                     The Mythical Man-Month is about to be shipped
                     The Mythical Man-Month is about to be shipped in 1 hour
                     Sofa is about to be shipped
                     Sofa is about to be shipped in 1 hour
                     Soundbar is about to be shipped
                     Soundbar is about to be shipped in 1 hour
                     Tire is about to be shipped
                     Tire is about to be shipped in 1 hour""", outString);

        dynamicClassExtension.setAspectsEnabled(true);
        out.clear();
        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_ShippableNoAspects.class);
            System.out.println(extension.toString());
            extension.log();
            extension.log(true);
        }
        System.out.println("----------- Aspects Disabled by annotation");
        outString = String.join("\n", out);
        System.out.println(outString);
        assertEquals("""
                     The Mythical Man-Month is about to be shipped
                     The Mythical Man-Month is about to be shipped in 1 hour
                     Sofa is about to be shipped
                     Sofa is about to be shipped in 1 hour
                     Soundbar is about to be shipped
                     Soundbar is about to be shipped in 1 hour
                     Tire is about to be shipped
                     Tire is about to be shipped in 1 hour""", outString);
    }

    @Test
    void logPerformTimeExtensionTest() {
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        for (Item item : items) {
            Item_Shippable extension = Aspects.logPerformanceExtension(item, Item_Shippable.class);
            System.out.println(extension.toString());
        }
    }

    @Test
    void propertyChangeAdviceTest() {

        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension();
        dynamicClassExtension.aspectBuilder().
                extensionInterface(ItemInterface.class).
                objectClass(Item.class).
                    operation("set*(*)").
                        around(new PropertyChangeAdvice(evt -> out.println(evt.toString())));

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        out.println(extension.getName());
        extension.setName("Shining");
        out.println(extension.getName());
        assertEquals("Shining", extension.getName());

    }

    @Test
    void propertyChangeUtilityTest() {
        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = Aspects.propertyChangeExtension(book, Item_Shippable.class,
                evt -> out.println(evt.toString()));
        out.println(extension.getName());
        extension.setName("Shining");
        out.println(extension.getName());
        assertEquals("Shining", extension.getName());
    }

    @Test
    void handleThrowableAdviceTest() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
        operationName("throwException").
                operation(Item.class, aItem -> { throw new RuntimeException("Test exception"); }).
        build();

        dynamicClassExtension.aspectBuilder().
                extensionInterface(ItemInterface.class).
                    objectClass(Item.class).
                        operation("throwException()").
                            around(new HandleThrowableAdvice<String>(() -> null));

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        assertNull(extension.throwException());
    }

    @Test
    void logBeforeAndAfterAdviceTest() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
                aspectBuilder().
                    extensionInterface(ItemInterface.class).
                        objectClass(Item.class).
                        operation("*").
                            before(new LogBeforeAdvice()).
                            after(new LogAfterAdvice()).
                build();

        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            System.out.println(extension.toString());
        }
    }

    @Test
    void extensionWithDescriptionTest() {
        final String description = "Some description for Runnable";
        Runnable extension = lambdaWithDescription((Runnable) () -> {}, description);
        out.println(extension.toString());
        assertEquals(description, extension.toString());
    }

    static int retryTestCount = 0;

    @Test
    void retryAdviceTest() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
                aspectBuilder().extensionInterface(ItemInterface.class).
                objectClass(Item.class).
                    operation("ship()").
                        around(new RetryAdvice(3, 1000,
                                Logger.getLogger(getClass().getName()),
                                (result, ex) -> {
                                    // check if the operation succeeded
                                    if (result instanceof ShippingInfo(String aResult)) {
                                        return ! aResult.contains("Ship");
                                    } else {
                                        return ! (ex instanceof IllegalStateException);
                                    }
                                })).
                build().
                builder(Item_Shippable.class).
                operationName("ship").
                operation(Book.class, (book) -> {
                    if (retryTestCount > 0)
                        return new ShippingInfo("Shipped: " + book);
                    else {
                        retryTestCount++;
                        throw new IllegalStateException("Failed to ship: " + book);
                    }
                }).
                async((result, ex) -> {
                    if (ex != null) {
                        out.println("FAILED: " + ex);
                    } else {
                        out.println(result);
                        assertEquals("ShippingInfo[result=Shipped: Book[\"The Mythical Man-Month\"]]", result);
                    }
                }).
                build();

        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = dynamicClassExtension.extension(book, Item_Shippable.class);
        System.out.println(extension.ship());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    @Test
    void cachedValueAdviceTest() {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%n");

        StringBuilderHandler stringBuilderHandler = new StringBuilderHandler();
        Logger logger = Logger.getLogger(getClass().getName());
        logger.addHandler(stringBuilderHandler);

        CachedValueProvider cache = new CachedValueProvider() {
            private final Map<Object, Object> cache =new HashMap<>();
            @Override
            public Object getOrCreate(OperationPerformKey key, Supplier<?> aValueSupplier) {
                Object result = cache.get(key.object());
                if (result == null) {
                    result = aValueSupplier.get();
                    cache.put(key.object(), result);
                }
                return result;
            }
        };

        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
                aspectBuilder().
                    extensionInterface(ItemInterface.class).
                        objectClass(Item.class).
                            operation("*").
                                around(new CachedValueAdvice(cache, logger)).
                                around((performer, operation, object, args) -> "AROUND: " +
                                        AroundAdvice.applyDefault(performer, operation, object, args)).
                build();

        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            System.out.println(extension.toString());
        }

        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            System.out.println(extension.toString());
        }

        out.println(stringBuilderHandler.getStringBuilder().toString());
        assertEquals("""
                    INFO: Not cached; getting value: Book["The Mythical Man-Month"] -> toString()
                    INFO: Not cached; getting value: Furniture["Sofa"] -> toString()
                    INFO: Not cached; getting value: ElectronicItem["Soundbar"] -> toString()
                    INFO: Not cached; getting value: AutoPart["Tire"] -> toString()
                    """, stringBuilderHandler.getStringBuilder().toString());
    }

    @Test
    void chainedAroundAdviceTest() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
                aspectBuilder().
                    extensionInterface(ItemInterface.class).
                        objectClass(Item.class).
                            operation("*").
                                advices(builder -> {
                                    builder.around((performer, operation, object, args) -> "AROUND 1: " + AroundAdvice.applyDefault(performer, operation, object, args)).
                                            around((performer, operation, object, args) -> "AROUND 2: " + AroundAdvice.applyDefault(performer, operation, object, args)).
                                            around((performer, operation, object, args) -> "AROUND 3: " + AroundAdvice.applyDefault(performer, operation, object, args)).
                                            around((performer, operation, object, args) -> "AROUND 4: " + AroundAdvice.applyDefault(performer, operation, object, args)).
                                            around((performer, operation, object, args) -> "AROUND 5: " + AroundAdvice.applyDefault(performer, operation, object, args));
                                }).
                build();

        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };


        List<String> out = new ArrayList<>();
        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            out.add(extension.toString());
        }
        String outStr = String.join("\n", out);
        System.out.println(outStr);
        assertEquals("""
                     AROUND 1: AROUND 2: AROUND 3: AROUND 4: AROUND 5: Book["The Mythical Man-Month"]
                     AROUND 1: AROUND 2: AROUND 3: AROUND 4: AROUND 5: Furniture["Sofa"]
                     AROUND 1: AROUND 2: AROUND 3: AROUND 4: AROUND 5: ElectronicItem["Soundbar"]
                     AROUND 1: AROUND 2: AROUND 3: AROUND 4: AROUND 5: AutoPart["Tire"]""", outStr);
    }

    @Test
    void removeAspectTest() {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%n");
        final String LOG_BEFORE = "logBefore";

        StringBuilderHandler stringBuilderHandler = new StringBuilderHandler();
        Logger logger = Logger.getLogger(getClass().getName());
        logger.addHandler(stringBuilderHandler);
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
                aspectBuilder().
                    extensionInterface(ItemInterface.class).
                        objectClass(Item.class).
                            operation("*").
                                before(new LogBeforeAdvice(logger)).
                                before(new LogBeforeAdvice(logger), LOG_BEFORE).
                                after(new LogAfterAdvice(logger)).
                build();

        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            System.out.println(extension.toString());
        }

        assertEquals("""
                    INFO: BEFORE: Book["The Mythical Man-Month"] -> toString()
                    INFO: BEFORE: Book["The Mythical Man-Month"] -> toString()
                    INFO: AFTER: Book["The Mythical Man-Month"] -> toString() = Book["The Mythical Man-Month"]
                    INFO: BEFORE: Furniture["Sofa"] -> toString()
                    INFO: BEFORE: Furniture["Sofa"] -> toString()
                    INFO: AFTER: Furniture["Sofa"] -> toString() = Furniture["Sofa"]
                    INFO: BEFORE: ElectronicItem["Soundbar"] -> toString()
                    INFO: BEFORE: ElectronicItem["Soundbar"] -> toString()
                    INFO: AFTER: ElectronicItem["Soundbar"] -> toString() = ElectronicItem["Soundbar"]
                    INFO: BEFORE: AutoPart["Tire"] -> toString()
                    INFO: BEFORE: AutoPart["Tire"] -> toString()
                    INFO: AFTER: AutoPart["Tire"] -> toString() = AutoPart["Tire"]
                    """, stringBuilderHandler.getStringBuilder().toString());

        // test removal before advices with no ID
        dynamicClassExtension.setVerbose(true);
        stringBuilderHandler.getStringBuilder().setLength(0);

        dynamicClassExtension.aspectBuilder().
                    extensionInterface(ItemInterface.class).
                        objectClass(Item.class).
                            operation("*").
                                remove(AdviceType.BEFORE).
                build();

        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            System.out.println(extension.toString());
        }
        assertEquals("""
                    INFO: BEFORE: Book["The Mythical Man-Month"] -> toString()
                    INFO: AFTER: Book["The Mythical Man-Month"] -> toString() = Book["The Mythical Man-Month"]
                    INFO: BEFORE: Furniture["Sofa"] -> toString()
                    INFO: AFTER: Furniture["Sofa"] -> toString() = Furniture["Sofa"]
                    INFO: BEFORE: ElectronicItem["Soundbar"] -> toString()
                    INFO: AFTER: ElectronicItem["Soundbar"] -> toString() = ElectronicItem["Soundbar"]
                    INFO: BEFORE: AutoPart["Tire"] -> toString()
                    INFO: AFTER: AutoPart["Tire"] -> toString() = AutoPart["Tire"]
                    """, stringBuilderHandler.getStringBuilder().toString());

        // test removal before advices with non-existent ID
        stringBuilderHandler.getStringBuilder().setLength(0);

        dynamicClassExtension.aspectBuilder().
                extensionInterface(ItemInterface.class).
                    objectClass(Item.class).
                        operation("*").
                            remove("fakeID", AdviceType.BEFORE).
                build();

        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            System.out.println(extension.toString());
        }
        assertEquals("""
                    INFO: BEFORE: Book["The Mythical Man-Month"] -> toString()
                    INFO: AFTER: Book["The Mythical Man-Month"] -> toString() = Book["The Mythical Man-Month"]
                    INFO: BEFORE: Furniture["Sofa"] -> toString()
                    INFO: AFTER: Furniture["Sofa"] -> toString() = Furniture["Sofa"]
                    INFO: BEFORE: ElectronicItem["Soundbar"] -> toString()
                    INFO: AFTER: ElectronicItem["Soundbar"] -> toString() = ElectronicItem["Soundbar"]
                    INFO: BEFORE: AutoPart["Tire"] -> toString()
                    INFO: AFTER: AutoPart["Tire"] -> toString() = AutoPart["Tire"]
                    """, stringBuilderHandler.getStringBuilder().toString());

        // test removal before advices with ID
        stringBuilderHandler.getStringBuilder().setLength(0);

        dynamicClassExtension.aspectBuilder().
                extensionInterface(ItemInterface.class).
                    objectClass(Item.class).
                        operation("*").
                            remove(LOG_BEFORE, AdviceType.BEFORE).
                build();

        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            System.out.println(extension.toString());
        }
        assertEquals("""
                    INFO: AFTER: Book["The Mythical Man-Month"] -> toString() = Book["The Mythical Man-Month"]
                    INFO: AFTER: Furniture["Sofa"] -> toString() = Furniture["Sofa"]
                    INFO: AFTER: ElectronicItem["Soundbar"] -> toString() = ElectronicItem["Soundbar"]
                    INFO: AFTER: AutoPart["Tire"] -> toString() = AutoPart["Tire"]
                    """, stringBuilderHandler.getStringBuilder().toString());
    }

    @Test
    void enableAspectTest() {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%n");

        StringBuilderHandler stringBuilderHandler = new StringBuilderHandler();
        Logger logger = Logger.getLogger(getClass().getName());
        logger.addHandler(stringBuilderHandler);
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
                aspectBuilder().
                    extensionInterface(ItemInterface.class).
                        objectClass(Item.class).
                        operation("*").
                            before(new LogBeforeAdvice(logger)).
                            before(new LogBeforeAdvice(logger)).
                            after(new LogAfterAdvice(logger)).
                build();

        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            System.out.println(extension.toString());
        }

        assertEquals("""
                     INFO: BEFORE: Book["The Mythical Man-Month"] -> toString()
                     INFO: BEFORE: Book["The Mythical Man-Month"] -> toString()
                     INFO: AFTER: Book["The Mythical Man-Month"] -> toString() = Book["The Mythical Man-Month"]
                     INFO: BEFORE: Furniture["Sofa"] -> toString()
                     INFO: BEFORE: Furniture["Sofa"] -> toString()
                     INFO: AFTER: Furniture["Sofa"] -> toString() = Furniture["Sofa"]
                     INFO: BEFORE: ElectronicItem["Soundbar"] -> toString()
                     INFO: BEFORE: ElectronicItem["Soundbar"] -> toString()
                     INFO: AFTER: ElectronicItem["Soundbar"] -> toString() = ElectronicItem["Soundbar"]
                     INFO: BEFORE: AutoPart["Tire"] -> toString()
                     INFO: BEFORE: AutoPart["Tire"] -> toString()
                     INFO: AFTER: AutoPart["Tire"] -> toString() = AutoPart["Tire"]
                     """, stringBuilderHandler.getStringBuilder().toString());

        // test disabled
        dynamicClassExtension.setVerbose(true);
        stringBuilderHandler.getStringBuilder().setLength(0);
        dynamicClassExtension.aspectBuilder().
                extensionInterface(ItemInterface.class).
                    objectClass(Item.class).
                        operation("*").
                            enabled(false, AdviceType.BEFORE, AdviceType.AFTER).
                build();
        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            System.out.println(extension.toString());
        }
        assertEquals("", stringBuilderHandler.getStringBuilder().toString());
    }

    @Test
    void multipleInterfacesTest() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
                builder(Shippable.class).
                operationName("ship").
                operation(Item.class, o -> new ShippingInfo("SHIPPED: " + o.toString())).
                build().
                builder(ClassExtension.IdentityHolder.class).
                operationName("getID").
                operation(Object.class, aO -> "ID: " + aO.toString()).
                build();
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        List<String> out = new ArrayList<>();
        for (Item item : items) {
            Shippable extension = dynamicClassExtension.extension(item,
                    Shippable.class,
                    ItemInterface.class, ClassExtension.IdentityHolder.class);
            out.add(extension.ship().toString());
            out.add(((ClassExtension.IdentityHolder) extension).getID().toString());
            out.add(((ItemInterface) extension).getName());
        }
        String result = String.join("\n", out);
        System.out.println(result);
        assertEquals("""
                                ShippingInfo[result=SHIPPED: Book["The Mythical Man-Month"]]
                                ID: Book["The Mythical Man-Month"]
                                The Mythical Man-Month
                                ShippingInfo[result=SHIPPED: Furniture["Sofa"]]
                                ID: Furniture["Sofa"]
                                Sofa
                                ShippingInfo[result=SHIPPED: ElectronicItem["Soundbar"]]
                                ID: ElectronicItem["Soundbar"]
                                Soundbar
                                ShippingInfo[result=SHIPPED: AutoPart["Tire"]]
                                ID: AutoPart["Tire"]
                                Tire""", result);
    }

    @Test
    void lambdaWithDescriptionAndIDTest() {
        Runnable r1 = DynamicClassExtension.lambdaWithDescriptionAndID((Runnable) () -> out.println("R"),"R1", "r1");
        assertEquals("r1", r1 instanceof DynamicClassExtension.IdentityHolder id2 ? id2.getID() : null);

        Runnable r2 = DynamicClassExtension.lambdaWithDescriptionAndID((Runnable) () -> out.println("R"), "R1", "r1");
        assertEquals("r1", r1 instanceof DynamicClassExtension.IdentityHolder id2 ? id2.getID() : null);

        Runnable r3 = DynamicClassExtension.lambdaWithDescriptionAndID((Runnable) () -> out.println("R3"),"R3", "r3");
        assertEquals("r3", r3 instanceof DynamicClassExtension.IdentityHolder id3 ? id3.getID() : null);

        assertEquals("R1", r1.toString());
        assertEquals("R1", r2.toString());
        assertEquals("R3", r3.toString());

        assertEquals(r1, r2);
        assertEquals(r2, r1);

        assertNotEquals(r1, r3);
        assertNotEquals(r3, r1);
    }

    @Test
    void cachedAndLogPerformAroundAdviceTest() {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%n");

        CachedValueProvider cache = new CachedValueProvider() {
            private final Map<Object, Object> cache =new HashMap<>();
            @Override
            public Object getOrCreate(OperationPerformKey key, Supplier<?> aValueSupplier) {
                Object result = cache.get(key.object());
                if (result == null) {
                    result = aValueSupplier.get();
                    cache.put(key.object(), result);
                }
                return result;
            }
        };

        StringBuilderHandler stringBuilderHandler = new StringBuilderHandler();
        Logger logger = Logger.getLogger(getClass().getName());
        logger.addHandler(stringBuilderHandler);

        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().aspectBuilder().
                    extensionInterface(ItemInterface.class).
                        objectClass(Item.class).
                            operation("*").
                                advices(new Advice[] {
                                        new CachedValueAdvice(cache, logger),
                                        new LogPerformTimeAdvice(logger, "Perform time for \"{0}\" is 0 ms.") // ignoring actual times for a test
                                }).
                build();

        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            System.out.println(extension.toString());
        }

        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            System.out.println(extension.toString());
        }

        out.println(stringBuilderHandler.getStringBuilder().toString());
        assertEquals("""
                            INFO: Not cached; getting value: Book["The Mythical Man-Month"] -> toString()
                            INFO: Perform time for "toString" is 0 ms.
                            INFO: Not cached; getting value: Furniture["Sofa"] -> toString()
                            INFO: Perform time for "toString" is 0 ms.
                            INFO: Not cached; getting value: ElectronicItem["Soundbar"] -> toString()
                            INFO: Perform time for "toString" is 0 ms.
                            INFO: Not cached; getting value: AutoPart["Tire"] -> toString()
                            INFO: Perform time for "toString" is 0 ms.
                            """, stringBuilderHandler.getStringBuilder().toString());
    }

    private interface NamesHolder {
        List<String> getNames();
    }

    private static class Names implements NamesHolder {
        private final List<String> names = new ArrayList<>();
        @Override
        public List<String> getNames() {
            return names;
        }
    }

    @Test
    void readOnlyCollectionsAdviceTest() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
                aspectBuilder().
                extensionInterface(NamesHolder.class).
                    objectClass(Names.class).
                        operation("getNames()").
                            around(new ReadOnlyCollectionOrMapAdvice()).
                build();
        Names names = new Names();
        try {
            NamesHolder namesHolder = dynamicClassExtension.extension(names, NamesHolder.class);
            namesHolder.getNames().add("test"); // cant add because list is read-only
            fail("Unexpected success on list modification");
        } catch (UnsupportedOperationException ex) {
            // do nothing
        }
    }

    static int circuitBreakfastedAttemptsCount = 0;

    @Test
    void circuitBreakerAdviceTest() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
                aspectBuilder().
                    extensionInterface(Shippable.class).
                    objectClass(Book.class).
                        operation("ship").
                            around(new CircuitBreakerAdvice(3, Duration.ofSeconds(5))).
                build().
                builder(Shippable.class).
                    objectClass(Book.class).
                        operation("ship", (Book book) -> {
                            if (circuitBreakfastedAttemptsCount++ > 2)
                                return new ShippingInfo("SHIPPED: " + book.toString());
                            else
                                throw new RuntimeException("Failed to ship " + book.toString());
                        }).
                build();

        int succeedCount = 0;
        Shippable shippable = dynamicClassExtension.extension(new Book("The Mythical Man-Month"), Shippable.class);
        for (int i = 0; i < 10; i++) {
            try {
                if (i == 5)
                    Thread.sleep(6000);
                out.println(shippable.ship());
                succeedCount++;
            } catch (Exception aE) {
                out.println(aE.getMessage());
            }
        }
        assertEquals(5, succeedCount);
    }

    interface MultipleParameters {
        String[] arrayParameter(String[] anArray);
        Object[] multipleParameters(int p1, String p2, String p3);
    }

    @Test
    void testRateLimitedAdvice() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().aspectBuilder().
                    extensionInterface(Shippable.class).
                        objectClass(Book.class).
                            operation("track(*)").
                                around(new RateLimitedAdvice(5, Duration.ofSeconds(1))).
                build().
                    builder(Shippable.class).
                        objectClass(Book.class).
                            operation("track", (Book book, Boolean isVerbose) -> {
                                return new TrackingInfo("Delivered: " + book.toString());
                            }).
                build();

        int succeedCount = 0;
        Shippable shippable = dynamicClassExtension.extension(new Book("The Mythical Man-Month"), Shippable.class);
        try {
            for (int i = 0; i < 7; i++) {
                shippable.track(true);
                succeedCount++;
            }
        } catch (Exception ex) {
            out.println(ex.getMessage());
        }
        assertEquals(5, succeedCount);

        sleep(500);
        try {
            shippable.track(false);
            fail("Unexpected success on rate limited operation");
        } catch (Exception ex) {
            out.println(ex.getMessage());
        }

        sleep(1001);
        try {
            shippable.track(false);
        } catch (Exception ex) {
            out.println(ex.getMessage());
            fail("Unexpected failure on rate limited operation");
        }
    }

    @Test
    void TestDynamicQuotaAdvice() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
                aspectBuilder().
                extensionInterface(Shippable.class).
                objectClass(Book.class).
                operation("track(*)").
                around(new DynamicQuotaAdvice(new QuotaHandler() {
                    long quota = 20;
                    @Override
                    public long getQuota(String anOperation, Object anObject, Object[] anArgs) {
                        return quota;
                    }

                    @Override
                    public void decreaseQuota(long anAmount, String operation, Object object, Object[] args) {
                        quota -= anAmount;
                    }

                    @Override
                    public long calculateOperationCost(String operation, Object object, Object[] args) {
                        return 2;
                    }

                    @Override
                    public long calculateOperationResultCost(String anOperation, Object aResult, Object anObject, Object[] anArgs) {
                        return 1;
                    }
                }, Logger.getLogger(getClass().getName()))).
                build().
                builder(Shippable.class).
                objectClass(Book.class).
                operation("track", (Book book, Boolean isVerbose) -> {
                    return new TrackingInfo("Delivered: " + book.toString());
                }).
                build();

        int succeedCount = 0;
        Shippable shippable = dynamicClassExtension.extension(new Book("The Mythical Man-Month"), Shippable.class);
        try {
            for (int i = 0; i < 10; i++) {
                shippable.track(true);
                succeedCount++;
            }
            assertEquals(7, succeedCount);
        } catch (Exception ex) {
            out.println(ex.getMessage());
        }
    }

    @Test
    void multipleParametersOperationTest() {
        DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(MultipleParameters.class).
                operationName("arrayParameter").
                    operation(Object.class, (Object a1, String[] a2) -> a2).
                operationName("multipleParameters").
                    operation(Object.class, (Object a1, Object[] a2) -> a2).
                build();
        MultipleParameters extension = dynamicClassExtension.extension(new Object(), MultipleParameters.class);

        String result = Arrays.toString(extension.arrayParameter(new String[]{"1", "2", "3"}));
        out.println(result);
        assertEquals("[1, 2, 3]", result);

        result = Arrays.toString(extension.multipleParameters(1, "2", "3"));
        out.println(result);
        assertEquals("[1, 2, 3]", result);
    }

    @Test
    void nullTest() {
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension(null);
        Item_Shippable extension = dynamicClassExtension.extension(null, Item_Shippable.class);
        ShippingInfo result = extension.ship();
        out.println(result);
        assertEquals("ShippingInfo[result=Nothing to ship]", result.toString());
    }

    private static final StringBuilder sharedShippingLog = new StringBuilder();

    static {
        setupDynamicClassExtension(DynamicClassExtension.sharedInstance(), sharedShippingLog);
    }

    static class Jewelery extends Item {
        public Jewelery(String aName) {
            super(aName);
        }
    }

    @Test
    void testAdhocNewItem() {
        DynamicClassExtension dynamicClassExtension = DynamicClassExtension.sharedInstance().builder().
                extensionInterface(Item_Shippable.class).
                    operationName("ship").
                        operation(Jewelery.class, jewelery -> new ShippingInfo(STR."\{jewelery.getName()} jewelery shipped")).
                build();

        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
                new Jewelery("Diamond ring"),
        };

        List<String> shippingLog = new ArrayList<>();

        for (Item item : items) {
            Item_Shippable extension = dynamicClassExtension.extension(item, Item_Shippable.class);
            ShippingInfo shippingInfo = extension.ship();
            shippingLog.add(shippingInfo.toString());
            System.out.println(shippingInfo);
        }

        assertEquals("""
                     ShippingInfo[result=The Mythical Man-Month book shipped]
                     ShippingInfo[result=Sofa furniture shipped]
                     ShippingInfo[result=Soundbar electronic item shipped]
                     ShippingInfo[result=Tire item NOT shipped]
                     ShippingInfo[result=Diamond ring jewelery shipped]""", String.join("\n", shippingLog));
    }

    private static void sleep() {
        sleep(1000);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private static class StringBuilderHandler extends Handler {
        public StringBuilder getStringBuilder() {
            return stringBuilder;
        }

        private final StringBuilder stringBuilder = new StringBuilder();

        public StringBuilderHandler() {
            setFormatter(new SimpleFormatter());
        }

        @Override
        public void publish(LogRecord record) {
            stringBuilder.append(getFormatter().format(record));
        }

        @Override
        public void flush() {}

        @Override
        public void close() throws SecurityException {}
    }

//    interface Persistable {
//        void load();
//        void load(String aFileName);
//        void save();
//        void save(String aFileName);
//    }
//
//    @Test
//    void testPersistable() {
//        initialize an instance of `DynamicClassExtension`for all methods of `Persistable` interface and for the following classes: `Item`, `Book`, `Furniture`, `AutoPart`
//    }
}


