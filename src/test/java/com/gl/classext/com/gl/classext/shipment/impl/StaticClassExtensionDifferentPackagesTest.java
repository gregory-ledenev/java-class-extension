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

    @Test
    void removeExtensionPackageTest() {
        GroceryItem groceryItem = new GroceryItem("bread");
        Shippable shippable = StaticClassExtension.sharedExtension(groceryItem, Shippable.class);
        assertEquals("ShippingInfo[result=bread shipped]", shippable.ship().toString());

        StaticClassExtension.sharedInstance().cacheClear();

        try {
            StaticClassExtension.sharedInstance().removeExtensionPackage(Shippable.class, "com.gl.classext.com.gl.classext.shipment.impl.grocery");
            shippable = StaticClassExtension.sharedExtension(groceryItem, Shippable.class);
            assertEquals("ShippingInfo[result=bread NOT shipped]", shippable.ship().toString());
        } finally {
            StaticClassExtension.sharedInstance().addExtensionPackage(Shippable.class, "com.gl.classext.com.gl.classext.shipment.impl.grocery");
        }
    }

    static {
        StaticClassExtension.sharedInstance().addExtensionPackage(Shippable.class, "com.gl.classext.com.gl.classext.shipment.impl.grocery");
    }

    public Shippable.ShippingInfo ship(Item anItem) {
        Shippable shippable = StaticClassExtension.sharedExtension(anItem, Shippable.class);
        return shippable.ship();
    }
}
