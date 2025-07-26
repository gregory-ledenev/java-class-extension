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

#### Aspects.DeepCloneIsolationAroundAdvice
`DeepCloneIsolationAroundAdvice` introduces an effective isolation layer that creates deep-cloned copies of both the input arguments and the results of operations. This mechanism is particularly valuable during module development to prevent the sharing of mutable state, which can lead to unintended side effects and tight coupling between components.

By using deep cloning, this advice ensures that the operation receives independent copies of the arguments, protecting the original inputs from modification during processing. Similarly, it returns isolated copies of the results, preventing clients from inadvertently altering shared state. It also makes unmodifiable views of collections and maps, ensuring that the returned data structures cannot be modified, further enforcing immutability and isolation. This approach provides a clean slate for each method invocation, enhancing predictability and safety.

This strategy is especially beneficial in the context of Modular Monolith architectures, where multiple modules coexist within a single application but should remain loosely coupled. `DeepCloneIsolationAroundAdvice` helps keep modules isolated by avoiding direct sharing of mutable objects, thereby simplifying future efforts to spin off modules into independent services or microservices.

For example, consider a `User` entity handled through this isolation layer. Any changes made to the `User` object within a module will not affect the persistent state until explicitly saved through the `UserService`. This guarantees that modifications are deliberate and controlled, reducing the risk of accidental data corruption or state leakage.

Overall, `DeepCloneIsolationAroundAdvice` enhances modularity, testability, and maintainability by enforcing strict data isolation at the boundary of operations.

#### Aspects.LogBeforeAdvice
`LogBeforeAdvice` is a powerful aspect that enhances the visibility of operations by logging their arguments before execution. This is particularly useful for debugging and monitoring purposes, as it allows developers to trace the flow of data through the system and understand how different components interact.

When applied, `LogBeforeAdvice` captures the method name, the object on which it is called, and the arguments passed to it. This information is then logged, providing a clear record of what operations are being performed and with what data. This can be invaluable in identifying issues or understanding the behavior of complex systems.

#### Aspects.LogAfterAdvice
`LogAfterAdvice` complements `LogBeforeAdvice` by logging the results of operations after they have been executed. This aspect captures the outcome of method calls, providing insights into the effects of operations and helping to verify that they behave as expected.

When `LogAfterAdvice` is applied, it logs the method name, the object on which it was called, and the result returned by the operation. This information is crucial for debugging and monitoring, as it allows developers to see not only what inputs were used but also what outputs were produced, facilitating a better understanding of system behavior.

#### Aspects.LogPerformTimeAdvice
`LogPerformTimeAdvice` is an aspect that measures and logs the time taken to execute operations. This is particularly useful for performance monitoring, as it helps identify bottlenecks and optimize the efficiency of the system.

When applied, `LogPerformTimeAdvice` captures the start time before the operation is executed and calculates the elapsed time once the operation completes. It then logs this information, providing a clear picture of how long each operation takes to perform. This can be invaluable in performance tuning, allowing developers to focus on optimizing slow operations and improving overall system responsiveness.

#### Aspects.PropertyChangeAdvice
`PropertyChangeAdvice` is an aspect that listens for changes to properties of objects and notifies registered listeners when such changes occur. This is particularly useful in scenarios where you need to track modifications to object state, such as in user interfaces or data binding contexts.

When applied, `PropertyChangeAdvice` intercepts property setter methods and triggers notifications to any registered listeners whenever a property value changes. This allows other components of the system to react to changes in real-time, enabling dynamic updates and interactions without requiring manual polling or checks.

#### Aspects.RetryAdvice
`RetryAdvice` is an aspect that implements a retry mechanism for operations that may fail due to transient issues, such as network errors or temporary unavailability of resources. This aspect allows developers to specify how many times an operation should be retried before giving up, along with optional delay strategies between retries.

When applied, `RetryAdvice` intercepts method calls and, if an exception occurs (or a result signals an error), it will automatically retry the operation up to the specified number of attempts. The Default policy is "retry after any exception" but it is possible to fine-tune that behavior to provide `resultChecker` that allows checking whether results and exceptions are errors that can be recovered by retrying the operation.

This can significantly improve the resilience of applications by allowing them to recover from temporary failures without crashing or requiring manual intervention.

#### Aspects.CachedValueAdvice
`CachedValueAdvice` is an aspect that implements caching for operation results, allowing for efficient reuse of previously computed values. This is particularly useful in scenarios where operations are expensive or time-consuming, and the same inputs are likely to be used multiple times.

When applied, `CachedValueAdvice` intercepts method calls and checks if a cached result already exists for the given inputs. If a cached value is found, it is returned immediately, avoiding the need to recompute the result. If no cached value exists, the operation is executed, and the result is stored in the cache for future use.

This `CachedValueAdvice` advice utilises `CachedValueProvider` to manage the cache, allowing for flexible caching strategies. It can be configured to use different caching mechanisms, such as in-memory caches or distributed caches, depending on the application's requirements.

#### Aspects.ReadOnlyCollectionOrMapAdvice
`ReadOnlyCollectionOrMapAdvice` is an aspect that ensures that the results of operations returning collections or maps are wrapped in unmodifiable views. This is particularly useful for preventing or detecting accidental modifications to data structures that should remain immutable, enhancing the safety and integrity of the system.

When applied, `ReadOnlyCollectionOrMapAdvice` intercepts method calls that return collections or maps and automatically converts them into unmodifiable views. For collections, it uses `Collections.unmodifiableCollection()` or `Collections.unmodifiableList()`, and for maps, it uses `Collections.unmodifiableMap()`.

#### Aspects.UnmodifiableValueAdvice
`UnmodifiableValueAdvice` is an aspect that ensures that the results of operations returning collections or maps are wrapped in unmodifiable views. This is particularly useful for preventing or detecting accidental modifications to data structures that should remain immutable, enhancing the safety and integrity of the system.

When applied, `UnmodifiableValueAdvice` intercepts method calls that return collections or maps and automatically converts them into unmodifiable views. For collections, it uses `Collections.unmodifiableCollection()` or `Collections.unmodifiableList()`, and for maps, it uses `Collections.unmodifiableMap()`. For other types of results, it applies a lambda function to convert them into an unmodifiable form.
  
#### Aspects.HandleThrowableAdvice
`HandleThrowableAdvice` is an aspect that provides a mechanism for handling exceptions thrown by operations. This is particularly useful for implementing custom error handling strategies, such as logging errors, transforming exceptions, or providing fallback values. Additionally, it can help adapt existing code to methods that have recently started throwing exceptions in newer versions of libraries or APIs, when the calling code is not prepared to handle them.

When applied, `HandleThrowableAdvice` intercepts method calls and catches any exceptions that occur during execution. It then allows developers to define custom behavior for handling these exceptions, such as logging the error, transforming it into a different type of exception, or returning a default value.

#### Aspects.CircuitBreakerAdvice
`CircuitBreakerAdvice` is an aspect that implements the Circuit Breaker pattern, which is used to prevent a system from repeatedly attempting operations that are likely to fail. This is particularly useful in distributed systems or microservices architectures, where network failures or service unavailability can lead to cascading failures.

When applied, `CircuitBreakerAdvice` monitors the success and failure rates of operations and opens a circuit when the failure rate exceeds a specified threshold. While the circuit is open, any attempts to execute the operation will immediately fail without attempting to call the underlying method. After a predefined timeout, the circuit will attempt to close again, allowing operations to be retried.

Next >> [Expressions](expressions.md)
