package com.gl.classext;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DynamicClassExtensionTest {
    static class Item {
        private final String name;

        public Item(String aName) {
            name = aName;
        }

        @Override
        public String toString() {
            return name;
        }

        public String getName() {
            return name;
        }
    }

    static class Book extends Item {
        public Book(String aName) {
            super(aName);
        }
    }

    static class Furniture extends Item {
        public Furniture(String aName) {
            super(aName);
        }
    }

    static class ElectronicItem extends Item {
        public ElectronicItem(String aName) {
            super(aName);
        }
    }

    static class AutoPart extends Item {
        public AutoPart(String aName) {
            super(aName);
        }
    }

    record ShippingInfo(String result) {
    }

    record TrackingInfo(String result) {
    }

    interface Item_Shippable {
        ShippingInfo ship();
        TrackingInfo track();
        void log();
    }


    @Test
    void operationTest() {
        DynamicClassExtension build = new DynamicClassExtension();
        build.addExtensionOperation(Item.class, Item_Shippable.class, "ship", anItem3 -> {
            return new ShippingInfo(anItem3.getName() + " item NOT shipped");
        });
        build.addExtensionOperation(Book.class, Item_Shippable.class, "ship", anItem2 -> {
            return new ShippingInfo(anItem2.getName() + " book shipped");
        });
        build.addExtensionOperation(Furniture.class, Item_Shippable.class, "ship", anItem1 -> {
            return new ShippingInfo(anItem1.getName() + " furniture shipped");
        });
        build.addExtensionOperation(ElectronicItem.class, Item_Shippable.class, "ship", anItem -> {
            return new ShippingInfo(anItem.getName() + " electronic item shipped");
        });

        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        StringBuilder shippingInfos = new StringBuilder();
        for (Item item : items) {
            ShippingInfo shippingInfo = build.extension(item, Item_Shippable.class).ship();
            if (!shippingInfos.isEmpty())
                shippingInfos.append("\n");
            shippingInfos.append(shippingInfo);
            System.out.println(shippingInfo);
        }

        assertEquals("""
                     ShippingInfo[result=The Mythical Man-Month book shipped]
                     ShippingInfo[result=Sofa furniture shipped]
                     ShippingInfo[result=Soundbar electronic item shipped]
                     ShippingInfo[result=Tire item NOT shipped]""",
                shippingInfos.toString());
    }

    @Test
    void operationUsingBuilderTest() {

        StringBuilder shippingLog = new StringBuilder();

        DynamicClassExtension build = new DynamicClassExtension.Builder<>(Item.class, Item_Shippable.class).
                operationName("ship").
                    operation(Item.class, book -> new ShippingInfo(book.getName() + " item NOT shipped")).
                    operation(Book.class, book -> new ShippingInfo(book.getName() + " book shipped")).
                    operation(Furniture.class, furniture -> new ShippingInfo(furniture.getName() + " furniture shipped")).
                    operation(ElectronicItem.class, electronicItem -> new ShippingInfo(electronicItem.getName() + " electronic item shipped")).
                operationName("log").
                    voidOperation(Item.class, item -> {
                        if (! shippingLog.isEmpty())
                            shippingLog.append("\n");
                        shippingLog.append(item.getName() + " is about to be shipped");
                    }).
                operationName("track").
                    operation(Item.class, item -> new TrackingInfo(item.getName() + "item on its way")).
                build();

        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        StringBuilder shippingInfos = new StringBuilder();
        for (Item item : items) {
            Item_Shippable extension = build.extension(item, Item_Shippable.class);
            extension.log();
            ShippingInfo shippingInfo = extension.ship();
            if (!shippingInfos.isEmpty())
                shippingInfos.append("\n");
            shippingInfos.append(shippingInfo);
        }
        System.out.println(shippingLog.toString());
        System.out.printf(shippingInfos.toString());

        assertEquals("""
                     The Mythical Man-Month is about to be shipped
                     Sofa is about to be shipped
                     Soundbar is about to be shipped
                     Tire is about to be shipped""", shippingLog.toString());

        assertEquals("""
                     ShippingInfo[result=The Mythical Man-Month book shipped]
                     ShippingInfo[result=Sofa furniture shipped]
                     ShippingInfo[result=Soundbar electronic item shipped]
                     ShippingInfo[result=Tire item NOT shipped]""",
                shippingInfos.toString());
    }
}

