package com.gl.classext.com.gl.classext.shipment.impl;

import com.gl.classext.StaticClassExtension;
import com.gl.classext.com.gl.classext.shipment.*;
import com.gl.classext.com.gl.classext.shipment.impl.grocery.GroceryItem;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StaticClassExtensionDifferentPackagesTest {
    @Test
    void shipmentTest() {
        Item[] items = {
                new Book("book"),
                new Furniture("furniture"),
                new ElectronicItem("electronic item"),
                new AutoPart("tire"),
                new GroceryItem("bread")
        };

        StringBuilder shippingInfos = new StringBuilder();
        for (Item item : items) {
            Shippable.ShippingInfo shippingInfo = ship(item);
            if (!shippingInfos.isEmpty())
                shippingInfos.append("\n");
            shippingInfos.append(shippingInfo);
            System.out.println(shippingInfo);
        }
        assertEquals("""
                     ShippingInfo[result=book shipped]
                     ShippingInfo[result=furniture shipped]
                     ShippingInfo[result=electronic item shipped]
                     ShippingInfo[result=tire NOT shipped]
                     ShippingInfo[result=bread shipped]""",
                shippingInfos.toString());
    }

    public Shippable.ShippingInfo ship(Item anItem) {
        Shippable shippable = StaticClassExtension.sharedExtension(anItem,
                Shippable.class,
                Arrays.asList(
                        "com.gl.classext.com.gl.classext.shipment",
                        "com.gl.classext.com.gl.classext.shipment.impl.grocery"
                ));
        return shippable.ship();
    }
}
