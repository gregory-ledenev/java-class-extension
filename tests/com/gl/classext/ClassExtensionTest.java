package com.gl.classext;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Shape {
}

class Circle extends Shape {
}

class Rectangle extends Shape {
}

class Oval extends Shape {
}

class Square extends Rectangle {
}

class Shape_Describable implements ClassExtension.DelegateHolder<Shape> {
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

class Circle_Describable extends Shape_Describable {
    public String getDescription() {
        return "Circle_Describable description";
    }
}

class Rectangle_Describable extends Shape_Describable {
    public String getDescription() {
        return "Circle_Describable description";
    }
}

class Square_Describable extends Rectangle_Describable {
    public String getDescription() {
        return "Square_Describable description";
    }
}

class ClassExtensionTest {
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