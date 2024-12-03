## Java Class Extensions Library - Static Class Extensions

There is the `StaticClassExtension` class, which offers methods for dynamically finding and creating extension objects as needed. With this approach you should define and implement extensions as usual Java classes and then utilize the Java Class Extension library to find matching extension classes and create extension objects.

For example, we can create an `Shippable` interface that defines new methods for a `Shippable` extension (category) and provides a `ship()` method. Then we should implement all needed extension classes which implement the `Shippable`interface and provide particular implementation for all `Shippable` methods. _Note: All those extension classes must either implement the `DelegateHolder` interface to used to supply extensions with items to work with or provide a constructor that takes an `Item` as a parameter._
```java
public interface Shippable {
    ShippingInfo ship();
}

class ItemShippable implements Shippable {
    public ItemShippable(Item item) {
        this.item = item;			
    }

    public ShippingInfo ship() {
        return new ShippingInfo("done");
    }
    …
}

class BookShippable extends ItemShippable{
    public ShippingInfo ship() {
        return new ShippingInfo("done");
    }
}
```
Using `StaticClassExtension`, shipping an item becomes as simple as calling:

```java
Book book = new Book("The Mythical Man-Month");
StaticClassExtension.sharedExtension(book, Shippable.class).ship();
``` 

Shipping a collection of items is equally straightforward:
```java
Item[] items = {
    new Book("The Mythical Man-Month"), 
    new Furniture("Sofa"), 
    new ElectronicItem(“Soundbar")
};

for (Item item : items) {
    StaticClassExtension.sharedExtension(item, Shippable.class).ship();
}
```
It is possible to further simplify things by adding an `extensionFor(Item)` helper method to the `Shippable`:
```java
public interface Shippable {
	public static Shippable extensionFor(Item item) {
    	    return StaticClassExtension.sharedExtension(item, Shippable.class).ship();
	}
  ...
}
```

With that helper method, shipping become even more simpler and shorter:
```java
Shippable.extensionFor(anItem).ship();
```
Supporting a new `Item` class using the Java Class Extension library requires:
1. Adding a new `Shippable` extension with a proper `ship()` implementation like `class GroceryItemShippable extends ItemShippable {...}`.
2. Registering a package for a new extension (if the extension resides in a different package than the extension interface) like `StaticClassExtension.sharedInstance().addExtensionPackage(Shippable.class, "test.grocery.shipment")`.

No need to change any other code. That is it.

### Details
The following are requirements for all the static extension classes:
1. **Naming Convention:** they must be named as _\[ClassName]\[ExtensionName]_ - a class name followed by an extension name. For example, `BookShippable` where `Book` is the name of the class and `Shippable` is the name of extension.
2. **Delegate Management:** they must provide a way for `StaticClassExtension`to supply delegate objects to work with. It can be done either:
* By providing a single parameter constructor that takes a delegate object to work with as an argument.
* By implementing the `DelegateHolder` interface. The `DelegateHolder.setDelegate(Object)` method is used to supply extensions with delegate objects to work with. Usually, it is fine to implement the `DelegateHolder` interface in a common root of some classes' hierarchy.

By default, `StaticClassExtension` searches for extension classes in the same package as the extension interface. To register extension classes located in different packages, use the `addExtensionPackage(Class, String)` method of `StaticClassExtension`. For example:
```java
StaticClassExtension.sharedInstance().addExtensionPackage(Shippable.class, "test.toys.shipment");
```

### Flexible Extension Interface Handling
The `StaticClassExtension` enables powerful interface combining (merging), allowing extensions to be treated as their original objects. As result, extensions become transparent, behaving like original objects while providing additional functionality. 

For example, when working with the `ItemShippableInterface` extension, you can:
* Interact with it as with an original item (e.g. use its `getName()` method)
* Simultaneously use its shipping-related capabilities (e.g. use its `ship()` method)
 
```java
interface ItemInterface {
    String getName();
}

class Item implements ItemInterface {
    …
}
 …

@ExtensionInterface
interface Shippable {
    ShippingInfo ship();
}

interface ItemShippableInterface extends ItemInterface, Shippable {
}

```java
Book book = new Book("The Mythical Man-Month");
ItemShippableInterface itemShippable = StaticClassExtension.sharedExtension(book,
        ItemShippableInterface.class);
System.out.println(itemShippable.getName());
itemShippable.ship();
```
**Note:** To ensure proper functionality of `StaticClassExtension`, the `Shippable` interface must be annotated with `@ExtensionInterface`. This annotation enables `StaticClassExtension` to correctly locate and utilize extension classes (e.g., `BookShippable`).

#### Inheritance and Polymorphism Support
`StaticClassExtension` effectively manages inheritance, allowing you to design class extension hierarchies that mirror the original class hierarchy, either fully or partially. This feature promotes flexibility in extension management, ensuring that all classes benefit from available extensions without requiring redundant definitions. If a specific extension is not defined for a class, the extension from one of its parent classes will be utilized.

For example, if the `Toy` class does not have an explicit `Shippable` extension, the base `ItemShippable` extension will be applied instead.

#### Cashing
Cashing of extension objects are supported out of the box and it can be controlled via the `Classextension.cacheEnabled` property. It utilizes weak references to release extension objects that are not in use. Though, to perform full cleanup either the `cacheCleanup()` should be used or automatic cleanup can be initiated via the `scheduleCacheCleanup()`. If automatic cache cleanup is used - it can be stopped by calling the `shutdownCacheCleanup()`.

Next >> [Dynamic Class Extensions](dynamic-class-extensions.md)