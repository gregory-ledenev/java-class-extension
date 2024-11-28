package com.gl.classext.com.gl.classext.shipment.impl;

@SuppressWarnings("unused")
public class Items {
    public static class Book_Shippable extends com.gl.classext.com.gl.classext.shipment.Items.Item_Shippable {
        public Book_Shippable(com.gl.classext.com.gl.classext.shipment.Items.Item aDelegate) {
            super(aDelegate);
        }

        public com.gl.classext.com.gl.classext.shipment.Items.ShippingInfo ship() {
            return new com.gl.classext.com.gl.classext.shipment.Items.ShippingInfo(getDelegate() + " shipped");
        }
    }

    public static class Furniture_Shippable extends com.gl.classext.com.gl.classext.shipment.Items.Item_Shippable {
        public Furniture_Shippable(com.gl.classext.com.gl.classext.shipment.Items.Item aDelegate) {
            super(aDelegate);
        }

        public com.gl.classext.com.gl.classext.shipment.Items.ShippingInfo ship() {
            return new com.gl.classext.com.gl.classext.shipment.Items.ShippingInfo(getDelegate() + " shipped");
        }
    }

    public static class ElectronicItem_Shippable extends com.gl.classext.com.gl.classext.shipment.Items.Item_Shippable {
        public ElectronicItem_Shippable(com.gl.classext.com.gl.classext.shipment.Items.Item aDelegate) {
            super(aDelegate);
        }

        public com.gl.classext.com.gl.classext.shipment.Items.ShippingInfo ship() {
            return new com.gl.classext.com.gl.classext.shipment.Items.ShippingInfo(getDelegate() + " shipped");
        }
    }
}
