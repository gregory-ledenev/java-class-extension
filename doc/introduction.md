## Java Class Extensions Library - Introduction

Consider a scenario where we are building a warehouse application designed to handle the shipping of various items. We have established a hierarchy of classes to represent the goods we have:
```java
class Item {
    …
}
class Book extends Item {
    …
}
class Furniture extends Item {
    …
}
class ElectronicItem extends Item {
    …
}
```
To implement shipping logic for each item, one might be tempted to add a `ship()` method directly to each `Item` class. While this is straightforward, it can lead to bloated classes filled with unrelated operations—such as shipping, storing, retrieving from a database, and rendering.

Instead of mixing these responsibilities, it’s better to keep items as primarily data classes and separate domain-specific logic from them.

### Functional Style

To ship an item, we could define a method that uses a modern switch statement with pattern matching:
```java
public ShippingInfo ship(Item item) {
    return switch (item) {
        case Book book -> shipBook(book);
        case Furniture furniture -> shipFurniture(furniture);
        case ElectronicItem electronicItem -> shipElectronicItem(electronicItem);
    };
}
```
While this method is simple and direct, it comes with several disadvantages:

1. It bypasses polymorphism, opting instead for an outdated imitation of it.
2. It violates SOLID principles, as introducing a new class (Item) requires modifications to existing code.
3. It is inconvenient and error-prone because the code can become scattered and duplicated throughout the project, potentially with varying content, making it difficult to locate and modify.
  
### Class Extensions

A more effective solution involves introducing a `Shippable` class extension (category) that encapsulates the `ship()` method for items. Each `Item` class would then have a corresponding `Shippable` extension:
```java
class Item (Shippable) {
    public abstract ShippingInfo ship();
}
class Book (Shippable) {
    public ShippingInfo ship() {}
}
class Furniture (Shippable) {
    public abstract ShippingInfo ship();
}
class ElectronicItem (Shippable) {
    public ShippingInfo ship() {}
}
```
With this design, shipping an item simplifies to a single call:
```java
Book book = new Book("The Mythical Man-Month");
book.ship();
```
This approach is clean, convenient, extensible, and maintainable. However, Java does not natively support class extensions, but it would be good to utilize that approach somehow.

### Java Class Extension Library
The Java Class Extension library provides an ability to emulate class extensions (categories) in Java.

Class extensions (categories) are beneficial because they:

Improve code organization by separating core data structures from specialized functionality.
Enhance modularity, allowing domain-specific logic to be added without modifying original classes.
Promote the Single Responsibility Principle by keeping data classes focused on data representation.
Enable adding new features to existing types, including third-party or framework types.
Facilitate better separation of concerns in complex applications.
Improve code readability and maintainability.
These benefits lead to more flexible, maintainable, and efficient code structures. Unfortunately, Java does not natively support class extensions (categories) and there is a little chance such support is going to be introduced in the near future.

Java Class Extension library provides a valuable alternative for native class extensions with just a little more verbose code and little more complex implementation.

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
With that helper method, shipping become even simpler and shorter:
```java
Shippable.extensionFor(item).ship();
```

The library supports the following approaches:

1. [Static Class Extensions](static-class-extensions.md): define and implement extensions as usual Java classes and then utilize the Java Class Extension library to find matching extension classes and create extension objects.
2. [Dynamic Class Extensions](dynamic-class-extensions.md): utilize the Java Class Extension library to define extensions by composing them as sets of lambda operations and let the library create extensions dynamically on the fly.

Next >> [Static Class Extensions](static-class-extensions.md)