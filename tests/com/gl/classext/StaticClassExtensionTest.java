package com.gl.classext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

interface ItemInterface {
    String getName();
}

class Item implements ItemInterface {
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

@ExtensionInterface
interface Shippable {
    ShippingInfo ship();
    void log();

    static Shippable extensionFor(Item anItem) {
        return StaticClassExtension.sharedExtension(anItem, Shippable.class);
    }
}

interface ShippableItemInterface extends Shippable, ItemInterface {
}

class ItemShippable implements Shippable, StaticClassExtension.DelegateHolder<Item> {
    static final StringBuilder LOG = new StringBuilder();

    public ShippingInfo ship() {
        return new ShippingInfo(getDelegate() + " NOT shipped");
    }

    public void log() {
        if (!LOG.isEmpty())
            LOG.append("\n");
        LOG.append(delegate.getName()).append(" is about to be shipped");
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

@SuppressWarnings("unused")
class BookShippable extends ItemShippable {
    public ShippingInfo ship() {
        return new ShippingInfo(getDelegate() + " shipped");
    }
}

@SuppressWarnings("unused")
class FurnitureShippable extends ItemShippable {
    public ShippingInfo ship() {
        return new ShippingInfo(getDelegate() + " shipped");
    }
}

@SuppressWarnings("unused")
class ElectronicItemShippable extends ItemShippable {
    public ShippingInfo ship() {
        return new ShippingInfo(getDelegate() + " shipped");
    }
}

public class StaticClassExtensionTest {
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
            ShippableItemInterface shippable = StaticClassExtension.sharedExtension(item, ShippableItemInterface.class);
            ShippingInfo shippingInfo = shippable.ship();
            assertEquals(item.getName(), shippable.getName());
            assertEquals(item.hashCode(), shippable.hashCode());
            assertEquals(item.toString(), shippable.toString());
            assertEquals((Object) shippable, item);

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
        Shippable shippable = Shippable.extensionFor(anItem);
//        System.out.println(shippable.getName());
        return shippable.ship();
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
            StaticClassExtension.DelegateHolder extension = StaticClassExtension.sharedExtension(new Book("noname"), StaticClassExtension.DelegateHolder.class);
            fail("Unexpected extension found: " + extension);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Test for cached extension
     */
    @Test
    void cacheTest() {
        Book book = new Book("");
        Shippable extension = Shippable.extensionFor(book);
        assertSame(extension, Shippable.extensionFor(book));

        StaticClassExtension.sharedInstance().setCacheEnabled(false);
        try {
            extension = Shippable.extensionFor(book);
            assertNotSame(extension, Shippable.extensionFor(book));

            StaticClassExtension.sharedInstance().setCacheEnabled(true);
            extension = Shippable.extensionFor(book);
            assertSame(extension, Shippable.extensionFor(book));
        } finally {
            StaticClassExtension.sharedInstance().setCacheEnabled(true);
        }
    }

    /**
     * Test for cached extension
     */
    @SuppressWarnings("unchecked")
    @Test
    void cacheEntryRemovalTest() {
        Book book = new Book("");
        Shippable extension = Shippable.extensionFor(book);
        assertSame(extension, Shippable.extensionFor(book));
        StaticClassExtension.sharedInstance().extensionCache.remove(book);
        assertNotSame(extension, Shippable.extensionFor(book));
    }

    /**
     * Test for not cached extension
     */
    @Test
    void nonCacheTest() {
        Book book = new Book("Shining");
        Shippable extension = StaticClassExtension.sharedExtensionNoCache(book, Shippable.class);
        Shippable actual = StaticClassExtension.sharedExtensionNoCache(book, Shippable.class);
        assertNotSame(extension, actual);
    }

    /**
     * Tests for cached extension release by GC and cleaned manually
     */
    @Test
    void cacheCleanupTest() {
        Book book = new Book("");
        Shippable extension = Shippable.extensionFor(book);
        assertSame(extension, Shippable.extensionFor(book));

        assertFalse(StaticClassExtension.sharedInstance().cacheIsEmpty());
        StaticClassExtension.sharedInstance().cacheCleanup();
        assertFalse(StaticClassExtension.sharedInstance().cacheIsEmpty());
    }

    /**
     * Tests for cached extension cleared
     */
    @Test
    void cacheClearTest() {
        Book book = new Book("");
        Shippable extension = Shippable.extensionFor(book);
        assertSame(extension, Shippable.extensionFor(book));
        StaticClassExtension.sharedInstance().cacheClear();
        assertTrue(StaticClassExtension.sharedInstance().cacheIsEmpty());
        assertNotSame(extension, Shippable.extensionFor(book));
    }

    /**
     * Tests for cached extension release by GC
     */
    @Test
    void scheduledCleanupCacheTest() {
        StaticClassExtension.sharedInstance().scheduleCacheCleanup();
        try {
            Book book = new Book("");
            Shippable.extensionFor(book);
            System.gc();
            assertFalse(StaticClassExtension.sharedInstance().cacheIsEmpty());
            try {
                System.out.println("Waiting 1.5 minutes for automatic cache cleanup...");
                Thread.sleep(90000);
            } catch (InterruptedException aE) {
                // do nothing
            }
            assertTrue(StaticClassExtension.sharedInstance().cacheIsEmpty());
        } finally {
            StaticClassExtension.sharedInstance().shutdownCacheCleanup();
        }
    }

    @Test
    void performanceTest() {
        performanceTestStatic();
    }

    public static void performanceTestStatic() {
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++) {
            for (Item item : items) {
                ShippableItemInterface extension = StaticClassExtension.sharedInstance().extension(item, ShippableItemInterface.class);
                extension.log();
                extension.getName();
            }
        }
        System.out.println("STATIC - Elapsed time: " + ((System.currentTimeMillis()-startTime) / 1000f));
    }
}