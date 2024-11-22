package com.gl.classext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Item {
    private final String name;

    public Item(String aName) {
        name = aName;
    }

    @Override
    public String toString() {
        return getName();
    }

    public String getName() {
        return name;
    }
}

class Book extends Item {
    public Book(String aName) {
        super(aName);
    }
}

class Furniture extends Item {
    public Furniture(String aName) {
        super(aName);
    }
}

class ElectronicItem extends Item {
    public ElectronicItem(String aName) {
        super(aName);
    }
}

class AutoPart extends Item {
    public AutoPart(String aName) {
        super(aName);
    }
}

record ShippingInfo(String result) {
}

class Item_Shippable implements ClassExtension.DelegateHolder<Item> {
    public static Item_Shippable extensionFor(Item anItem) {
        return ClassExtension.extension(anItem, Item_Shippable.class);
    }

    public ShippingInfo ship() {
        return new ShippingInfo(getDelegate() + " NOT shipped");
    }

    private Item delegate;

    @Override
    public Item getDelegate() {
        return delegate;
    }

    @Override
    public void setDelegate(Item aDelegate) {
        delegate = aDelegate;
    }
}

@suppress("unused")
class Book_Shippable extends Item_Shippable {
    public ShippingInfo ship() {
        return new ShippingInfo(getDelegate() + " shipped");
    }
}

@suppress("unused")
class Furniture_Shippable extends Item_Shippable {
    public ShippingInfo ship() {
        return new ShippingInfo(getDelegate() + " shipped");
    }
}

@suppress("unused")
class ElectronicItem_Shippable extends Item_Shippable {
    public ShippingInfo ship() {
        return new ShippingInfo(getDelegate() + " shipped");
    }
}

public class ClassExtensionTest {
    /**
     * Tests for exact match when a matching extension is defined for the passed object's class
     */
    @Test
    void shipmentTest() {
        Item[] items = {
                new Book("book"),
                new Furniture("furniture"),
                new ElectronicItem("electronic item")
        };

        StringBuilder shippingInfos = new StringBuilder();
        for (Item item : items) {
            ShippingInfo shippingInfo = ship(item);
            if (!shippingInfos.isEmpty())
                shippingInfos.append("\n");
            shippingInfos.append(shippingInfo);
            System.out.println(shippingInfo);
        }
        assertEquals("""
                     ShippingInfo[result=book shipped]
                     ShippingInfo[result=furniture shipped]
                     ShippingInfo[result=electronic item shipped]""",
                shippingInfos.toString());
    }

    ShippingInfo ship(Item anItem) {
        return Item_Shippable.extensionFor(anItem).ship();
    }

    /**
     * Tests for partial match when only a super class extension is defined for the passed object's class
     */
    @Test
    void partialMatchShipmentTest() {
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        StringBuilder shippingInfos = new StringBuilder();
        for (Item item : items) {
            ShippingInfo shippingInfo = ship(item);
            if (!shippingInfos.isEmpty())
                shippingInfos.append("\n");
            shippingInfos.append(shippingInfo);
            System.out.println(shippingInfo);
        }
        assertEquals("""
                     ShippingInfo[result=The Mythical Man-Month shipped]
                     ShippingInfo[result=Sofa shipped]
                     ShippingInfo[result=Soundbar shipped]
                     ShippingInfo[result=Tire NOT shipped]""",
                shippingInfos.toString());
    }

    /**
     * Tests for missing extension
     */
    @Test
    @SuppressWarnings({"rawtypes"})
    void noShippableClassFoundTest() {
        try {
            ClassExtension.DelegateHolder extension = ClassExtension.extension(new Book("noname"), ClassExtension.DelegateHolder.class);
            fail("Unexpected extension found: " + extension);
        } catch (Exception aE) {
            System.out.println(aE.toString());
        }
    }

    /**
     * Test for cached extension
     */
    @Test
    void cacheTest() {
        Book book = new Book("");
        String extension = Item_Shippable.extensionFor(book).toString();
        assertEquals(extension, Item_Shippable.extensionFor(book).toString());
    }

    /**
     * Tests for cached extension release by GC and cleaned manually
     */
    @Test
    void cacheCleanupTest() {
        Book book = new Book("");
        String extension = Item_Shippable.extensionFor(book).toString();
        assertEquals(extension, Item_Shippable.extensionFor(book).toString());
        System.gc();
        assertNotEquals(extension, Item_Shippable.extensionFor(book).toString());
        System.gc();
        assertFalse(ClassExtension.cacheIsEmpty());
        ClassExtension.cacheCleanup();
        assertTrue(ClassExtension.cacheIsEmpty());
    }

    /**
     * Tests for cached extension cleared
     */
    @Test
    void cacheClearTest() {
        Book book = new Book("");
        String extension = Item_Shippable.extensionFor(book).toString();
        assertEquals(extension, Item_Shippable.extensionFor(book).toString());
        ClassExtension.cacheClear();
        assertTrue(ClassExtension.cacheIsEmpty());
        assertNotEquals(extension, Item_Shippable.extensionFor(book).toString());
    }

    /**
     * Tests for cached extension release by GC
     */
    @Test
    void scheduledCleanupCacheTest() {
        ClassExtension.scheduleCacheCleanup();
        try {
            Book book = new Book("");
            Item_Shippable.extensionFor(book);
            System.gc();
            assertFalse(ClassExtension.cacheIsEmpty());
            try {
                System.out.println("Waiting 1.5 minutes for automatic cache cleanup...");
                Thread.sleep(90000);
            } catch (InterruptedException aE) {
                // do nothing
            }
            assertTrue(ClassExtension.cacheIsEmpty());
        } finally {
            ClassExtension.shutdownCacheCleanup();
        }
    }
}