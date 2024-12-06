## Java Class Extensions Library - Static Class Extensions

There is the `StaticClassExtension` class, which offers methods for dynamically finding and creating extension objects as needed. With this approach you should define and implement extensions as usual Java classes and then utilize the Java Class Extension library to find matching extension classes and create extension objects using the `extension(Object Class)` method.

For example, we can create a `Shippable` interface that defines new methods for a `Shippable` extension (category) and provides a `ship()` method. Then we should implement all needed extension classes which implement the `Shippable`interface and provide particular implementation for all `Shippable` methods. _Note: All those extension classes must either implement the `DelegateHolder` interface to used to supply extensions with items to work with or provide a constructor that takes an `Item` as a parameter._
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
### Details
The following are requirements for all the static extension classes:
1. **Naming Convention:** they must be named as _\[ClassName]\[ExtensionName]_ - a class name followed by an extension name (extension interface name). For example, `BookShippable` where `Book` is the name of the class and `Shippable` is the name of extension.
2. **Delegate Management:** they must provide a way for `StaticClassExtension`to supply delegate objects to work with. It can be done either:
* By providing a single parameter constructor that takes a delegate object to work with as an argument.
* By implementing the `DelegateHolder` interface. The `DelegateHolder.setDelegate(Object)` method is used to supply extensions with delegate objects to work with. Usually, it is fine to implement the `DelegateHolder` interface in a common root of some classes' hierarchy.

By default, `StaticClassExtension` searches for extension classes in the same package as the extension interface. To register extension classes located in different packages, use the `addExtensionPackage(Class, String)` method of `StaticClassExtension`. For example:
```java
StaticClassExtension.sharedInstance().addExtensionPackage(Shippable.class, "test.toys.shipment");
```
**Note:** Extensions returned by `StaticClassExtension` may not directly correspond to the extension classes themselves. Therefore, it is crucial not to cast these extensions. Instead, always utilize only the methods provided by the extension interface.

If you need to check that an extension represents a particular object you may use the `ClassExtension.equals(Object, Object)` method:
```java
Book book = new Book("The Mythical Man-Month");
Shippable shippable = Shippable.extensionFor(book);
assertTrue(ClassExtension.equals(book, shippable));
```

If you need to get a delegate object for an extension you may use the `ClassExtension.getDelegate()` method:
```java
Book book = new Book("The Mythical Man-Month");
Shippable shippable = Shippable.extensionFor(book);
assertSame(book, ClassExtension.getDelegate(shippable));
```

#### Adding New Extension Classes
To support new extension classes:
1. Code all the required classes that implement desired extension interface.
2. Register packages for new extensions if new classes reside in the packages not known to `StaticClassExtension`

For example:
```java
class GroceryItemShippable extends ItemShippable {
    public ShippingInfo ship() {
        return new ShippingInfo("done");
    }
}

static {
    StaticClassExtension.sharedInstance().addExtensionPackage(Shippable.class, "test.grocery.shipment");
}

```
No need to touch or change any existing code. That is it.

#### Instantiation Strategy
The `StaticClassExtension` offers two extension instantiation strategies - Proxy and Direct.

#### Proxy {#instantiation-strategy-proxy}
The `StaticClassExtension` returns dynamic proxies with extension instances inside. For example: a `Shippable` proxy containing a `BookShippable` instance under the hood. This strategy offers the following benefits:
   * Enables interface combining (merging)
   * Allows extensions to be treated as original objects (delegates)
   * Facilitates method call tracking in verbose mode

#### Direct 
The `StaticClassExtension`  returns extension instances directly. For example: a `BookShippable` instance. This strategy offers much faster performance than the Proxy strategy. So use Direct instantiation when proxy-related features are not needed and performance is critical.

The @ExtensionInterface annotation controls the instantiation strategy. 

#### Extension Interface Annotation
The optional `@ExtensionInterface` annotation allows developers to mark interfaces as extension interfaces. The `StaticClassExtension` utilizes this annotation to:
1. Compose Class Names: dynamically generate appropriate extension class names.
2. Package Discovery: specify packages for searching extension classes using the `packages` parameter.
3. Instantiation Strategy: decide between using dynamic proxies or direct class extensions via the `instantiationStrategy` parameter.

#### Flexible Extension Interface Handling
When using dynamic proxies, the `StaticClassExtension` enables powerful interface combining (merging), allowing extensions to be treated as their original objects. As result, extensions become transparent, behaving like original objects while providing additional functionality. 

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
Cashing of extension objects are supported out of the box, and it can be controlled via the `Classextension.cacheEnabled` property. It utilizes weak references to release extension objects that are not in use. Though, to perform full cleanup either the `cacheCleanup()` should be used or automatic cleanup can be initiated via the `scheduleCacheCleanup()`. If automatic cache cleanup is used - it can be stopped by calling the `shutdownCacheCleanup()`.

Next >> [Dynamic Class Extensions](dynamic-class-extensions.md)
