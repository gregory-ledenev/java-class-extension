package com.gl.classext;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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
        ShippingInfo ship();
    }

    private static DynamicClassExtension setupDynamicClassExtension() {
        return new DynamicClassExtension().builder().extensionInterface(Shippable.class).
                    operationName("ship").
                        operation(Book.class, book -> new ShippingInfo(book.name() + " book shipped")).
                        operation(Furniture.class, furniture -> new ShippingInfo(furniture.name() + " furniture shipped")).
                        operation(ElectronicItem.class, electronicItem -> new ShippingInfo(electronicItem.name() + " electronic item shipped")).
                        operation(AutoPart.class, electronicItem -> new ShippingInfo(electronicItem.name() + " auto part shipped")).
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

        List<String> names = new ArrayList<>();
        List<String> shippingInfos = new ArrayList<>();

        for (Object item : items) {
            Shippable extension = dynamicClassExtension.extension(item, Shippable.class);
            names.add(extension.name());
            shippingInfos.add(extension.ship().result);
        }
        assertEquals("""
                    The Mythical Man-Month book shipped
                    Sofa furniture shipped
                    Soundbar electronic item shipped
                    Tire auto part shipped""", String.join("\n", shippingInfos));
        assertEquals("""
                    The Mythical Man-Month
                    Sofa
                    Soundbar
                    Tire""", String.join("\n", names));
    }
}
