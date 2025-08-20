package com.gl.classext.com.gl.classext.shipment.impl;

import com.gl.classext.com.gl.classext.shipment.Item;
import com.gl.classext.com.gl.classext.shipment.ItemShippable;

public class ElectronicItemShippable extends ItemShippable {
    public ElectronicItemShippable(Item aDelegate) {
        super(aDelegate);
    }

    public ShippingInfo ship() {
        return new ShippingInfo(getDelegate() + " shipped");
    }
}