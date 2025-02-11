package com.gl.classext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StaticClassExtensionInnerClassesTest {
    public static class Item {
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

    public static class Book extends Item {
        public Book(String aName) {
            super(aName);
        }
    }

    public static class Furniture extends Item {
        public Furniture(String aName) {
            super(aName);
        }
    }

    public static class ElectronicItem extends Item {
        public ElectronicItem(String aName) {
            super(aName);
        }
    }

    public static class AutoPart extends Item {
        public AutoPart(String aName) {
            super(aName);
        }
    }

    public record ShippingInfo(String result) {
    }

    @ExtensionInterface
    public interface Shippable {
        public ShippingInfo ship();

        public static Shippable extensionFor(Item anItem) {
            return StaticClassExtension.sharedExtension(anItem, Shippable.class);
        }
    }

    public static class ItemShippable implements Shippable, StaticClassExtension.DelegateHolder<Item> {
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

    @SuppressWarnings("unused")
    static class BookShippable extends ItemShippable {
        public ShippingInfo ship() {
            return new ShippingInfo(getDelegate() + " shipped");
        }
    }

    @SuppressWarnings("unused")
    static class FurnitureShippable extends ItemShippable {
        public ShippingInfo ship() {
            return new ShippingInfo(getDelegate() + " shipped");
        }
    }

    @SuppressWarnings("unused")
    static class ElectronicItemShippable extends ItemShippable {
        public ShippingInfo ship() {
            return new ShippingInfo(getDelegate() + " shipped");
        }
    }

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

    public ShippingInfo ship(Item anItem) {
        return Shippable.extensionFor(anItem).ship();
    }

    /**
     * Tests for cached extension release by GC
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
}