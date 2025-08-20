package com.gl.classext.com.gl.classext.shipment.impl;

import com.gl.classext.com.gl.classext.shipment.Item;
import com.gl.classext.com.gl.classext.shipment.ItemShippable;

public class FurnitureShippable extends ItemShippable {
    public FurnitureShippable(Item aDelegate) {
        super(aDelegate);
    }

    public ShippingInfo ship() {
        return new ShippingInfo(getDelegate() + " shipped");
    }
}