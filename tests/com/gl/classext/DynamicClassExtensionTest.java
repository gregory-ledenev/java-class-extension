package com.gl.classext;


import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

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
            return getName();
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

    interface Item_Shippable {
        ShippingInfo ship();
        TrackingInfo track(boolean isVerbose);
        TrackingInfo track();
        void log(boolean isVerbose);
        void log();
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

        setupDynamicClassExtension(shippingLog);
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        StringBuilder shippingInfos = new StringBuilder();
        for (Item item : items) {
            Item_Shippable extension = DynamicClassExtension.sharedInstance().extension(item, Item_Shippable.class);
            extension.log();
            if (!shippingInfos.isEmpty())
                shippingInfos.append("\n");
            shippingInfos.append(extension.ship());
            shippingInfos.append("\n").append(extension.track());
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
    void verboseOperationUsingBuilderTest() {

        StringBuilder shippingLog = new StringBuilder();

        setupDynamicClassExtension(shippingLog);
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        StringBuilder shippingInfos = new StringBuilder();
        for (Item item : items) {
            Item_Shippable extension = DynamicClassExtension.sharedInstance().extension(item, Item_Shippable.class);
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

    private static void setupDynamicClassExtension(StringBuilder shippingLog) {
        DynamicClassExtension.sharedBuilder(Item_Shippable.class).
                name("ship").
                    op(Item.class, book -> new ShippingInfo(book.getName() + " item NOT shipped")).
                    op(Book.class, book -> new ShippingInfo(book.getName() + " book shipped")).
                    op(Furniture.class, furniture -> new ShippingInfo(furniture.getName() + " furniture shipped")).
                    op(ElectronicItem.class, electronicItem -> new ShippingInfo(electronicItem.getName() + " electronic item shipped")).
                name("log").
                    voidOp(Item.class, (Item item, Boolean isVerbose) -> {
                        if (! shippingLog.isEmpty())
                            shippingLog.append("\n");
                        shippingLog.append(item.getName()).append(" is about to be shipped in 1 hour");
                    }).
                    voidOp(Item.class, item -> {
                        if (! shippingLog.isEmpty())
                            shippingLog.append("\n");
                        shippingLog.append(item.getName()).append(" is about to be shipped");
                    }).
                name("track").
                    op(Item.class, item -> new TrackingInfo(item.getName() + " item on its way")).
                    op(Item.class, (Item item, Boolean isVerbose) -> new TrackingInfo(item.getName() +
                            " item on its way" + (isVerbose ? "Status: SHIPPED" : "")));
    }

    /**
     * Test for cached extension
     */
    @Test
    void cacheTest() {
        setupDynamicClassExtension(new StringBuilder());
        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = DynamicClassExtension.sharedInstance().extension(book, Item_Shippable.class);
        assertSame(extension, DynamicClassExtension.sharedInstance().extension(book, Item_Shippable.class));
    }

    /**
     * Test for cached extension
     */
    @SuppressWarnings("unchecked")
    @Test
    void cacheEntryRemovalTest() {
        setupDynamicClassExtension(new StringBuilder());
        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = DynamicClassExtension.sharedInstance().extension(book, Item_Shippable.class);
        assertSame(extension, DynamicClassExtension.sharedInstance().extension(book, Item_Shippable.class));
        DynamicClassExtension.sharedInstance().extensionCache.remove(book);
        assertNotSame(extension, DynamicClassExtension.sharedInstance().extension(book, Item_Shippable.class));
    }

    /**
     * Test for not cached extension
     */
    @Test
    void nonCacheTest() {
        setupDynamicClassExtension(new StringBuilder());
        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = DynamicClassExtension.sharedInstance().extensionNoCache(book, Item_Shippable.class);
        assertNotSame(extension, DynamicClassExtension.sharedInstance().extensionNoCache(book, Item_Shippable.class));
    }

    /**
     * Tests for cached extension release by GC and cleaned manually
     */
    @Test
    void cacheCleanupTest() {
        setupDynamicClassExtension(new StringBuilder());
        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = DynamicClassExtension.sharedInstance().extension(book, Item_Shippable.class);

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> future = executor.schedule(() -> {
            DynamicClassExtension.sharedInstance().extension(book, Item_Shippable.class);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException aE) {
                // do nothing
            }
        }, 5, TimeUnit.SECONDS);
        try {
            future.get();
            System.gc();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
        assertNotSame(extension, DynamicClassExtension.sharedInstance().extension(book, Item_Shippable.class));
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
        setupDynamicClassExtension(new StringBuilder());
        Book book = new Book("The Mythical Man-Month");
        Item_Shippable extension = DynamicClassExtension.sharedInstance().extension(book, Item_Shippable.class);
        assertSame(extension, DynamicClassExtension.sharedInstance().extension(book, Item_Shippable.class));
        DynamicClassExtension.sharedInstance().cacheClear();
        assertTrue(ClassExtension.cacheIsEmpty());
        assertNotSame(extension, DynamicClassExtension.sharedInstance().extension(book, Item_Shippable.class));
    }

    /**
     * Tests for cached extension release by GC
     */
    @Test
    void scheduledCleanupCacheTest() {
        setupDynamicClassExtension(new StringBuilder());
        Book book = new Book("The Mythical Man-Month");

        DynamicClassExtension.sharedInstance().scheduleCacheCleanup();
        try {
            DynamicClassExtension.sharedInstance().extension(book, Item_Shippable.class);
            System.gc();
            assertFalse(DynamicClassExtension.sharedInstance().cacheIsEmpty());
            try {
                System.out.println("Waiting 1.5 minutes for automatic cache cleanup...");
                Thread.sleep(90000);
            } catch (InterruptedException aE) {
                // do nothing
            }
            assertTrue(DynamicClassExtension.sharedInstance().cacheIsEmpty());
        } finally {
            DynamicClassExtension.sharedInstance().shutdownCacheCleanup();
        }
    }
}

