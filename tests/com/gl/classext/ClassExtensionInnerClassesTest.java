package com.gl.classext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ClassExtensionInnerClassesTest {
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

    public static class Item_Shippable implements ClassExtension.DelegateHolder<Item> {
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

    @SuppressWarnings("unused")
    static class Book_Shippable extends Item_Shippable {
        public ShippingInfo ship() {
            return new ShippingInfo(getDelegate() + " shipped");
        }
    }

    @SuppressWarnings("unused")
    static class Furniture_Shippable extends Item_Shippable {
        public ShippingInfo ship() {
            return new ShippingInfo(getDelegate() + " shipped");
        }
    }

    @SuppressWarnings("unused")
    static class ElectronicItem_Shippable extends Item_Shippable {
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
            ClassExtension.DelegateHolder extension = ClassExtension.extension(new Book("noname"), ClassExtension.DelegateHolder.class);
            fail("Unexpected extension found: " + extension);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public ShippingInfo ship(Item anItem) {
        return Item_Shippable.extensionFor(anItem).ship();
    }

    /**
     * Tests for cached extension release by GC
     */
    @Test
    void cacheTest() {
        ClassExtensionRecordsTest.Book book = new ClassExtensionRecordsTest.Book("");
        String extension = ClassExtensionRecordsTest.Item_Shippable.extensionFor(book).toString();
        System.gc();
        assertNotEquals(extension, ClassExtensionRecordsTest.Item_Shippable.extensionFor(book).toString());
    }
}