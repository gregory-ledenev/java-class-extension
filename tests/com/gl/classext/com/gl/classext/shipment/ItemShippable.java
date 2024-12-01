package com.gl.classext.com.gl.classext.shipment;

public class ItemShippable implements Shippable {
    public ShippingInfo ship() {
        return new Shippable.ShippingInfo(delegate + " NOT shipped");
    }

    public ItemShippable(Item aDelegate) {
        delegate = aDelegate;
    }

    public Item getDelegate() {
        return delegate;
    }

    private final Item delegate;
}