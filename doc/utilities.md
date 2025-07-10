## Java Class Extension Library - Utilities

The library provides lots of ready-to-use utility methods that may simplify various , often complex, development tasks.

### Lambda Functions with Description
There is a `DynamicClassExtension.lambdaWithDescription` utility method that returns an extension (wrapper) for a lambda function that associates a custom description with it. Lambda functions in Java cannot customize their `toString()` method output, which can be inconvenient for debugging. This method allows specifying a textual description that will be returned by the `toString()` method of the wrapper.
```java
final String description = "Some description for Runnable";
Runnable extension = DynamicClassExtension.lambdaWithDescription((Runnable) () -> {}, Runnable.class, description);
out.println(extension.toString());
assertEquals(description, extension.toString());
```
### Lambda Functions with Description and Identity
There is a `DynamicClassExtension.lambdaWithDescriptionAndID` utility method that returns an extension (wrapper) for a lambda function that associates a custom description and in identity with it. Lambda functions in Java cannot customize their `toString()`, `hashCode` or `equals` methods, which can be inconvenient for some implementations and for debugging. This method allows specifying:
1. A textual description that will be returned by the `toString()` method
2. An identity that will be used to calculate `hashCode` and to determine equality using the `equals` method
```java
// r1 and r2 are lambdas with the same identity
Runnable r1 = DynamicClassExtension.lambdaWithDescriptionAndID((Runnable) () -> out.println("R"), Runnable.class, "R1", "r1");
Runnable r2 = DynamicClassExtension.lambdaWithDescriptionAndID((Runnable) () -> out.println("R"), Runnable.class, "R1", "r1");
// r3 uses a different identity
Runnable r3 = DynamicClassExtension.lambdaWithDescriptionAndID((Runnable) () -> out.println("R3"), Runnable.class, "R3", "r3");

assertEquals(r1, r2);
assertEquals(r2, r1);

assertNotEquals(r1, r3);
assertNotEquals(r3, r1);
```
### Payload Bundling
Sometimes, you may want to attach additional data—such as diagnostics or contextual information—to an extension without modifying interfaces or their implementations. The DynamicClassExtension utility makes this possible by allowing you to bundle and retrieve payloads dynamically, eliminating the need for static code changes.

* To add a payload to an extension, use: `DynamicClassExtension.extensionWithPayload(...)`.
* To retrieve the payload from an extension, use: `DynamicClassExtension.getPayloadForExtension(...)`

This approach provides flexibility when you need to associate extra information with extensions at runtime, streamlining your code and avoiding the complexity of static inheritance or interface modification.
```java
String payload = "Some textual payload";
Book book = new Book("The Mythical Man-Month");
ItemInterface extension = DynamicClassExtension.extensionWithPayload(book, ItemInterface.class, payload);
// check if payload present and correct
assertEquals("Some textual payload", DynamicClassExtension.getPayloadForExtension(extension).orElse("No payload"));
// check if extension itself behave correctly
assertEquals("The Mythical Man-Month", extension.getName());
```

### Performance Logging
There is an `Aspects.logPerformanceExtension` utility method that returns an extension with added performance logging for all operations. Use of this utility eliminates manual method modification for performance logging:
```java
Book book = new Book("The Mythical Man-Month");
Item_Shippable extension = Aspects.logPerformanceExtension(book, Item_Shippable.class);
extension.ship();
```

### Logging All Operations
There is an `Aspects.logBeforeAndAfterExtension` utility method that returns an extension with logging added before and after all the operations, Use of this utility eliminates manual method modification for arguments and results logging:

```java
Book book = new Book("The Mythical Man-Month");
Item_Shippable extension = Aspects.logBeforeAndAfterExtension(book, Item_Shippable.class);
extension.getName();
extension.setName("Shining");
```

### Listen for Property Change Events
There is an `Aspects.propertyChangeExtension` utility method that returns an extension with added ability to listen for property changes:
```java
Book book = new Book("The Mythical Man-Month");
Item_Shippable extension = Aspects.propertyChangeExtension(book, Item_Shippable.class, 
        evt -> out.println(evt.toString()));
out.println(extension.getName());
extension.setName("Shining");
out.println(extension.getName());
assertEquals("Shining", extension.getName());
```