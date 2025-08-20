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
        double getArea();
    }

    // define all dynamic operations for Shape interface
    static {
        DynamicClassExtension.sharedBuilder().extensionInterface(Shape.class).
                operationName("getDescription").
                    operation(Circle.class, c -> c.radius > 10 ? "Large Circle" : "Small Circle").
                    operation(Rectangle.class, r -> r.width() == r.height ? "Square" : "Rectangle").
                    operation(Object.class, Object::toString).
                operationName("getArea").
                    operation(Circle.class, c -> Math.PI * c.radius() * c.radius()).
                    operation(Rectangle.class, r -> (double) (r.width() * r.height())).
                    operation(Octagon.class, o -> 2 * o.radius() * o.radius() * (1 + Math.sqrt(2))).
                build();
    }

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
            Shape extension = DynamicClassExtension.sharedExtension(shape, Shape.class);
            System.out.println(extension.getDescription());
        }
    }

    @Test
    void areaTest() {
        List<?> shapes = List.of(new Circle(5), new Rectangle(50, 50));

        for (Object shape : shapes) {
            Shape extension = sharedExtension.extension(shape, Shape.class);
            System.out.println(extension.getArea());
        }
    }

    private static final DynamicClassExtension sharedExtension = new DynamicClassExtension().builder().
            extensionInterface(Shape.class).
                operationName("getArea").
                    operation(Circle.class, c -> Math.PI * c.radius() * c.radius()).
                    operation(Rectangle.class, r -> (double) (r.width() * r.height())).
            build();
}

