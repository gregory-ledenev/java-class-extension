package com.gl.classext;

import org.junit.jupiter.api.Test;

import java.util.List;

public class ShapesTest {
    record Circle(int radius) {}
    record Rectangle(int width, int height) {}
    record Octagon(int radius) {}

    // virtual common interface for all shapes
    public interface Shape {
        String getDescription();
    }

    // define all dynamic operations for Shape interface
    static DynamicClassExtension classExtension = new DynamicClassExtension().builder(Shape.class).
            operationName("getDescription").
                operation(Circle.class, c -> c.radius > 10 ? "Large Circle" : "Small Circle").
                operation(Rectangle.class, r -> r.width() == r.height ? "Square" : "Rectangle").
                operation(Object.class, Object::toString).
            build();

    @Test
    void descriptionTest() {
        List<?> shapes = List.of(
                new Circle(5),
                new Circle(100),
                new Rectangle(50, 50),
                new Rectangle(100, 50),
                new Octagon(50));
        for (Object shape : shapes) {
            // obtain an extension and treat all the shapes uniformly
            Shape extension = classExtension.extension(shape, Shape.class);
            System.out.println(extension.getDescription());
        }
    }
}

