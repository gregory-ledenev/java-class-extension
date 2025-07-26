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
Chaining multiple advices to a single pointcut enables complex behavior composition. While chaining `before` and `after` advices is straightforward, chaining `around` advices requires careful consideration. The first `around` advice in the chain executes first, and each advice can call `AroundAdvice.applyDefault()` to invoke the next one. Typically, only the last `around` advice performs the actual operation, while if `around` advice skips calling `applyDefault()`, the rest of the chain will be bypassed.

Therefore, the order of your chained advices is crucial for creating meaningful combinations. This strategy allows for sophisticated workflows by combining various `around advices`. For instance, you can chain `CachedValueAdvice`, `RetryAdvice`, and `LogPerformTimeAdvice` to implement caching, retry logic, and performance logging for specific operations. Thoughtful design in chaining enhances modularity and functionality while avoiding unintended consequences.
```java
DynamicClassExtension dynamicClassExtension = new DynamicClassExtension().
        aspectBuilder().
            extensionInterface(ItemInterface.class).
                objectClass(Item.class).
                    operation("*").
                        around(new CachedValueAdvice(cache)).
                        around(new RetryAdvice(5)).
                        around(new LogPerformTimeAdvice()).
        build();
```

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
* `Aspects.ReadOnlyCollectionOrMapAdvice` - allows turning all the `Collection` or `Map` results to their unmodifiable views 
* `Aspects.UnmodifiableValueAdvice` - allows turning all results to their unmodifiable views. It automatically converts 
   `Collection` or `Map` results and uses a lambda function to convert values of other type 
* `Aspects.HandleThrowableAdvice` - allows catching all exceptions and return some value instead 
* `Aspects.CircuitBreakerAdvice` - allows adding circuit breaker to some operations 
* `Aspects.DeepCloneIsolationAroundAdvice` - allows deep cloning of objects before and after the operation execution to isolate changes. It can be useful during module development to prevent tight coupling and avoid unintended sharing of mutable state.


Next >> [Expressions](expressions.md)
