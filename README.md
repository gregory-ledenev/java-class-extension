# Java Class Extension Library
The Java Class Extension library provides a way to mimic class extensions (categories) by finding matching extension objects and using them to perform any extended functionality.

## Introduction
The core of Java Class Extension is the `ClassExtension` class that provides several methods to allow finding and creation of extension objects on demand.

For example: lets imagine a `Shape` class that provides only coordinates and dimensions of shapes. If we need to introduce a drawable shape we can create a `Shape(Drawable)` class extension with the `draw()` method, and we can call the `draw()` method as simple as `new Shape().draw()`. Though such kind of extension is not available in Java so the `ClassExtension` provides a way to simulate that. To do it we should introduce an extension class `Shape_Drawable` with the `draw()` method. Now we can call the `draw()` method as simple as `ClassExtension.extension(new Shape(), Shape_Drawable.class).draw()`.
```java
      class Shape {
          // some properties and methods here
      }
      ...more shape classes here...
 
      class Shape_Drawable implements DelegateHolder {
          private Shape delegate;
          public void draw() {
              // use delegate properties to draw a shape
          }
          public Shape getDelegate() {
              return delegate;
          }
          public void setDelegate(Shape aDelegate) {
              delegate = aDelegate;
          }
      }
      ...more shape drawable extensions here
 
      class ShapesView {
          void drawShapes() {
              List<Shape> shapes = ...
              for (Shape shape : shapes)
                  ClassExtension.extension(shape, Shape_Drawable.class).draw();
          }
      }
```
All the extension classes must implement the `DelegateHolder` interface and must end with the name of an extension delimited by underscore e.g. `Shape_Drawable` where `Shape` is the name of the class and `Drawable` is the name of extension.

`ClassExtension` takes care of inheritance so it is possible to design and implement class extensions hierarchy that fully or partially resembles original classes hierarchy. If there's no explicit extension specified for particular class - its parent extension will be utilized. For example, if there's no explicit `Drawable` extension for `Oval` objects - base `Shape_Drawable` will be used instead.

Cashing of extension objects are supported out of the box. Cache utilizes weak references to release extension objects that are not in use. Though, to perform full cleanup either the `cacheCleanup()` should be used or automatic cleanup can be initiated via the `scheduleCacheCleanup()`. If automatic cache cleanup is used - it can be stopped by calling the `shutdownCacheCleanup()`.

## Adding to Your Build 
To add Java Class Extension library to your build:
* Add */dist/class-extension-x.x.x.jar* as classes
* Add */dist/class-extension-doc-x.x.x.jar* as documentation

## License
The Java Class Extension library is licensed under the terms of the [MIT License](https://opensource.org/license/mit).
