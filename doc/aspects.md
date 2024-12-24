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

#### Altering
It is possible to remove already defined aspects using the `remove()` method specifying which types of advices should be removed. To remove specific advices only - specify them with identifiers and then use that identifiers in `remove()` methods.

```java
dynamicClassExtension.aspectBuilder().
            extensionInterface(ItemInterface.class).
                objectClass(Item.class).
                    operation("*").
                        remove(AdviceType.BEFORE).
        build();
```

It is possible to enable/disable already defined aspects using the `enabled()` method specifying which types of advices should be enabled/disabled. To enable/disable specific advices only - specify them with identifiers and then use that identifiers in `enabled()` methods.
```java
dynamicClassExtension.aspectBuilder().
            extensionInterface(ItemInterface.class).
                objectClass(Item.class).
                    operation("*").
                        enabled(false, AdviceType.BEFORE, AdviceType.AFTER).
        build();
```

If Aspects are used for debugging and testing purposes only - all of them can be turned OFF using the `aspectsEnabled` property of `ClassExtension`.

#### Multiple Advices
Multiple advices can be applied to a single pointcut, enabling chaining. Chaining `before` and `after` advices is straightforward and has no side effects. However, chaining multiple `around` advices is more complex, as only the last `around` advice in the chain can perform the underlying operation; all subsequent advices will operate on the results of the previous ones. Therefore, it's important to consider the implications of your chaining to avoid meaningless combinations. For example, chaining a performance tracking advice right after a caching advice would be counterproductive, as it would always report zero execution time. Proper chaining enhances modularity and reusability but requires thoughtful design to maintain effectiveness and accuracy.

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
Explicit Aspects are only supported for defined operations only. So if there is a need to intercept calls of usual methods - such methods should be dynamically "overridden" by defining operations with the same signature. The following example intercepts `Object.toString()` method:
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
* `Aspects.RetryAdvice` - automatically retries failed operations.
* `Aspects.CachedValueAdvice` - allows caching of operation results

Next >> [Utilities](utilities.md)