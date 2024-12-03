## Java Class Extensions Library - Dynamic Class Extensions
***
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
  
  static {
      DynamicClassExtension.sharedBuilder(Shippable.class).
      nameOp("ship").
            op(Item.class, item -> ...).
            op(Book.class, book -> ...).
            op(Furniture.class, furniture -> ...).
            op(ElectronicItem.class, electronicItem -> ...).
      nameOp("log").
            voidOp(Item.class, (Item item, Boolean isVerbose) -> {...}).
      build();
  }
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
    new ElectronicItem("Soundbar")
};

for (Item item : items) {
    DynamicClassExtension.sharedExtension(item, Shippable.class).ship();
}
```
Supporting a new `Item` class using the Java Class Extension library requires just adding the operations for that new `Item` class. No need to change any other code that does shipping with help of `Shippable` interface. That is it.

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

### Details
For the most of the cases a shared instance of `DynamicClassExtension` should be used. But if there is a need to have different implementations of extensions in different places or domains it is possible to create and utilize new instances of `DynamicClassExtension`.

#### Inheritance and Polymorphism Support
`DynamicClassExtension` takes care of inheritance so it is possible to design and implement class extensions hierarchy that fully or partially resembles original classes' hierarchy. If there's no explicit extension operations specified for particular class - its parent extension will be utilized. For example, if there's no explicit extension operations defined for `AutoPart` objects - base `ship()` and `log(boolean)` operations specified for `Item` will be used instead.

Dynamic operations can override methods defined in the objects class. For example, if you add a `toString` operation to the `AutoPart` class - it will override the `toString()` method defined in the `Object` class.

Objects and extensions can be utilized uniformly as similar objects if they implement the same base interfaces. For example, if both `Item` and `ItemShippable` implements(extends) the same `ItemInterface` interface having the `getName()` method - both items and their extensions can use that method with the same results.

```java
interface ItemInterface {
    String getName();
} 		
class Item implements ItemInterface {...}
class Book extends Item {...}
class Furniture extends Item {...}
class ElectronicItem extends Item {...}
class AutoPart extends Item {...}

interface ItemShippable extends ItemInterface{
    ShippingInfo ship();
    void log(boolean isVerbose);
}
...
Book book = new Book("The Mythical Man-Month");
System.out.println(book.getName());
System.out.println(DynamicClassExtension.sharedExtension(book, ItemShippable.class).getName());
```

#### Cashing
Cashing of extension objects are supported out of the box and it can be controlled via the `Classextension.cacheEnabled` property. Cache utilizes weak references to release extension objects that are not in use. Though, to perform full cleanup either the `cacheCleanup()` should be used or automatic cleanup can be initiated via the `scheduleCacheCleanup()`. If automatic cache cleanup is used - it can be stopped by calling the `shutdownCacheCleanup()`.

#### Validation
`DynamicClassExtension` offers a capability to validate extensions for a given class through its `checkValid(...)` method. An extension is deemed valid when corresponding operations are registered for all its methods. However, in certain scenarios, it's desirable to maintain extension validity while supporting only a subset of operations. This flexibility can be achieved by annotating specific methods in the extension interface with `@OptionalMethods` annotation.

This feature is especially useful for testing, as it simplifies the process of detecting discrepancies. When new methods are added to an interface, it becomes easy to identify cases where corresponding operations have not been registered with `DynamicClassExtension`.

#### Limitations
The following are limitations of `DynamicClassExtension`:
1. Overloaded operations are not supported yet. So for example, it is not possible to define both `log(boolean)` and `log(String)` operations
2. Operations having more than one parameter are not supported yet.
