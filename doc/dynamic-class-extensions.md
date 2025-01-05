## Java Class Extension Library - Dynamic Class Extensions

Class `DynamicClassExtension` provides a way to emulate class extensions (categories) by composing extensions as a set of lambda operations. To specify an extension:

1. Create a `Builder` for an interface you want to compose an extension for by using the `DynamicClassExtension.sharedBuilder(...)` method
2. Specify the name of an operation using `Builder.opName(String)`
3. List all the method implementations per particular classes with lambdas using `Builder.operation(...)` or `Builder.voiOperation(...)`
4. Repeat 2, 3 for all operations

For example, the following code creates `Shippable` extensions for `Item classes`. There are explicit `ship()` method implementations for all the `Item` classes. Though, the `log()` method is implemented for the `Item` class only so extensions for all the `Item` descendants will utilize the same `log()` method.
```java
interface ItemInterface {...}

class Item implements ItemInterface {...}
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
  operationName("ship").
        operation(Item.class, item -> ...).
        operation(Book.class, book -> ...).
        operation(Furniture.class, furniture -> ...).
        operation(ElectronicItem.class, electronicItem -> ...).
  operationName"log").
        voidOperation(Item.class, (Item item, Boolean isVerbose) -> {...}).
  build();
}
```
**Note:** `operation()` and `voidOperation()` builder methods support operations having no or single parameters only. So to represent an operation having more than one parameter - declare a lambda taking an array of objects as an argument:
```java
interface MultipleParameters {
    String[] arrayParameter(String[] anArray);
    Object[] multipleParameters(int p1, String p2, String p3);
}

static DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
        builder(MultipleParameters.class).
            operationName("arrayParameter").
                operation(Object.class, (Object a1, String[] a2) -> a2).
            operationName("multipleParameters").
                operation(Object.class, (Object a1, Object[] a2) -> a2).
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
    new ElectronicItem("Soundbar")
};

for (Item item : items) {
    DynamicClassExtension.sharedExtension(item, Shippable.class).ship();
}
```
Supporting a new `Item` class using the Java Class Extension library requires just adding the operations for that new `Item` class. No need to change any other code that does shipping with help of `Shippable` interface. That is it.

### Details
For the most of the cases a shared instance of `DynamicClassExtension` should be used. But if there is a need to have different implementations of extensions in different places or domains it is possible to create and utilize new instances of `DynamicClassExtension`.

**Note:** Extensions returned by `DynamicClassExtension` do not directly correspond to certain classes themselves. Therefore, it is crucial not to cast these extensions. Instead, always utilize only the methods provided by the extension interface. For example, an extension obtained for the `ItemShippable` interface that combines both `Shippable` and `ItemInterface` can not be cast to the `Item`.

If you need to check that an extension represents a particular object you may use the `ClassExtension.equals(Object, Object)` method:
```java
Book book = new Book("The Mythical Man-Month");
Shippable extension = Shippable.extensionFor(book);
assertTrue(ClassExtension.equals(book, extension));
```

If you need to get a delegate object for an extension you may use the `ClassExtension.getDelegate()` method:
```java
Book book = new Book("The Mythical Man-Month");
Shippable shippable = Shippable.extensionFor(book);
assertSame(book, ClassExtension.getDelegate(shippable));
```

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
#### Composition Support
`DynamicClassExtension` enables dynamic composition of multiple interfaces, complementing static inheritance. Key features:
* Combines several interfaces at runtime
* Useful when static composition is impractical or impossible
* Implemented via the `extension(...)` method with a list of supplemental interfaces
* Allows flexible usage of the extensions

For example: you can get an extension for a composition of `Shippable` and `ItemInterface` and use it primarily for shipping activities and secondary as an `ItemInterface` object itself (e.g. a `Book`) via type casting.

This approach offers greater flexibility in object composition and usage compared to static inheritance alone.
```java
Book book = new Book("The Mythical Man-Month");
Shippable shippable = DynamicClassExtension.sharedExtension(book, Shippable.class, ItemInterface.class);
shippable.ship(); // use it for shipping
out.println(((ItemInterface) shippable).getName()); // use it as a Book itself

```

#### Unions Support
Dynamic Extensions provide a powerful mechanism to unify objects of different, unrelated types under a common interface. This approach is particularly useful when dealing with objects that lack a shared superclass or interface.

Consider a scenario where we need to add shipping functionality to various item types:

```java
public record Book(String name) {}
public record Furniture(String name) {}
public record ElectronicItem(String name) {}
public record AutoPart(String name) {}
```
We can introduce a Shippable interface to act as a unifying abstraction:

```java
public record ShippingInfo(String result) {}

public interface Shippable {
    String name();
    ShippingInfo ship();
}
```

Then we should configure Dynamic Extensions for the Shippable interface.

```java
static {
    DynamicClassExtension.sharedBuilder().extensionInterface(Shippable.class).
    operationName("ship").
        operation(Book.class, book -> shipBook(book)).
        operation(Furniture.class, furniture -> shipFurniture(furniture)).
        operation(ElectronicItem.class, electronicItem -> shipElectronicItem(electronicItem)).
        operation(AutoPart.class, electronicItem -> shipAutoPart(autoPart)).
    operationName("name").
        operation(Object.class, (object) -> DynamicClassExtension.performOperation("name", object)).
    build();
}
```
Use the `DynamicClassExtension.performOperation(String, Object, Object...)` method to handle common operations across different object types using reflection. For example, to manage the "name" operation, specify `Object.class` as a common superclass and retrieve names uniformly:
```java
DynamicClassExtension.sharedBuilder().extensionInterface(Shippable.class).
// ... other operations ...
    operationName("name").
        operation(Object.class, (object) -> DynamicClassExtension.performOperation("name", object)).
build();
```

This approach promotes code reusability and flexibility, allowing you to handle multiple types with a single operation while ensuring consistent behavior.

Shipping a collection of items is straightforward as usual:

```java
Object[] items = {
    new Book("The Mythical Man-Month"), 
    new Furniture("Sofa"), 
    new ElectronicItem("Soundbar")
};

for (Object item : items) {
    Shippable shippable = DynamicClassExtension.sharedExtension(item, Shippable.class);
    System.out.println("Shipping: " + shippable.name());
    shippable.ship();
}
```
**Key Benefits**
* Type Unification: Enables handling of disparate types through a common interface.
* Flexibility: Easily extend functionality without modifying existing classes.
* Code Reusability: Implement shared behavior across unrelated types.
* Maintainability: Centralized configuration of extensions for better management.

**Considerations**
* Ensure proper error handling for unsupported types.
* Consider performance implications for large-scale applications.
* Maintain clear documentation of the extension configurations.

This approach demonstrates the power of Dynamic Extensions in creating flexible, maintainable systems that can adapt to diverse object types without extensive refactoring.

#### Asynchronous Operations
It is possible to use `Builder.async()` to declaratively define asynchronous operations. Such operations are running in background, and they are non-blocking therefore caller threads continue immediately.

```java
DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
        operationName("ship").
            operation(Book.class, shipBook(book)).async().
        build();

Book book = new Book("The Mythical Man-Month");
dynamicClassExtension.extension(book, ItemShippable.class).ship();
```
**Notes:**
* Operations must be already defined first via the `Builder.operation()` or `Builder.voidoperation()` methods
* Non-void operations return `0` or `null` instantly depending on the operation return type
* Extension usage mirrors synchronous operations
* Ideal for long-running tasks to improve responsiveness

If there is a need to handle results of such asynchronous operations it can be done by specifying a lambda function as an argument for `Builder.async()`.
```java
DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
        operationName("ship").
            operation(Book.class, shipBook(book)).
                async((Book book, Throwable ex) -> System.out.println("Book shipped: " + book)).
        build();

Book book = new Book("The Mythical Man-Month");
dynamicClassExtension.extension(book, ItemShippable.class).ship();
```

#### Altering Operations

To alter an operation itself: 
1. Remove it first using the `Builder.removeoperation(...)` method
2. Add a replacement operation using one of `Builder.operation(...)` or `Builder.voidoperation(...)` methods

```java
DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
        operationName("toString").
            removeOperation(Object.class,new Class<?>[0]).
                operation(Object.class, o -> "result: " + o.tostring()).
        build();
```

To alter properties of an operation:
1. Make an alteration intention for the operation using the `Builder.alteroperation(...)` method
2. Specify properties for the operation e.g. by using the `Builder.async(...)` method
```java
DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
        operationName("ship").
            alterOperation(Fuurniture.class,new Class<?>[0]).
                async().
        build();
```

#### Cashing
Cashing of extension objects are supported out of the box, and it can be controlled via the `Classextension.cacheEnabled` property. Cache utilizes weak references to release extension objects that are not in use. Though, to perform full cleanup either the `cacheCleanup()` should be used or automatic cleanup can be initiated via the `scheduleCacheCleanup()`. If automatic cache cleanup is used - it can be stopped by calling the `shutdownCacheCleanup()`.

#### Validation
`DynamicClassExtension` offers a capability to validate extensions for a given class through its `checkValid(...)` method. An extension is deemed valid when corresponding operations are registered for all its methods. However, in certain scenarios, it's desirable to maintain extension validity while supporting only a subset of operations. This flexibility can be achieved by annotating specific methods in the extension interface with `@OptionalMethods` annotation.

This feature is especially useful for testing, as it simplifies the process of detecting discrepancies. When new methods are added to an interface, it becomes easy to identify cases where corresponding operations have not been registered with `DynamicClassExtension`.

#### Limitations
The following are limitations of `DynamicClassExtension`:
1. Overloaded operations are not supported yet. So for example, it is not possible to define both `log(boolean)` and `log(String)` operations
2. Operations having more than one parameter are supported by passing all the arguments as an array of objects.
3. Dynamic nature of the operations prevents detecting some errors at compile time so be careful during refactoring of extension interfaces and check operations handling after any refactorings. It is recommended to mark any extension interfaces with `@ExtensionInterface` annotation to let developers know that they should check and test dynamic operations after any refactorings.

Next >> [Aspects](aspects.md)
