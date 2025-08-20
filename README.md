# Java Class Extension Library

The Java Class Extension library provides an ability to emulate class extensions (categories) in Java.

Class extensions (categories) are beneficial because they:
* Improve code organization by separating core data structures from specialized functionality.
  * Enhance modularity, allowing domain-specific logic to be added without modifying original classes.
  * Promote the Single Responsibility Principle by keeping data classes focused on data representation.
  * Enable adding new features to existing types, including third-party or framework types.
  * Facilitate better separation of concerns in complex applications.
  * Improve code readability and maintainability.

These benefits lead to more flexible, maintainable, and efficient code structures. Unfortunately, Java does not natively support class extensions (categories) and there is a little chance such support is going to be introduced soon.

Java Class Extension library provides a valuable alternative for native class extensions with just a little more verbose code and little more complex implementation.

The library supports the following approaches for creating of class extensions:

1. [Static Class Extensions](doc/static-class-extensions.md): define and implement extensions as usual Java classes and then utilize the Java Class Extension library to find matching extensions.
2. [Dynamic Class Extensions](doc/dynamic-class-extensions.md): utilize the Java Class Extension library to define extensions by composing them as sets of lambda operations and let the library create extensions dynamically on the fly.

Both approaches leverage the `ClassExtension` interface, which facilitates querying for an extension based on an object's extension interface. Once obtained, these extensions unlock additional functionality with remarkable ease. For example, obtaining a `Shippable` extension and using its `ship()` method to perform shipping a book would look like:
```java
Book book = new Book("The Mythical Man-Month");
Shippable shippable = StaticClassExtension.sharedExtension(book, Shippable.class);
shippable.ship();
```

or 
```java
Book book = new Book("The Mythical Man-Month");
Shippable shippable = DynamicClassExtension.sharedExtension(book, Shippable.class);
shippable.ship();
```

Shipping a collection of items is equally straightforward:
```java
Item[] items = {
    new Book("The Mythical Man-Month"), 
    new Furniture("Sofa"), 
    new ElectronicItem("Soundbar")
};

for (Item item : items) {
    Shippable shippable = StaticClassExtension.sharedExtension(item, Shippable.class);
    item.ship();
}
```
It is possible to further simplify things by adding an `extensionFor(Item)` helper method to the `Shippable` interface:
```java
public interface Shippable {
	public static Shippable extensionFor(Item item) {
    	    return StaticClassExtension.sharedExtension(item, Shippable.class).ship();
	}
  ...
}
```
With that helper method, shipping becomes even simpler and shorter:
```java
Shippable.extensionFor(item).ship();
```

## Details ##
1. [Introduction](doc/introduction.md)
2. [Static Class Extensions](doc/static-class-extensions.md)
3. [Dynamic Class Extensions](doc/dynamic-class-extensions.md)
4. [Aspects](doc/aspects.md)
5. [Expressions](doc/expressions.md)
6. [Utilities](doc/utilities.md)

## Adding to Your Build 
To add Java Class Extension Library to your build system, you can use the following Maven dependency:
```xml
<dependency>
    <groupId>io.github.gregory-ledenev</groupId>
    <artifactId>class-extension</artifactId>
    <version>1.2.1</version>
</dependency>
```
To add JavaDoc:
```xml
<dependency>
    <groupId>io.github.gregory-ledenev</groupId>
    <artifactId>class-extension</artifactId>
    <version>1.2.1</version>
    <classifier>javadoc</classifier>
</dependency>
```

## License
The Java Class Extension library is licensed under the terms of the [MIT License](https://opensource.org/license/mit).
