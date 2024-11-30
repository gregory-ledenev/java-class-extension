package com.gl.classext.com.gl.classext.shipment.impl;

@SuppressWarnings("unused")
public class Items {
    public static class ItemShippable extends com.gl.classext.com.gl.classext.shipment.Items.Shippable {
        public ItemShippable(com.gl.classext.com.gl.classext.shipment.Items.Item aDelegate) {
            super(aDelegate);
        }

        public com.gl.classext.com.gl.classext.shipment.Items.ShippingInfo ship() {
            return new com.gl.classext.com.gl.classext.shipment.Items.ShippingInfo(getDelegate() + " shipped");
        }
    }

    public static class BookShippable extends com.gl.classext.com.gl.classext.shipment.Items.Shippable {
        public BookShippable(com.gl.classext.com.gl.classext.shipment.Items.Item aDelegate) {
            super(aDelegate);
        }

        public com.gl.classext.com.gl.classext.shipment.Items.ShippingInfo ship() {
            return new com.gl.classext.com.gl.classext.shipment.Items.ShippingInfo(getDelegate() + " shipped");
        }
    }

    public static class FurnitureShippable extends com.gl.classext.com.gl.classext.shipment.Items.Shippable {
        public FurnitureShippable(com.gl.classext.com.gl.classext.shipment.Items.Item aDelegate) {
            super(aDelegate);
        }

        public com.gl.classext.com.gl.classext.shipment.Items.ShippingInfo ship() {
            return new com.gl.classext.com.gl.classext.shipment.Items.ShippingInfo(getDelegate() + " shipped");
        }
    }

    public static class ElectronicItemShippable extends com.gl.classext.com.gl.classext.shipment.Items.Shippable {
        public ElectronicItemShippable(com.gl.classext.com.gl.classext.shipment.Items.Item aDelegate) {
            super(aDelegate);
        }

        public com.gl.classext.com.gl.classext.shipment.Items.ShippingInfo ship() {
            return new com.gl.classext.com.gl.classext.shipment.Items.ShippingInfo(getDelegate() + " shipped");
        }
    }
}
