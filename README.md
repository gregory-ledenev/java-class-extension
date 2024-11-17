# Java Class Extension Library
The Java Class Extension library provides a way to mimic class extensions (categories) by finding matching extension objects and using them to perform any extended functionality.

## Introduction
Consider a scenario where we are building a warehouse application designed to handle the shipping of various items. We have established a hierarchy of classes to represent the goods we have:
```java
class Item {}
class Book extends Item {}
class Furniture extends Item {}
class ElectronicItem extends Item {}
```
To implement shipping logic for each item, one might be tempted to add a `ship()` method directly to each `Item` class. While this is straightforward, it can lead to bloated classes filled with unrelated operations—such as shipping, storing, retrieving from a database, and rendering.

Instead of mixing these responsibilities, it’s better to keep items as primarily data classes and separate domain-specific logic from them. We can create an `Item_Shippable` class that acts as a `Shippable` extension (category) and provides a `ship()` method. This class must implement the `DelegateHolder` interface to allow it to work with items. Then we should subclass `Item_Shippable` and provide class extensions for each particular `Item` classes.
```java
class Item_Shippable implements ClassExtension.DelegateHolder<Item> {
    public ShippingInfo ship() {
        return …;
    }
}

class Book_Shippable extends Item_Shippable{
    public ShippingInfo ship() {
        return …;
    }
}
```
The core of the library is the `ClassExtension` class, which offers methods for dynamically finding and creating extension objects as needed. Using `ClassExtension`, shipping an item becomes as simple as calling `ClassExtension.extension(item, Item_Shippable.class).ship()`. Shipping a collection of items is equally straightforward:
```java
Item[] items = {
    new Book("The Mythical Man-Month"), 
    new Furniture("Sofa"), 
    new ElectronicItem(“Soundbar")
};

for (Item item : items) {
    ClassExtension.extension(item, Item_Shippable.class).ship();
}
```

## Details
All the extension classes must implement the `DelegateHolder` interface and must end with the name of an extension delimited by underscore e.g. `Book_Shippable` where `Book` is the name of the class and `Shippable` is the name of extension.

`ClassExtension` takes care of inheritance so it is possible to design and implement class extensions hierarchy that fully or partially resembles original classes hierarchy. If there's no explicit extension specified for particular class - its parent extension will be utilized. For example, if there's no explicit `Drawable` extension for `Oval` objects - base `Shape_Drawable` will be used instead.

Cashing of extension objects are supported out of the box. Cache utilizes weak references to release extension objects that are not in use. Though, to perform full cleanup either the `cacheCleanup()` should be used or automatic cleanup can be initiated via the `scheduleCacheCleanup()`. If automatic cache cleanup is used - it can be stopped by calling the `shutdownCacheCleanup()`.

## Adding to Your Build 
To add Java Class Extension library to your build:
* Add */dist/class-extension-x.x.x.jar* as classes
* Add */dist/class-extension-doc-x.x.x.jar* as documentation

## License
The Java Class Extension library is licensed under the terms of the [MIT License](https://opensource.org/license/mit).
