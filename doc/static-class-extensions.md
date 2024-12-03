## Java Class Extensions Library - Static Class Extensions

The core of the library is the `StaticClassExtension` class, which offers methods for dynamically finding and creating extension objects as needed. We can create an `Shippable` interface that defines new methods for a `Shippable` extension (category) and provides a `ship()` method. Then we should implement all needed extension classes which implement the `Shippable`interface and provide particular implementation for all `Shippable` methods. All those extension classes must either implement the `DelegateHolder` interface to allow it to work with items or provide a constructor that takes an `Item` as a parameter.
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
2. Registeing a package for a new extension (if the extension recides in a different package than the extension interface) like `StaticClassExtension.sharedInstance().addExtensionPackage(Shippable.class, "test.grocery.shipment")`.

No need to change any other code. That is it.

### Details ###
All the static extension classes must:
1. Either:
* Implement the `DelegateHolder` interface. The `DelegateHolder.setDelegate(...)` method is used to supply extensions with objects to work with. Usually, it is fine to implement the `DelegateHolder` interface in a common root of some classes hierarchy.
* Provide a single parameter constructor that takes an object to work with as an argument.
2. Be named as a class name followed by an extension name e.g. `BookShippable` where `Book` is the name of the class and `Shippable` is the name of extension.

#### Inheritance Support ####
`StaticClassExtension` takes care of inheritance so it is possible to design and implement class extensions hierarchy that fully or partially resembles original classes' hierarchy. If there's no explicit extension specified for particular class - its parent extension will be utilized. For example, if there's no explicit `Shippable` extension for the `Toy` class - base `ItemShippable` will be used instead.

#### Cashing ####
Cashing of extension objects are supported out of the box and it can be controlled via the `Classextension.cacheEnabled` property. It utilizes weak references to release extension objects that are not in use. Though, to perform full cleanup either the `cacheCleanup()` should be used or automatic cleanup can be initiated via the `scheduleCacheCleanup()`. If automatic cache cleanup is used - it can be stopped by calling the `shutdownCacheCleanup()`.

Next >> [Dynamic Class Extensions](dynamic-class-extensions.md)