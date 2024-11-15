package com.gl.classext.com.gl.classext.shipment.impl;

import com.gl.classext.ClassExtension;
import com.gl.classext.com.gl.classext.shipment.Items.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClassExtensionDifferentPackagesTest {
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
        return ((Item_Shippable) ClassExtension.extension(anItem, "Shippable", List.of("com.gl.classext.com.gl.classext.shipment.impl"))).ship();
    }
}
