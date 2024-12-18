## Java Class Extension Library - Utilities

The library provides lots of ready-to-use utility methods that may simplify various , often complex, development tasks.

### Lambda Functions with Descriptions
There is a `DynamicClassExtension.lambdaWithDescription` utility method that returns an extension (wrapper) for a lambda function that associates a custom description with it. Lambda functions in Java cannot customize their `toString()` method output, which can be inconvenient for debugging. This method allows specifying a textual description that will be returned by the `toString()` method of the wrapper.
```java
final String description = "Some description for Runnable";
Runnable extension = lambdaWithDescription((Runnable) () -> {}, Runnable.class, description);
out.println(extension.toString());
assertEquals(description, extension.toString());
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