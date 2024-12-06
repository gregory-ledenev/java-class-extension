package com.gl.classext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DynamicClassExtensionRecordsTest {
    public interface Item {
        String name();
    }

    public record Book(String name) implements Item {
    }

    public record Furniture(String name) implements Item {
    }

    public record ElectronicItem(String name) implements Item {
    }

    public record AutoPart(String name) implements Item {
    }

    public record ShippingInfo(String result) {
    }

    public record TrackingInfo(String result) {
    }

    @ExtensionInterface
    public interface Shippable {
        ShippingInfo ship();
        void log(boolean isVerbose);
        void log();
        TrackingInfo track(boolean isVerbose);
        TrackingInfo track();
    }

    public interface ItemShippable extends Item, Shippable {
    }

    private static final StringBuilder LOG = new StringBuilder();

    private static DynamicClassExtension setupDynamicClassExtension() {
        return new DynamicClassExtension().builder(Shippable.class).
                opName("ship").
                    op(Item.class, book -> new ShippingInfo(book.name() + " item NOT shipped")).
                    op(Book.class, book -> new ShippingInfo(book.name() + " book shipped")).
                    op(Furniture.class, furniture -> new ShippingInfo(furniture.name() + " furniture shipped")).
                    op(ElectronicItem.class, electronicItem -> new ShippingInfo(electronicItem.name() + " electronic item shipped")).
                opName("log").
                    voidOp(Item.class, (Item item, Boolean isVerbose) -> {
                        if (!LOG.isEmpty())
                            LOG.append("\n");
                        LOG.append(item.name()).append(" is about to be shipped in 1 hour");
                    }).
                    voidOp(Item.class, item -> {
                        if (!LOG.isEmpty())
                            LOG.append("\n");
                        LOG.append(item.name()).append(" is about to be shipped");
                    }).
                opName("track").
                    op(Item.class, item -> new TrackingInfo(item.name() + " item on its way")).
                    op(Item.class, (Item item, Boolean isVerbose) -> new TrackingInfo(item.name() +
                            " item on its way" + (isVerbose ? "Status: SHIPPED" : ""))).
                build();
    }

    @Test
    void operationUsingBuilderTest() {
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension();
        dynamicClassExtension.setVerbose(true);
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        StringBuilder shippingInfos = new StringBuilder();
        for (Item item : items) {
            Shippable extension = dynamicClassExtension.extension(item, Shippable.class);
            extension.log();
            if (!shippingInfos.isEmpty())
                shippingInfos.append("\n");
            shippingInfos.append(extension.ship());
            shippingInfos.append("\n").append(extension.track());
        }
        System.out.println(LOG);
        System.out.println(shippingInfos);

        assertEquals("""
                     The Mythical Man-Month is about to be shipped
                     Sofa is about to be shipped
                     Soundbar is about to be shipped
                     Tire is about to be shipped""", LOG.toString());

        assertEquals("""
                     ShippingInfo[result=The Mythical Man-Month book shipped]
                     TrackingInfo[result=The Mythical Man-Month item on its way]
                     ShippingInfo[result=Sofa furniture shipped]
                     TrackingInfo[result=Sofa item on its way]
                     ShippingInfo[result=Soundbar electronic item shipped]
                     TrackingInfo[result=Soundbar item on its way]
                     ShippingInfo[result=Tire item NOT shipped]
                     TrackingInfo[result=Tire item on its way]""",
                shippingInfos.toString());
    }

    @Test
    void overriddenOperationsTest() {
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension();
        dynamicClassExtension.setVerbose(true);
        Item[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        StringBuilder names = new StringBuilder();
        for (Item item : items) {
            ItemShippable extension = dynamicClassExtension.extension(item, ItemShippable.class);
            if (!names.isEmpty())
                names.append("\n");
            names.append(extension.name());
        }
        System.out.println(names.toString());
        assertEquals("""
                     The Mythical Man-Month
                     Sofa
                     Soundbar
                     Tire""", names.toString());

        dynamicClassExtension = dynamicClassExtension.builder(Item.class).
                opName("name").
                    op(Item.class, item -> item.name()  + "[OVERRIDDEN]").
                    op(AutoPart.class, item -> item.name()  + "[OVERRIDDEN for AutoPart]").
                build();
        names.setLength(0);
        for (Item item : items) {
            ItemShippable extension = dynamicClassExtension.extension(item, ItemShippable.class);
            if (!names.isEmpty())
                names.append("\n");
            names.append(extension.name());
        }
        System.out.println(names.toString());
        assertEquals("""
                     The Mythical Man-Month[OVERRIDDEN]
                     Sofa[OVERRIDDEN]
                     Soundbar[OVERRIDDEN]
                     Tire[OVERRIDDEN for AutoPart]""", names.toString());
    }
}
