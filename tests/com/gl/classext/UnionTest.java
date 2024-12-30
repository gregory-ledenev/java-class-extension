package com.gl.classext;

import org.junit.jupiter.api.Test;

import static java.lang.System.out;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UnionTest {
    public record Book(String name) {
        public String getName() {
            return name;
        }

        public String getName(String suffix) {
            return name + suffix;
        }
    }

    public record Furniture(String name) {
        public String getName() {
            return name;
        }

        public String getName(String suffix) {
            return name + suffix;
        }
    }

    public record ElectronicItem(String name) {
        public String getName() {
            return name;
        }

        public String getName(String suffix) {
            return name + suffix;
        }
    }

    public record AutoPart(String name) {
        public String getName() {
            return name;
        }

        public String getName(String suffix) {
            return name + suffix;
        }
    }

    public record ShippingInfo(String result) {}

    @ExtensionInterface
    public interface Shippable {
        String name();
        String getName();
        String getName(String suffix);
        ShippingInfo ship();
    }

    private static DynamicClassExtension setupDynamicClassExtension() {
        return new DynamicClassExtension().builder().extensionInterface(Shippable.class).
                    operationName("ship").
                        operation(Book.class, book -> new ShippingInfo(book.name() + " book shipped")).
                        operation(Furniture.class, furniture -> new ShippingInfo(furniture.name() + " furniture shipped")).
                        operation(ElectronicItem.class, electronicItem -> new ShippingInfo(electronicItem.name() + " electronic item shipped")).
                        operation(AutoPart.class, electronicItem -> new ShippingInfo(electronicItem.name() + " auto part shipped")).
                    operationName("getName").
                        operation(Object.class, (object) -> DynamicClassExtension.performOperation("getName", object)).
                        operation(Object.class, (Object object, String suffix) -> DynamicClassExtension.performOperation("getName", object, suffix)).
                    operationName("name").
                        operation(Object.class, (object) -> DynamicClassExtension.performOperation("name", object)).
                build();
    }

    @Test
    void operationsUsingBuilderTest() {
        DynamicClassExtension dynamicClassExtension = setupDynamicClassExtension();
        Object[] items = {
                new Book("The Mythical Man-Month"),
                new Furniture("Sofa"),
                new ElectronicItem("Soundbar"),
                new AutoPart("Tire"),
        };

        StringBuilder shippingInfos = new StringBuilder();
        for (Object item : items) {
            Shippable extension = dynamicClassExtension.extension(item, Shippable.class);
            if (!shippingInfos.isEmpty())
                shippingInfos.append("\n");
            ShippingInfo ship = extension.ship();
            shippingInfos.append(ship).append("\n");
            shippingInfos.append(extension.name()).append("\n");
            shippingInfos.append(extension.getName()).append("\n");
            shippingInfos.append(extension.getName("123"));
        }
        out.println(shippingInfos);

        assertEquals("""
                                ShippingInfo[result=The Mythical Man-Month book shipped]
                                The Mythical Man-Month
                                The Mythical Man-Month
                                The Mythical Man-Month123
                                ShippingInfo[result=Sofa furniture shipped]
                                Sofa
                                Sofa
                                Sofa123
                                ShippingInfo[result=Soundbar electronic item shipped]
                                Soundbar
                                Soundbar
                                Soundbar123
                                ShippingInfo[result=Tire auto part shipped]
                                Tire
                                Tire
                                Tire123""",
                shippingInfos.toString());
    }
}
