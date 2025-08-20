package com.gl.classext.com.gl.classext.shipment;

import com.gl.classext.ExtensionInterface;

@ExtensionInterface(packages = {"com.gl.classext.com.gl.classext.shipment.impl"})
public interface Shippable {
    ShippingInfo ship();

    record ShippingInfo(String result) {}
}