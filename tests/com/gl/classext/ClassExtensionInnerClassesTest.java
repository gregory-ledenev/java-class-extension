package com.gl.classext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ClassExtensionInnerClassesTest {
    static class Shape {
    }

    static class Circle extends Shape {
    }

    static class Rectangle extends Shape {
    }

    static class Oval extends Shape {
    }

    static class Square extends Rectangle {
    }

    static class Shape_Describable implements ClassExtension.DelegateHolder<Shape> {
        private Shape delegate;
        public String getDescription() {
            return "Shape_Describable description";
        }

        @Override
        public Shape getDelegate() {
            return delegate;
        }

        @Override
        public void setDelegate(Shape aDelegate) {
            delegate = aDelegate;
        }
    }

    static class Circle_Describable extends Shape_Describable {
        public String getDescription() {
            return "Circle_Describable description";
        }
    }

    static class Rectangle_Describable extends Shape_Describable {
        public String getDescription() {
            return "Circle_Describable description";
        }
    }

    static class Square_Describable extends Rectangle_Describable {
        public String getDescription() {
            return "Square_Describable description";
        }
    }

    /**
     * Tests for exact match when a matching extension is defined for the passed object's class
     */
    @Test
    void exactMatchExtension() {
        Square square = new Square();
        Shape_Describable shape = ClassExtension.extension(square, Shape_Describable.class);

        assertEquals(shape, ClassExtension.extension(square, Shape_Describable.class));
        System.out.println("Description for shape is: " + shape.getDescription());
        assertEquals(shape.getDelegate(), square);
        assertEquals(Square_Describable.class, shape.getClass());
        assertEquals("Square_Describable description", shape.getDescription());
    }

    /**
     * Tests for partial match when only a super class extension is defined for the passed object's class
     */
    @Test
    void partialMatchExtension() {
        Oval oval = new Oval();
        Shape_Describable shape = ClassExtension.extension(oval, Shape_Describable.class);

        assertEquals(shape, ClassExtension.extension(oval, Shape_Describable.class));
        System.out.println("Description for shape is: " + shape.getDescription());
        assertEquals(shape.getDelegate(), oval);
        assertEquals(Shape_Describable.class, shape.getClass());
        assertEquals("Shape_Describable description", shape.getDescription());
    }

    public static class Item {
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

    public static class Book extends Item {
        public Book(String aName) {
            super(aName);
        }
    }

    public static class Furniture extends Item {
        public Furniture(String aName) {
            super(aName);
        }
    }

    public static class ElectronicItem extends Item {
        public ElectronicItem(String aName) {
            super(aName);
        }
    }

    public record ShippingInfo(String result) {}

    public static class Item_Shippable implements ClassExtension.DelegateHolder<Item> {
        public ShippingInfo ship() {
            return new ShippingInfo(getDelegate() + " NOT shipped");
        }

        private Item delegate;
        @Override
        public Item getDelegate() {
            return delegate;
        }

        @Override
        public void setDelegate(Item aDelegate) {
            delegate = aDelegate;
        }
    }

    static class Book_Shippable extends Item_Shippable{
        public ShippingInfo ship() {
            return new ShippingInfo(getDelegate() + " shipped");
        }
    }

    static class Furniture_Shippable extends Item_Shippable {
        public ShippingInfo ship() {
            return new ShippingInfo(getDelegate() + " shipped");
        }
    }

    static class ElectronicItem_Shippable extends Item_Shippable {
        public ShippingInfo ship() {
            return new ShippingInfo(getDelegate() + " shipped");
        }
    }

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
        return ClassExtension.extension(anItem, Item_Shippable.class).ship();
    }
}