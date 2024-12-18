## Java Class Extension Library - Aspects
The Java Class Extension Library provides support of Aspects (AOP) by applying them to class extensions. This can be done by allowing to specify lambda functions which should be applied before, after or around some operations.

Common use cases for Aspects include:
* Logging and tracing
* Performance monitoring and profiling
* Security and access control
* Transaction management
* Exception handling and error management
* Caching
* Auditing and compliance
* Cross-cutting concerns in web and enterprise applications
* Device compatibility and data synchronization in mobile apps
* Quota management for APIs
* Retry mechanisms

Aspects allows developers to separate these concerns from core business logic, reducing code duplication and improving maintainability.

### Defining Aspects
The `AspectBuilder` enables defining Aspects for `ClassExtensions`. Create an instance using the `aspectBuilder()` method of `DynamicClassExtension` or `StaticClassExtension`. Use this builder to define Aspects for pointcuts specified by extension interfaces, object classes, and operations. Supports exact values and wildcards for flexible aspect definition.

```java
StaticClassExtension.sharedInstance().aspectBuilder().
        extensionInterface("*").
            operation("toString()").
                objectClass(Object.class).
                    before((operation, object, args) -> out.println("BEFORE: " + object + "-> toString()")).
                    after((result, operation, object, args) -> out.println("AFTER: result - " + result)).
                objectClass(Book.class).
                    before((operation,object, args) -> out.println("BOOK BEFORE: " + object + "-> toString()")).
                    after((result, operation, object, args) -> out.println("BOOK AFTER: result - " + result)).
                objectClass(AutoPart.class).
                    around((performer, operation, object, args) -> "ALTERED AUTO PART: " + applyDefault(performer, operation, object, args)).
build();
```
To utilize extensions with Aspects - just obtain extensions and use them as usual:

```java
Item[] items = {
        new Book("The Mythical Man-Month"),
        new Furniture("Sofa"),
        new ElectronicItem("Soundbar"),
        new AutoPart("Tire"),
};
for (Item item : items) {
    ShippableItemInterface extension = StaticClassExtension.sharedExtension(item, ShippableItemInterface.class);
    out.println(extension.toString());
}
```
If Aspects are used for debugging and testing purposes only - they can be turned OFF using the `aspectsEnabled` property of `ClassExtension`.

### Explicitly Defined Aspects in DynamicLCassExtension
The `DynamicClassExtension` provides an ability to define Aspects for certain operations. If defined, such aspects will supersede any aspects defined via `AspectBuilder`. It can be done via use of the `Builder.before()`, `Builder.after()` and `Builder.around` methods respectively.
```java
DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
        opName("ship").
            op(Book.class, shipBook(book)).
                before((operation object, args) -> LOGGER.info("BEFORE: " + object + "-> ship()")).
                after((result, operation, object, args) -> LOGGER.info("AFTER: result - " + result)).
        build();

Book book = new Book("The Mythical Man-Month");
dynamicClassExtension.extension(book, ItemShippable.class).ship();
```
**Notes:**
* Operations must be already defined first via `Builder.op()` or `Builder.voidOp()`
* Both synchronous and asynchronous operations are supported

Aspects are only supported for defined operations only. So if there is a need to intercept calls of usual methods - such methods should be dynamically "overridden" by defining operations with the same signature. The following example intersects `Object.toString()` method:
```java
DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().builder(Item_Shippable.class).
        opName("toString").
            op(Object.class, Object::toString).
                before((operation, object, args) -> LOGGER.info("BEFORE: " + object + "-> ship()")).
                after((result, operation, object, args) -> LOGGER.info("AFTER: result - " + result)).
        build();

Book book = new Book("The Mythical Man-Month");
dynamicClassExtension.extension(book, ItemShippable.class).toString();
```
### Ready to Use Advices

The Java Class Extension Library offers several ready to use Advices - lambda functions used to handle before, after or around conditions:
* `Aspects.LogBeforeAdvice` - adds logging of operation arguments
* `Aspects.LogAfterAdvice` - adds logging of operation results
* `Aspects.LogPerformTimeAdvice` - allows logging perform times for operations
* `Aspects.PropertyChangeAdvice` - allows tracking all property changes.

Next >> [Utilities](utilities.md)