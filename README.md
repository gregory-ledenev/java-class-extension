# Java Class Extension Library
The Java Class Extension library provides an ability to mimic class extensions (categories) in Java. The library supports the following approaches:

1. **Static:** define and implement extensions as usual Java classes and then utilize the Java Class Extension library to find matching extension classes and create extension objects.
2. **Dynamic:** utilize the Java Class Extension library to define extensions by composing them as sets of lambda operations and let the library to create extensions dynamically on the fly.

After getting extensions they can be used to perform any extended functionality as easy as:
```java
Book book = new Book("The Mythical Man-Month");
Item_Shippable itemShippable = ClassExtension.extension(book, Item_Shippable.class);
itemShippable.ship();
```
Java Class Extension library provides a valuable alternative for class extensions (not supported in Java) with just a little more verbose code and little more complex implementation.
   
## Introduction
Consider a scenario where we are building a warehouse application designed to handle the shipping of various items. We have established a hierarchy of classes to represent the goods we have:
```java
class Item {…}
class Book extends Item {…}
class Furniture extends Item {…}
class ElectronicItem extends Item {…}
```
To implement shipping logic for each item, one might be tempted to add a `ship()` method directly to each `Item` class. While this is straightforward, it can lead to bloated classes filled with unrelated operations—such as shipping, storing, retrieving from a database, and rendering.

Instead of mixing these responsibilities, it’s better to keep items as primarily data classes and separate domain-specific logic from them. 

### Functional Style

To ship an item, we could define a method that uses a modern switch statement with pattern matching:
```java
public ShippingInfo ship(Item item) {
    return switch (item) {
        case Book book -> shipBook(book);
        case Furniture furniture -> shipFurniture(furniture);
        case ElectronicItem electronicItem -> shipElectronicItem(electronicItem);
    };
}
```
While this method is simple and direct, it comes with several disadvantages:

1. Breaks OOP Principles: They mimic polymorphism in an outdated style.
2. Violates SOLID Principles: Adding a new Item class necessitates changes in the shipping logic.
3. Inconvenient and Error-Prone: Without dedicated ship() methods, shipping logic can become scattered and duplicated across the codebase, making it hard to alter it.

### Static Extensions with Java Class Extension Library

The core of the library is the `ClassExtension` class, which offers methods for dynamically finding and creating extension objects as needed. We can create an `Item_Shippable` class that acts as a `Shippable` extension (category) and provides a `ship()` method. This class must implement the `DelegateHolder` interface to allow it to work with items. Then we should subclass `Item_Shippable` and provide class extensions for each particular `Item` classes.
```java
class Item_Shippable implements ClassExtension.DelegateHolder<Item> {
    public ShippingInfo ship() {
        return …;
    }
    …
}

class Book_Shippable extends Item_Shippable{
    public ShippingInfo ship() {
        return …;
    }
}
```
Using `ClassExtension`, shipping an item becomes as simple as calling:

```java
Book book = new Book("The Mythical Man-Month");
ClassExtension.extension(book, Item_Shippable.class).ship()
``` 

Shipping a collection of items is equally straightforward:
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
It is possible to further simplify things by adding an `extensionFor(Item)` helper method to the `Item_Shippable`:
```java
public static class Item_Shippable implements ClassExtension.DelegateHolder<Item> {
	public static Item_Shippable extensionFor(Item anItem) {
    	return ClassExtension.extension(anItem, Item_Shippable.class);
	}
  ...
}
```

With that helper method, shipping become even more simpler and shorter:
```java
Item_Shippable.extensionFor(anItem).ship()
```
Supporting a new Item class using the Java Class Extension library requires just adding a new Shippable extension with a proper `ship()` implementation. No need to change any other code. That is it.

#### Details ####
All the static extension classes must:
1. Implement the `DelegateHolder` interface. The `DelegateHolder.setDelegate(...)` method is used to supply extensions with objects to work with.
2. Be named as a class name followed by an extension name delimited by underscore e.g. `Book_Shippable` where `Book` is the name of the class and `Shippable` is the name of extension.

`ClassExtension` takes care of inheritance so it is possible to design and implement class extensions hierarchy that fully or partially resembles original classes' hierarchy. If there's no explicit extension specified for particular class - its parent extension will be utilized. For example, if there's no explicit `Shippable` extension for the `Toy` class - base `Item_Shippable` will be used instead.

Cashing of extension objects are supported out of the box. Cache utilizes weak references to release extension objects that are not in use. Though, to perform full cleanup either the `cacheCleanup()` should be used or automatic cleanup can be initiated via the `scheduleCacheCleanup()`. If automatic cache cleanup is used - it can be stopped by calling the `shutdownCacheCleanup()`.

### Dynamic Extensions with Java Class Extension Library
 Class `DynamicClassExtension` provides a way to mimic class extensions (categories) by composing extensions as a set of lambda operations. To specify an extension:
  
1. Create a `Builder` for an interface you want to compose an extension by using the `DynamicClassExtension.sharedBuilder(...)` method
2. Specify the name of an operation using `Builder.opName(String)`
3. List all the method implementations per particular classes with lambdas using `Builder.op(...)` or `Builder.voidOp(...)`
5. Repeat 2, 3 for all operations
  
 For example, the following code creates `Item_Shippable` extensions for `Item classes`. There are explicit `ship()` method implementations for all the `Item` classes. Though, the `log()` method is implemented for the `Item` class only so extensions for all the `Item` descendants will utilize the same `log()` method.
```java
  class Item {...}
  class Book extends Item {...}
  class Furniture extends Item {...}
  class ElectronicItem extends Item {...}
  class AutoPart extends Item {...}

  interface Item_Shippable {
      ShippingInfo ship();
      void log(boolean isVerbose);
  }
  ...
 DynamicClassExtension.sharedBuilder(Item_Shippable.class).
      nameOp("ship").
          op(Item.class, book -> ...).
          op(Book.class, book -> ...).
          op(Furniture.class, furniture -> ...).
          op(ElectronicItem.class, electronicItem -> ...).
      nameOp("log").
          voidOp(Item.class, (Item item, Boolean isVerbose) -> {...}).
      build();
```

Finding an extension and calling its methods is simple and straightforward:
```java
Book book = new Book("The Mythical Man-Month");
Item_Shippable itemShippable = DynamicClassExtension.sharedExtension(book,
	Item_Shippable.class);
itemShippable.log(true);
itemShippable.ship();
```

Shipping a collection of items is equally straightforward:
```java
Item[] items = {
    new Book("The Mythical Man-Month"), 
    new Furniture("Sofa"), 
    new ElectronicItem(“Soundbar")
};

for (Item item : items) {
    DynamicClassExtension.sharedExtension(item, Item_Shippable.class).ship();
}
```
Supporting a new `Item` class using the Java Class Extension library requires just adding the operations for that new `Item` class. No need to change any other code. That is it.

#### Details ####
For the most of the cases a shared instance of `DynamicClassExtension` should be used. But if there is a need to have different implementations of extensions in different places or domains it is possible to create and utilize new instances of `DynamicClassExtension`.

`DynamicClassExtension` takes care of inheritance so it is possible to design and implement class extensions hierarchy that fully or partially resembles original classes' hierarchy. If there's no explicit extension operations specified for particular class - its parent extension will be utilized. For example, if there's no explicit extension operations defined for `AutoPart` objects - base `ship()` and `log(boolean)` operations specified for `Item` will be used instead.

Cashing of extension objects are supported out of the box. Cache utilizes weak references to release extension objects that are not in use. Though, to perform full cleanup either the `cacheCleanup()` should be used or automatic cleanup can be initiated via the `scheduleCacheCleanup()`. If automatic cache cleanup is used - it can be stopped by calling the `shutdownCacheCleanup()`.

## Adding to Your Build 
To add Java Class Extension library to your build:
* Add */dist/class-extension-x.x.x.jar* as classes
* Add */dist/class-extension-doc-x.x.x.jar* as documentation

## License
The Java Class Extension library is licensed under the terms of the [MIT License](https://opensource.org/license/mit).
