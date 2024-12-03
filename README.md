# Java Class Extension Library
The Java Class Extension library provides an ability to emulate class extensions (categories) in Java. The library supports the following approaches:

1. **Static:** define and implement extensions as usual Java classes and then utilize the Java Class Extension library to find matching extension classes and create extension objects.
2. **Dynamic:** utilize the Java Class Extension library to define extensions by composing them as sets of lambda operations and let the library to create extensions dynamically on the fly.

Both approaches offer comparable performance, so the choice between them ultimately depends on several factors like implementation details, personal preferences, coding style, established habits and specific project requirements. Dynamic Class Extensions are generally simpler, as they only require adding new interfaces and defining operations via lambdas.
   
However, if the extension logic is sufficiently complex and necessitates coding new classes for extensions anyway, it may be worthwhile to consider using Static Class Extensions. Each approach has its strengths, and the best choice will depend on the particular context and needs of your project.

Both approaches leverage the `ClassExtension` interface, which facilitates querying for an extension based on an object's extension interface. Once obtained, these extensions unlock additional functionality with remarkable ease"
```java
Book book = new Book("The Mythical Man-Month");
Shippable shippable = StaticClassExtension.sharedExtension(book, Shippable.class);
shippable.ship();
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

1. It bypasses polymorphism, opting instead for an outdated imitation of it.
2. It violates SOLID principles, as introducing a new class (Item) requires modifications to existing code.
3. It is inconvenient and error-prone because the code can become scattered and duplicated throughout the project, potentially with varying content, making it difficult to locate and modify.
   
### Static Extensions with Java Class Extension Library

The core of the library is the `StaticClassExtension` class, which offers methods for dynamically finding and creating extension objects as needed. We can create an `Shippable` interface that defines new methods for a `Shippable` extension (category) and provides a `ship()` method. Then we should implement all needed extension classes which implement the `Shippable`interface and provide particular implementation for all `Shippable` methods. All those extension classes class must either implement the `DelegateHolder` interface to allow it to work with items or provide a constructor that takes an `Item` as a parameter.
```java
public interface Shippable {
    ShippingInfo ship();
}

class ItemShippable implements Shippable {
    public ItemShippable(Item item) {
	this.item = item;			
    }

    public ShippingInfo ship() {
        return …;
    }
    …
}

class BookShippable extends ItemShippable{
    public ShippingInfo ship() {
        return …;
    }
}
```
Using `StaticClassExtension`, shipping an item becomes as simple as calling:

```java
Book book = new Book("The Mythical Man-Month");
StaticClassExtension.sharedExtension(book, Shippable.class).ship();
``` 

Shipping a collection of items is equally straightforward:
```java
Item[] items = {
    new Book("The Mythical Man-Month"), 
    new Furniture("Sofa"), 
    new ElectronicItem(“Soundbar")
};

for (Item item : items) {
    StaticClassExtension.sharedExtension(item, Shippable.class).ship();
}
```
It is possible to further simplify things by adding an `extensionFor(Item)` helper method to the `Shippable`:
```java
public interface Shippable {
	public static Shippable extensionFor(Item item) {
    	    return StaticClassExtension.sharedExtension(item, Shippable.class).ship();
	}
  ...
}
```

With that helper method, shipping become even more simpler and shorter:
```java
Shippable.extensionFor(anItem).ship();
```
Supporting a new `Item` class using the Java Class Extension library requires:
1. Adding a new `Shippable` extension with a proper `ship()` implementation like `class GroceryItemShippable extends ItemShippable {...}.
2. Registeing a package for a new extension like `StaticClassExtension.sharedInstance().addExtensionPackage(Shippable.class, "test.grocery.shipment")`. 

No need to change any other code. That is it.

#### Details ####
All the static extension classes must:
1. Either:
* Implement the `DelegateHolder` interface. The `DelegateHolder.setDelegate(...)` method is used to supply extensions with objects to work with. Usually, it is fine to implement the `DelegateHolder` interface in a common root of some classes hierarchy.
* Provide a single parameter constructor that takes an object to work with as an argument.
3. Be named as a class name followed by an extension name e.g. `BookShippable` where `Book` is the name of the class and `Shippable` is the name of extension.

##### Inheritance Support #####
`StaticClassExtension` takes care of inheritance so it is possible to design and implement class extensions hierarchy that fully or partially resembles original classes' hierarchy. If there's no explicit extension specified for particular class - its parent extension will be utilized. For example, if there's no explicit `Shippable` extension for the `Toy` class - base `ItemShippable` will be used instead.

##### Cashing #####
Cashing of extension objects are supported out of the box and it can be controlled via the `Classextension.cacheEnabled` property. It utilizes weak references to release extension objects that are not in use. Though, to perform full cleanup either the `cacheCleanup()` should be used or automatic cleanup can be initiated via the `scheduleCacheCleanup()`. If automatic cache cleanup is used - it can be stopped by calling the `shutdownCacheCleanup()`.

### Dynamic Extensions with Java Class Extension Library
 Class `DynamicClassExtension` provides a way to emulate class extensions (categories) by composing extensions as a set of lambda operations. To specify an extension:
  
1. Create a `Builder` for an interface you want to compose an extension for by using the `DynamicClassExtension.sharedBuilder(...)` method
2. Specify the name of an operation using `Builder.opName(String)`
3. List all the method implementations per particular classes with lambdas using `Builder.op(...)` or `Builder.voidOp(...)`
5. Repeat 2, 3 for all operations
  
 For example, the following code creates `Shippable` extensions for `Item classes`. There are explicit `ship()` method implementations for all the `Item` classes. Though, the `log()` method is implemented for the `Item` class only so extensions for all the `Item` descendants will utilize the same `log()` method.
```java
  class Item {...}
  class Book extends Item {...}
  class Furniture extends Item {...}
  class ElectronicItem extends Item {...}
  class AutoPart extends Item {...}

  interface Shippable {
      ShippingInfo ship();
      void log(boolean isVerbose);
  }
  ...
 DynamicClassExtension.sharedBuilder(Shippable.class).
      nameOp("ship").
          op(Item.class, item -> ...).
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
Shippable shippable = DynamicClassExtension.sharedExtension(book, Shippable.class);
shippable.log(true);
shippable.ship();
```

Shipping a collection of items is equally straightforward:
```java
Item[] items = {
    new Book("The Mythical Man-Month"), 
    new Furniture("Sofa"), 
    new ElectronicItem(“Soundbar")
};

for (Item item : items) {
    DynamicClassExtension.sharedExtension(item, Shippable.class).ship();
}
```
Supporting a new `Item` class using the Java Class Extension library requires just adding the operations for that new `Item` class. No need to change any other code. That is it.

#### Details ####
For the most of the cases a shared instance of `DynamicClassExtension` should be used. But if there is a need to have different implementations of extensions in different places or domains it is possible to create and utilize new instances of `DynamicClassExtension`.

##### Inheritance and Polymorphism Support #####
`DynamicClassExtension` takes care of inheritance so it is possible to design and implement class extensions hierarchy that fully or partially resembles original classes' hierarchy. If there's no explicit extension operations specified for particular class - its parent extension will be utilized. For example, if there's no explicit extension operations defined for `AutoPart` objects - base `ship()` and `log(boolean)` operations specified for `Item` will be used instead.

Dynamic operations can override methods defined in the objects class. For example, if you add a `toString` operation to the `AutoPart` class - it will override the `toString()` method defined in the `Object` class.

Objects and extensions can be utilized uniformly as similar objects if they implement the same base interfaces. For example, if both `Item` and `Item_Shippable` implements(extends) the same `ItemInterface` interface having the `getName()` method - both items and their extensions can use that method with the same results.

```java
interface ItemInterface {
    String getName();
} 		
class Item implements ItemInterface {...}
class Book extends Item {...}
class Furniture extends Item {...}
class ElectronicItem extends Item {...}
class AutoPart extends Item {...}

interface Item_Shippable extends ItemInterface{
    ShippingInfo ship();
    void log(boolean isVerbose);
}
...
Book book = new Book("The Mythical Man-Month");
System.out.println(book.getName());
System.out.println(DynamicClassExtension.sharedExtension(book, Item_Shippable.class).getName());
```

##### Cashing #####
Cashing of extension objects are supported out of the box and it can be controlled via the `Classextension.cacheEnabled` property. Cache utilizes weak references to release extension objects that are not in use. Though, to perform full cleanup either the `cacheCleanup()` should be used or automatic cleanup can be initiated via the `scheduleCacheCleanup()`. If automatic cache cleanup is used - it can be stopped by calling the `shutdownCacheCleanup()`.

##### Validation #####
`DynamicClassExtension` offers a capability to validate extensions for a given class through its `checkValid(...)` method. This feature is particularly valuable for testing purposes. An extension is deemed valid when corresponding operations are registered for all its methods. However, in certain scenarios, it's desirable to maintain extension validity while supporting only a subset of operations. This flexibility can be achieved by annotating specific methods in the extension interface with `@OptionalMethods` annotation.

##### Limitations #####
The following are limitations of `DynamicClassExtension`:
1. Overloaded operations are not supported yet. So for example, it is not possible to define both `log(boolean)` and `log(String)` operations
2. Operations having more than one parameter are not supported yet.

## Adding to Your Build 
To add Java Class Extension library to your build:
* Add */dist/class-extension-x.x.x.jar* as classes
* Add */dist/class-extension-doc-x.x.x.jar* as documentation

## License
The Java Class Extension library is licensed under the terms of the [MIT License](https://opensource.org/license/mit).
