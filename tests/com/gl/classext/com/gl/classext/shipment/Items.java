package com.gl.classext.com.gl.classext.shipment;

import com.gl.classext.ClassExtension;

public class Items {
    public static class Item {
        private final String name;

        public Item(String aName) {
            name = aName;
        }

        @Override
        public String toString() {
            return name;
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

    public record ShippingInfo(String result) {}

    public static class Item_Shippable implements ClassExtension.DelegateHolder<Item> {
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
}
