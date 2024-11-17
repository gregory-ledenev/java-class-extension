package com.gl.classext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ClassExtensionRecordsTest {
    public interface Item {
        String name();
    }

    public record Book(String name) implements Item {
    }

    public record Furniture(String name) implements Item {
    }

    public record ElectronicItem(String name) implements Item {
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

    static class Book_Shippable extends Item_Shippable {
        public ShippingInfo ship() {
            return new ShippingInfo(getDelegate() + " book shipped");
        }
    }

    static class Furniture_Shippable extends Item_Shippable {
        public ShippingInfo ship() {
            return new ShippingInfo(getDelegate() + " furniture shipped");
        }
    }

    static class ElectronicItem_Shippable extends Item_Shippable {
        public ShippingInfo ship() {
            return new ShippingInfo(getDelegate() + " electronic item shipped");
        }
    }

    @Test
    void shipmentTest() {
        Item[] items = {new Book("The Mythical Man-Month"), new Furniture("Sofa"), new ElectronicItem("Soundbar")};

        StringBuilder shippingInfos = new StringBuilder();
        for (Item item : items) {
            ShippingInfo shippingInfo = ship(item);
            if (!shippingInfos.isEmpty())
                shippingInfos.append("\n");
            shippingInfos.append(shippingInfo);
            System.out.println(shippingInfo);
        }
        assertEquals("""
                     ShippingInfo[result=Book[name=The Mythical Man-Month] book shipped]
                     ShippingInfo[result=Furniture[name=Sofa] furniture shipped]
                     ShippingInfo[result=ElectronicItem[name=Soundbar] electronic item shipped]""",
                shippingInfos.toString());
    }

    public ShippingInfo ship(Item anItem) {
        return Item_Shippable.extensionFor(anItem).ship();
    }
}