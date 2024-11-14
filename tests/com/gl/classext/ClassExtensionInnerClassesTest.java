package com.gl.classext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassExtensionInnerClassesTest {
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
        assertEquals(shape.getClass(), Square_Describable.class);
        assertEquals(shape.getDescription(), "Square_Describable description");
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
        assertEquals(shape.getClass(), Shape_Describable.class);
        assertEquals(shape.getDescription(), "Shape_Describable description");
    }
}