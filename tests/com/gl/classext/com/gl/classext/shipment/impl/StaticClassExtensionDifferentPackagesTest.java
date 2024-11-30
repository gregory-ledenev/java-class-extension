package com.gl.classext.com.gl.classext.shipment.impl;

import com.gl.classext.StaticClassExtension;
import com.gl.classext.com.gl.classext.shipment.Items.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StaticClassExtensionDifferentPackagesTest {
    @Test
    void shipmentTest() {
        Item[] items = {new Book("book"), new Furniture("furniture"), new ElectronicItem("electronic item")};

        StringBuilder shippingInfos = new StringBuilder();
        for (Item item : items) {
            ShippingInfo shippingInfo = ship(item);
            if (!shippingInfos.isEmpty())
                shippingInfos.append("\n");
            shippingInfos.append(shippingInfo);
            System.out.println(shippingInfo);
        }
        assertEquals("""
                     ShippingInfo[result=book shipped]
                     ShippingInfo[result=furniture shipped]
                     ShippingInfo[result=electronic item shipped]""",
                shippingInfos.toString());
    }

    public ShippingInfo ship(Item anItem) {
        return ((Shippable) StaticClassExtension.sharedExtension(anItem, Shippable.class, List.of("com.gl.classext.com.gl.classext.shipment.impl"))).ship();
    }
}
