package com.gl.classext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("unused")
public class StaticClassExtensionRecordsTest {
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

    @ExtensionInterface
    public interface Shippable {
        public ShippingInfo ship();
    }

    public static class ItemShippable implements Shippable, StaticClassExtension.DelegateHolder<Item> {
        public static Shippable extensionFor(Item anItem) {
            return StaticClassExtension.sharedExtension(anItem, Shippable.class);
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

    static class BookShippable extends ItemShippable {
        public ShippingInfo ship() {
            return new ShippingInfo(getDelegate() + " book shipped");
        }
    }

    static class FurnitureShippable extends ItemShippable {
        public ShippingInfo ship() {
            return new ShippingInfo(getDelegate() + " furniture shipped");
        }
    }

    static class ElectronicItemShippable extends ItemShippable {
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
        return ItemShippable.extensionFor(anItem).ship();
    }
}