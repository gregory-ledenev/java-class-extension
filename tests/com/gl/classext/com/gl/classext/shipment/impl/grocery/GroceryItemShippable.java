package com.gl.classext.com.gl.classext.shipment.impl.grocery;

import com.gl.classext.com.gl.classext.shipment.Item;
import com.gl.classext.com.gl.classext.shipment.ItemShippable;

public class GroceryItemShippable extends ItemShippable {
    public GroceryItemShippable(Item aDelegate) {
        super(aDelegate);
    }

    public ShippingInfo ship() {
        return new ShippingInfo(getDelegate() + " shipped");
    }
}
