# Java Class Extension Library
The Java Class Extension library provides an ability to emulate class extensions (categories) in Java. The library supports the following approaches:

1. [Static Class Extensions](doc/static-class-extensions.md): define and implement extensions as usual Java classes and then utilize the Java Class Extension library to find matching extensions.
2. [Dynamic Class Extensions](doc/dynamic-class-extensions.md): utilize the Java Class Extension library to define extensions by composing them as sets of lambda operations and let the library create extensions dynamically on the fly.

Both approaches leverage the `ClassExtension` interface, which facilitates querying for an extension based on an object's extension interface. Once obtained, these extensions unlock additional functionality with remarkable ease:
```java
Book book = new Book("The Mythical Man-Month");
Shippable shippable = StaticClassExtension.sharedExtension(book, Shippable.class);
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
Java Class Extension library provides a valuable alternative for class extensions (not supported in Java) with just a little more verbose code and little more complex implementation.
   
## Details ##
1. [Introduction](doc/introduction.md)
2. [Static Class Extensions](doc/static-class-extensions.md)
3. [Dynamic Class Extensions](doc/dynamic-class-extensions.md)

## Adding to Your Build 
To add Java Class Extension library to your build:
* Add */dist/class-extension-x.x.x.jar* as classes
* Add */dist/class-extension-doc-x.x.x.jar* as documentation

## License
The Java Class Extension library is licensed under the terms of the [MIT License](https://opensource.org/license/mit).
